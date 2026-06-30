package dev.fragmentcode.gradle;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.jar.*;

/**
 * Скачивает client.jar и client_mappings.txt для нужной версии Minecraft,
 * ремаппирует jar (obfuscated -> readable), кэширует результат в
 * ~/.gradle/caches/fragment/<version>/ чтобы не делать это при каждой
 * сборке мода.
 *
 * Аналог того, что делает Fabric Loom, но намного проще: один класс
 * без дополнительных зависимостей кроме ASM и Gson.
 */
public final class MinecraftProvider {

    private static final String VERSION_MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private static final String FRAGMENT_API_URL =
            "https://github.com/VoiDDarkYT/FragmentLoader/releases/download/0.0.1/fragment-api-0.0.1.jar";

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final Gson gson = new Gson();

    /**
     * Главный метод — возвращает путь к готовому remapped minecraft jar
     * (и кэширует его). Если уже есть в кэше и не устарел — просто
     * возвращает путь без скачивания/ремаппинга.
     */
    public Path provideRemappedJar(String minecraftVersion) throws Exception {

        Path cacheDir = getCacheDir(minecraftVersion);
        Files.createDirectories(cacheDir);

        Path remappedJar = cacheDir.resolve("minecraft-" + minecraftVersion + "-mapped.jar");
        Path markerFile  = cacheDir.resolve("minecraft-" + minecraftVersion + "-mapped.sha1");

        // Получаем метаданные версии чтобы узнать URL и sha1 client.jar
        System.out.println("[Fragment] Fetching Minecraft " + minecraftVersion + " metadata...");
        JsonObject versionMeta = fetchVersionMetadata(minecraftVersion);

        JsonObject clientDownload = versionMeta
                .getAsJsonObject("downloads")
                .getAsJsonObject("client");
        String clientUrl  = clientDownload.get("url").getAsString();
        String clientSha1 = clientDownload.get("sha1").getAsString();

        JsonObject mappingsDownload = versionMeta
                .getAsJsonObject("downloads")
                .getAsJsonObject("client_mappings");
        String mappingsUrl  = mappingsDownload.get("url").getAsString();
        String mappingsSha1 = mappingsDownload.get("sha1").getAsString();

        // Проверяем кэш: если remapped jar уже есть и sha1 совпадает
        // с оригинальным client.jar (значит ремаппинг актуален) - используем
        if (isCacheValid(remappedJar, markerFile, clientSha1)) {
            System.out.println("[Fragment] Using cached remapped jar: " + remappedJar);
            return remappedJar;
        }

        // Скачиваем оригинальный client.jar
        Path clientJar = cacheDir.resolve("minecraft-" + minecraftVersion + ".jar");
        System.out.println("[Fragment] Downloading Minecraft " + minecraftVersion + " client...");
        downloadFile(clientUrl, clientJar, clientSha1);

        // Скачиваем маппинги
        Path mappingsFile = cacheDir.resolve("client_mappings.txt");
        System.out.println("[Fragment] Downloading Mojang mappings...");
        downloadFile(mappingsUrl, mappingsFile, mappingsSha1);

        // Ремаппим jar
        System.out.println("[Fragment] Remapping Minecraft jar (one-time, may take a moment)...");
        remapJar(clientJar, mappingsFile, remappedJar);

        // Сохраняем маркер (sha1 оригинального jar) для инвалидации кэша
        Files.writeString(markerFile, clientSha1);
        System.out.println("[Fragment] Remapped jar ready: " + remappedJar);

        return remappedJar;

    }

    /**
     * Скачивает fragment-api jar если ещё не скачан.
     */
    public Path provideFragmentApi() throws Exception {

        Path cacheDir = getFragmentCacheDir();
        Files.createDirectories(cacheDir);
        Path apiJar = cacheDir.resolve("fragment-api-0.0.1.jar");

        if (Files.exists(apiJar)) {
            return apiJar;
        }

        System.out.println("[Fragment] Downloading fragment-api...");
        downloadFile(FRAGMENT_API_URL, apiJar, null);
        return apiJar;

    }

    // -------------------------------------------------------------------------
    // Manifest / metadata fetching
    // -------------------------------------------------------------------------

