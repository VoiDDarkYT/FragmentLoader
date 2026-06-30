package dev.fragmentcode.loader.remap;

import dev.fragmentcode.installer.install.GameInstallation;
import dev.fragmentcode.loader.launcher.LaunchException;
import dev.fragmentcode.transformer.mapping.MappingFileParser;
import dev.fragmentcode.transformer.mapping.MappingTable;
import dev.fragmentcode.transformer.remap.JarRemapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Обеспечивает, что у нас есть готовый remapped client.jar (с читаемыми
 * именами классов/методов/полей вместо obfuscated), создавая его при
 * необходимости.
 *
 * Ремаппинг всего client.jar - относительно дорогая операция (тысячи
 * классов, каждый проходит через ASM ClassReader/Remapper/ClassWriter
 * с COMPUTE_FRAMES), поэтому делается ОДИН РАЗ и результат кэшируется
 * на диске рядом с оригинальным jar - похожим образом как Fabric кэширует
 * результат TinyRemapper между запусками.
 *
 * Кэш инвалидируется проверкой по хешу: если оригинальный client.jar
 * изменился (например после обновления версии) или remapped jar
 * отсутствует, ремаппинг выполняется заново.
 */
public final class RemappingService {

    private static final String REMAPPED_SUFFIX = "-remapped.jar";
    private static final String MARKER_SUFFIX = "-remapped.sha1";

    /**
     * Возвращает путь к remapped client.jar, создавая его при
     * необходимости (если отсутствует или оригинал изменился с прошлого
     * ремаппинга).
     */
    public Path ensureRemapped(GameInstallation installation) throws LaunchException {

        Path sourceJar = installation.getClientJar();
        Path remappedJar = sourceJar.resolveSibling(
                stripExtension(sourceJar.getFileName().toString()) + REMAPPED_SUFFIX
        );
        Path markerFile = sourceJar.resolveSibling(
                stripExtension(sourceJar.getFileName().toString()) + MARKER_SUFFIX
        );

        try {

            String currentSourceHash = sha256Of(sourceJar);

            if (isUpToDate(remappedJar, markerFile, currentSourceHash)) {
                return remappedJar;
            }

            System.out.println("Remapping client.jar to readable names (one-time, may take a moment)...");

            MappingTable mappingTable = loadMappingTable(installation.getMappingsFile());

            JarRemapper jarRemapper = new JarRemapper(mappingTable);
            jarRemapper.remapJar(sourceJar, installation.getLibraryJars(), remappedJar);

            Files.writeString(markerFile, currentSourceHash);

            System.out.println("Remapping complete: " + remappedJar);

            return remappedJar;

        } catch (IOException e) {
            throw new LaunchException("Failed to remap client.jar: " + e.getMessage(), e);
        }

    }

    private boolean isUpToDate(Path remappedJar, Path markerFile, String currentSourceHash) throws IOException {

        if (!Files.exists(remappedJar) || !Files.exists(markerFile)) {
            return false;
        }

        String storedHash = Files.readString(markerFile).strip();

        return storedHash.equals(currentSourceHash);

    }

    private MappingTable loadMappingTable(Path mappingsFile) throws IOException {

        String content = Files.readString(mappingsFile);

        MappingFileParser parser = new MappingFileParser();

        return parser.parse(content);

    }

    private String sha256Of(Path file) throws IOException {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (var in = Files.newInputStream(file)) {

                byte[] buffer = new byte[8192];
                int read;

                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }

            }

            StringBuilder hex = new StringBuilder();

            for (byte b : digest.digest()) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 not available", e);
        }

    }

    private String stripExtension(String fileName) {

        int dot = fileName.lastIndexOf('.');

        return dot == -1 ? fileName : fileName.substring(0, dot);

    }

}
