package dev.fragmentcode.installer.manifest;

import com.google.gson.Gson;
import dev.fragmentcode.installer.download.DownloadException;
import dev.fragmentcode.installer.version.AssetIndex;
import dev.fragmentcode.installer.version.VersionMetadata;
import dev.fragmentcode.installer.version.VersionMetadataParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Скачивает и разбирает JSON-манифесты Mojang:
 *   - version_manifest_v2.json (список всех версий игры)
 *   - <версия>.json (метаданные конкретной версии: libraries, mainClass и т.д.)
 *   - <assetIndex id>.json (список ресурсов игры)
 *
 * В отличие от FileDownloader (который сохраняет файлы на диск с проверкой
 * sha1, для крупных бинарных файлов), эти JSON-файлы небольшие и нам нужно
 * сразу получить их содержимое как объект, а не просто сохранить на диск.
 */
public final class ManifestFetcher {

    private static final String VERSION_MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public VersionManifest fetchVersionManifest() throws DownloadException {

        String json = fetchText(VERSION_MANIFEST_URL);
        Gson gson = VersionMetadataParser.gson();

        return gson.fromJson(json, VersionManifest.class);

    }

    public VersionMetadata fetchVersionMetadata(String url) throws DownloadException {

        String json = fetchText(url);
        return VersionMetadataParser.parse(json);

    }

    public AssetIndex fetchAssetIndex(String url) throws DownloadException {

        String json = fetchText(url);
        Gson gson = VersionMetadataParser.gson();

        return gson.fromJson(json, AssetIndex.class);

    }

    /**
     * Возвращает сырой текст JSON-ответа без парсинга в типизированные
     * модели. Используется, когда нужно сохранить/модифицировать JSON
     * как есть (например PrismProfileGenerator, который правит отдельные
     * поля vanilla version.json, сохраняя все остальные поля нетронутыми).
     */
    public String fetchRawText(String url) throws DownloadException {
        return fetchText(url);
    }

    private String fetchText(String url) throws DownloadException {

        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();

        try {

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new DownloadException(
                        "Unexpected HTTP status " + response.statusCode() + " for " + url
                );
            }

            return response.body();

        } catch (IOException | InterruptedException e) {
            throw new DownloadException("Failed to fetch " + url, e);
        }

    }

}
