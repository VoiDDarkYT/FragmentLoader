package dev.fragmentcode.installer.profile;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Строит library-запись в формате version.json для одного из наших
 * собственных jar-файлов, размещённых как GitHub Release asset.
 *
 * Формат записи аналогичен обычной vanilla library (см. Library в пакете
 * version), но без "path" - GitHub Releases не нужна maven-структура
 * директорий, просто прямой url на конкретный файл:
 *
 * {
 *   "name": "dev.fragmentcode:fragment-loader:0.0.1",
 *   "downloads": {
 *     "artifact": {
 *       "url": "https://github.com/.../fragment-loader-0.0.1.jar",
 *       "sha1": "...",
 *       "size": ...,
 *       "path": "dev/fragmentcode/fragment-loader/0.0.1/fragment-loader-0.0.1.jar"
 *     }
 *   }
 * }
 *
 * "path" всё равно нужен - launcher использует его как относительный путь,
 * куда положить скачанный файл внутри .minecraft/libraries/. Без "path"
 * некоторые launcher'ы (в т.ч. Prism) не знают, куда сохранить файл.
 *
 * Текущая версия Fragment Loader публикуется единым fat jar'ом (содержащим
 * fragment-api + fragment-installer + fragment-loader + gson) - так как
 * GitHub Releases это просто файлы, а не настоящий maven-репозиторий,
 * проще держать одну запись на один файл, а не пытаться разложить
 * по отдельным jar'ам (что потребовало бы отдельных релизов на каждый
 * модуль).
 */
public final class OwnLibraryEntry {

    private static final String GROUP = "dev.fragmentcode";
    private static final String ARTIFACT_ID = "fragment-loader";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Строит список library-записей для добавления в Prism-профиль.
     * Сейчас это единственная запись - наш fat jar.
     *
     * @param downloadUrl  прямая ссылка на GitHub Release asset
     *                     (например https://github.com/VoiDDarkYT/FragmentLoader/releases/download/0.0.1/fragment-loader-0.0.1.jar)
     * @param version      версия для отображения в maven coordinates,
     *                     например "0.0.1"
     */
    public static List<JsonObject> buildAll(String downloadUrl, String version) throws ProfileGenerationException {

        List<JsonObject> result = new ArrayList<>();
        result.add(buildEntry(downloadUrl, version));

        return result;

    }

    private static JsonObject buildEntry(String downloadUrl, String version) throws ProfileGenerationException {

        String fileName = ARTIFACT_ID + "-" + version + ".jar";
        String relativePath = GROUP.replace('.', '/') + "/" + ARTIFACT_ID + "/" + version + "/" + fileName;

        long[] size = new long[1];
        String sha1 = fetchSha1AndSize(downloadUrl, size);

        JsonObject artifact = new JsonObject();
        artifact.addProperty("url", downloadUrl);
        artifact.addProperty("sha1", sha1);
        artifact.addProperty("size", size[0]);
        artifact.addProperty("path", relativePath);

        JsonObject downloads = new JsonObject();
        downloads.add("artifact", artifact);

        JsonObject library = new JsonObject();
        library.addProperty("name", GROUP + ":" + ARTIFACT_ID + ":" + version);
        library.add("downloads", downloads);

        return library;

    }

    /**
     * Скачивает файл с GitHub Release один раз, считая sha1 и размер
     * в одном проходе - это нужно вписать в профиль для проверки
     * целостности при скачивании внешним launcher'ом. Так как сам
     * генератор профиля запускается редко (один раз на релиз), а не на
     * каждом старте игры, накладные расходы на полное скачивание здесь
     * приемлемы.
     *
     * @param outSize  однозначный "out"-параметр для размера файла в байтах
     *                 (Java не поддерживает несколько return-значений напрямую)
     */
    private static String fetchSha1AndSize(String url, long[] outSize) throws ProfileGenerationException {

        try {

            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<InputStream> response =
                    HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new ProfileGenerationException(
                        "Unexpected HTTP status " + response.statusCode() + " fetching " + url
                );
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            long totalBytes = 0;

            try (InputStream in = response.body()) {

                byte[] buffer = new byte[8192];
                int read;

                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                    totalBytes += read;
                }

            }

            outSize[0] = totalBytes;

            StringBuilder hex = new StringBuilder();

            for (byte b : digest.digest()) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (IOException | InterruptedException | NoSuchAlgorithmException e) {
            throw new ProfileGenerationException("Failed to compute sha1 for " + url, e);
        }

    }

}