    private JsonObject fetchVersionMetadata(String versionId) throws Exception {

        String manifestJson = fetchText(VERSION_MANIFEST_URL);
        JsonObject manifest = gson.fromJson(manifestJson, JsonObject.class);

        JsonArray versions = manifest.getAsJsonArray("versions");
        String versionUrl = null;

        for (JsonElement el : versions) {
            JsonObject entry = el.getAsJsonObject();
            if (versionId.equals(entry.get("id").getAsString())) {
                versionUrl = entry.get("url").getAsString();
                break;
            }
        }

        if (versionUrl == null) {
            throw new IllegalArgumentException(
                    "Minecraft version '" + versionId + "' not found in Mojang manifest"
            );
        }

        return gson.fromJson(fetchText(versionUrl), JsonObject.class);

    }

    // -------------------------------------------------------------------------
    // Remapping — упрощённая версия JarRemapper из fragment-transformer,
    // без зависимости на тот модуль (плагин — отдельный артефакт)
    // -------------------------------------------------------------------------

    private void remapJar(Path sourceJar, Path mappingsFile, Path destinationJar) throws Exception {

        // Парсим маппинги Mojang (ProGuard формат)
        Map<String, String> classMap = parseMappings(mappingsFile);

        // SimpleRemapper принимает flat map: obfuscatedInternalName -> readableInternalName
        // для классов, и owner/obfName -> readableName для методов/полей
        SimpleRemapper remapper = new SimpleRemapper(classMap);

        Files.createDirectories(destinationJar.getParent());

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarFile input   = new JarFile(sourceJar.toFile());
             JarOutputStream output = new JarOutputStream(
                     Files.newOutputStream(destinationJar), manifest)) {

            Enumeration<JarEntry> entries = input.entries();

            while (entries.hasMoreElements()) {

                JarEntry entry = entries.nextElement();

                if (entry.isDirectory()) continue;

                String name = entry.getName();

                // Пропускаем подписи — они становятся невалидными после ремаппинга
                if (isSignatureEntry(name)) continue;

                if (!name.endsWith(".class")) {
                    // Ресурсы копируем как есть
                    copyEntry(input, entry, output);
                    continue;
                }

                byte[] original;
                try (InputStream in = input.getInputStream(entry)) {
                    original = in.readAllBytes();
                }

                byte[] remapped = remapClass(original, remapper);

                // Путь в jar тоже должен быть readable
                String obfName = name.substring(0, name.length() - 6); // убираем ".class"
                String newName = classMap.getOrDefault(obfName, obfName) + ".class";

                output.putNextEntry(new JarEntry(newName));
                output.write(remapped);
                output.closeEntry();

            }

        }

    }

    private byte[] remapClass(byte[] bytecode, SimpleRemapper remapper) {

        ClassReader reader = new ClassReader(bytecode);
        ClassWriter writer = new ClassWriter(0);
        ClassRemapper classRemapper = new ClassRemapper(writer, remapper);
        reader.accept(classRemapper, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();

    }

    /**
     * Парсит ProGuard маппинги Mojang в flat map для SimpleRemapper.
     *
     * SimpleRemapper ожидает:
     *   "obf/Internal/Name" -> "readable/Internal/Name"  (для классов)
     *   "obf/Owner.obfMethodName(Ldesc;)V" -> "readableMethodName"  (для методов)
     *   "obf/Owner.obfFieldName" -> "readableFieldName"  (для полей)
     */
    private Map<String, String> parseMappings(Path mappingsFile) throws IOException {

        Map<String, String> map = new HashMap<>();

        String currentReadableClass = null;
        String currentObfClass      = null;

        for (String rawLine : Files.readAllLines(mappingsFile)) {

            String line = rawLine.trim();

            if (line.isEmpty() || line.startsWith("#")) continue;

            if (!line.startsWith(" ") && !line.startsWith("\t")) {

                // Заголовок класса: "readable.Name -> obf:"
                int arrow = line.indexOf(" -> ");
                if (arrow == -1 || !line.endsWith(":")) continue;

                currentReadableClass = line.substring(0, arrow).trim();
                String obfDotted     = line.substring(arrow + 4, line.length() - 1).trim();

                currentObfClass = obfDotted.replace('.', '/');
                String readableInternal = currentReadableClass.replace('.', '/');

                map.put(currentObfClass, readableInternal);

            } else if (currentObfClass != null) {

                // Поле или метод
                int arrow = line.indexOf(" -> ");
                if (arrow == -1) continue;

                String obfName      = line.substring(arrow + 4).trim();
                String leftSide     = line.substring(0, arrow).trim();

                // Убираем префикс номеров строк "12:34:" если есть
                if (leftSide.matches("^\\d+:\\d+:.*")) {
                    leftSide = leftSide.replaceFirst("^\\d+:\\d+:", "");
                }

                boolean isMethod = leftSide.contains("(");

                if (isMethod) {

                    // "returnType methodName(paramTypes)"
                    int parenOpen  = leftSide.indexOf('(');
                    int parenClose = leftSide.indexOf(')');
                    int spaceBeforeMethod = leftSide.lastIndexOf(' ', parenOpen);

                    if (spaceBeforeMethod == -1) continue;

                    String readableMethodName = leftSide.substring(spaceBeforeMethod + 1, parenOpen);
                    String paramsPart         = leftSide.substring(parenOpen + 1, parenClose);
                    String returnTypePart     = leftSide.substring(0, spaceBeforeMethod);

                    String descriptor = buildDescriptor(returnTypePart, paramsPart);

                    // SimpleRemapper key: "owner.name(desc)"
                    String key = currentObfClass + "." + obfName + descriptor;
                    map.put(key, readableMethodName);

                } else {

                    // Поле: "type fieldName"
                    int spaceIdx = leftSide.lastIndexOf(' ');
                    if (spaceIdx == -1) continue;

                    String readableFieldName = leftSide.substring(spaceIdx + 1);

                    // SimpleRemapper key: "owner.name"
                    String key = currentObfClass + "." + obfName;
                    map.put(key, readableFieldName);

                }

            }

        }

        return map;

    }

    /**
     * Строит JVM-дескриптор из ProGuard-нотации.
     * "void" -> "V", "int[]" -> "[I", "java.lang.String" -> "Ljava/lang/String;" и т.д.
     */
    private String buildDescriptor(String returnType, String params) {

        StringBuilder sb = new StringBuilder("(");

        if (!params.isEmpty()) {
            for (String param : params.split(",")) {
                sb.append(toDescriptor(param.trim()));
            }
        }

        sb.append(")");
        sb.append(toDescriptor(returnType.trim()));
        return sb.toString();

    }

    private String toDescriptor(String type) {

        if (type.isEmpty()) return "V";

        // Считаем массивные измерения
        int arrayDims = 0;
        while (type.endsWith("[]")) {
            arrayDims++;
            type = type.substring(0, type.length() - 2);
        }

        String base = switch (type) {
            case "void"    -> "V";
            case "boolean" -> "Z";
            case "byte"    -> "B";
            case "char"    -> "C";
            case "short"   -> "S";
            case "int"     -> "I";
            case "long"    -> "J";
            case "float"   -> "F";
            case "double"  -> "D";
            default        -> "L" + type.replace('.', '/') + ";";
        };

        return "[".repeat(arrayDims) + base;

    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private boolean isCacheValid(Path remappedJar, Path markerFile, String expectedSha1) throws IOException {

        if (!Files.exists(remappedJar) || !Files.exists(markerFile)) return false;

        String stored = Files.readString(markerFile).strip();
        return stored.equalsIgnoreCase(expectedSha1);

    }

    private void downloadFile(String url, Path destination, String expectedSha1) throws Exception {

        if (Files.exists(destination) && expectedSha1 != null) {
            if (sha1Of(destination).equalsIgnoreCase(expectedSha1)) return;
        }

        Files.createDirectories(destination.getParent());

        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<InputStream> response = http.send(request,
                HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }

        Path tmp = destination.resolveSibling(destination.getFileName() + ".tmp");

        try (InputStream in = response.body();
             OutputStream out = Files.newOutputStream(tmp)) {
            in.transferTo(out);
        }

        Files.move(tmp, destination, StandardCopyOption.REPLACE_EXISTING);

    }

    private String fetchText(String url) throws Exception {

        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }

        return response.body();

    }

    private String sha1Of(Path file) throws IOException {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) != -1) digest.update(buf, 0, read);
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 not available", e);
        }

    }

    private boolean isSignatureEntry(String name) {

        if (!name.startsWith("META-INF/")) return false;
        String upper = name.toUpperCase();
        return upper.equals("META-INF/MANIFEST.MF")
                || upper.endsWith(".SF")
                || upper.endsWith(".RSA")
                || upper.endsWith(".DSA")
                || upper.endsWith(".EC");

    }

    private void copyEntry(JarFile jar, JarEntry entry, JarOutputStream out) throws IOException {

        try (InputStream in = jar.getInputStream(entry)) {
            out.putNextEntry(new JarEntry(entry.getName()));
            in.transferTo(out);
            out.closeEntry();
        }

    }

    private Path getCacheDir(String version) {

        return getFragmentCacheDir().resolve(version);

    }

    private Path getFragmentCacheDir() {

        String gradleHome = System.getProperty("user.home");
        return Path.of(gradleHome, ".gradle", "caches", "fragment");

    }

}
