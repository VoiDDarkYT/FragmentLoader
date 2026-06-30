package dev.fragmentcode.installer.download;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Скачивает один файл по URL в указанный путь на диске.
 *
 * Логика проверки sha1 (повторяет поведение официального launcher'а):
 *   1. Если файл уже существует локально и его sha1 совпадает с ожидаемым
 *      -> скачивание пропускается (экономим время и трафик).
 *   2. Файл скачивается во временный файл (.tmp), не напрямую в целевой путь,
 *      чтобы при сбое/прерывании не оставить повреждённый файл на месте
 *      настоящего.
 *   3. После скачивания проверяется sha1 финального файла. Если не совпадает
 *      -> бросается DownloadException, временный файл удаляется.
 *   4. Только после успешной проверки временный файл переименовывается
 *      в целевой.
 */
public final class FileDownloader {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final DownloadListener listener;

    public FileDownloader(DownloadListener listener) {
        this.listener = listener;
    }

    /**
     * Скачивает файл, если он отсутствует или его sha1 не совпадает.
     *
     * @param url           адрес файла
     * @param destination   путь, куда сохранить файл
     * @param expectedSha1  ожидаемый sha1, может быть null если проверка не нужна
     * @param displayName   имя файла для прогресс-бара/логов
     */
    public void download(String url, Path destination, String expectedSha1, String displayName)
            throws DownloadException {

        try {

            if (Files.exists(destination) && expectedSha1 != null) {

                String localSha1 = sha1Of(destination);

                if (localSha1.equalsIgnoreCase(expectedSha1)) {
                    // Файл уже есть и совпадает — ничего скачивать не нужно.
                    listener.onFileComplete(displayName);
                    return;
                }

            }

            Files.createDirectories(destination.getParent());

            Path tempFile = destination.resolveSibling(destination.getFileName() + ".tmp");

            downloadWithRetry(url, tempFile, displayName);

            if (expectedSha1 != null) {

                String actualSha1 = sha1Of(tempFile);

                if (!actualSha1.equalsIgnoreCase(expectedSha1)) {
                    Files.deleteIfExists(tempFile);
                    throw new DownloadException(
                            "Sha1 mismatch for " + displayName
                                    + " (expected " + expectedSha1 + ", got " + actualSha1 + ")"
                    );
                }

            }

            Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);

            listener.onFileComplete(displayName);

        } catch (IOException e) {
            throw new DownloadException("Failed to download " + displayName, e);
        }

    }

    /**
     * Скачивает файл, повторяя попытку до MAX_ATTEMPTS раз при сетевых
     * сбоях (таймауты, обрывы соединения и т.д.). При установке игры
     * скачивается несколько тысяч отдельных файлов (assets) - единичные
     * сетевые сбои на таком объёме статистически почти неизбежны, поэтому
     * без retry любой случайный обрыв полностью прерывал бы установку.
     */
    private void downloadWithRetry(String url, Path tempFile, String displayName) throws DownloadException {

        DownloadException lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

            try {
                downloadToFile(url, tempFile, displayName);
                return;
            } catch (DownloadException e) {

                lastError = e;

                if (attempt < MAX_ATTEMPTS) {

                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }

                }

            }

        }

        throw new DownloadException(
                "Failed to download " + displayName + " after " + MAX_ATTEMPTS + " attempts",
                lastError
        );

    }

    private void downloadToFile(String url, Path tempFile, String displayName) throws DownloadException {

        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();

        try {

            HttpResponse<InputStream> response =
                    HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new DownloadException(
                        "Unexpected HTTP status " + response.statusCode() + " for " + url
                );
            }

            long totalBytes = response.headers()
                    .firstValueAsLong("Content-Length")
                    .orElse(-1);

            try (InputStream in = response.body();
                 var out = Files.newOutputStream(tempFile)) {

                byte[] buffer = new byte[8192];
                long bytesDone = 0;
                int read;

                while ((read = in.read(buffer)) != -1) {

                    out.write(buffer, 0, read);
                    bytesDone += read;

                    listener.onFileProgress(displayName, bytesDone, totalBytes);

                }

            }

        } catch (IOException | InterruptedException e) {
            throw new DownloadException("Failed to download " + url, e);
        }

    }

    private String sha1Of(Path file) throws IOException {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            try (InputStream in = Files.newInputStream(file)) {

                byte[] buffer = new byte[8192];
                int read;

                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }

            }

            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            // SHA-1 всегда доступен в стандартной JVM, это не должно происходить.
            throw new IOException("SHA-1 algorithm not available", e);
        }

    }

}
