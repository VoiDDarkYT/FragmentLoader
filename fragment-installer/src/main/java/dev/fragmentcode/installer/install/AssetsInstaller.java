package dev.fragmentcode.installer.install;

import dev.fragmentcode.installer.download.DownloadException;
import dev.fragmentcode.installer.download.DownloadListener;
import dev.fragmentcode.installer.download.FileDownloader;
import dev.fragmentcode.installer.layout.GameDirectoryLayout;
import dev.fragmentcode.installer.manifest.ManifestFetcher;
import dev.fragmentcode.installer.version.AssetIndex;
import dev.fragmentcode.installer.version.AssetIndexInfo;

import java.nio.file.Path;
import java.util.Map;

/**
 * Скачивает ресурсы игры (текстуры, звуки, шрифты и т.д.):
 *   1. Скачивает сам assetIndex JSON (список объектов с их hash).
 *   2. Сохраняет его в assets/indexes/<id>.json.
 *   3. Скачивает каждый объект в assets/objects/<hash 2 символа>/<hash>,
 *      используя hash как имя файла (так делает сам Mojang launcher -
 *      разные "виртуальные" имена файлов могут указывать на один и тот же
 *      физический объект, экономя место при дублях).
 *
 * Это самая объёмная по количеству файлов и суммарному размеру часть
 * установки - у 1.21.7 totalSize ~430 МБ, десятки тысяч отдельных файлов.
 */
public final class AssetsInstaller {

    private final GameDirectoryLayout layout;
    private final ManifestFetcher manifestFetcher;
    private final FileDownloader downloader;
    private final DownloadListener listener;

    public AssetsInstaller(GameDirectoryLayout layout, DownloadListener listener) {
        this.layout = layout;
        this.manifestFetcher = new ManifestFetcher();
        this.downloader = new FileDownloader(listener);
        this.listener = listener;
    }

    public void install(AssetIndexInfo assetIndexInfo) throws DownloadException {

        AssetIndex assetIndex = manifestFetcher.fetchAssetIndex(assetIndexInfo.getUrl());

        // Сохраняем сам индекс на диск, как это делает official launcher,
        // чтобы при повторном запуске не скачивать его заново без необходимости.
        Path indexFile = layout.getAssetIndexFile(assetIndexInfo.getId());
        downloader.download(
                assetIndexInfo.getUrl(),
                indexFile,
                assetIndexInfo.getSha1(),
                "asset index " + assetIndexInfo.getId()
        );

        Map<String, AssetIndex.AssetObject> objects = assetIndex.getObjects();

        int total = objects.size();
        int done = 0;

        for (Map.Entry<String, AssetIndex.AssetObject> entry : objects.entrySet()) {

            AssetIndex.AssetObject asset = entry.getValue();

            Path destination = layout.getAssetObjectFile(asset.getHashPrefix(), asset.getHash());
            String objectUrl = "https://resources.download.minecraft.net/"
                    + asset.getHashPrefix() + "/" + asset.getHash();

            downloader.download(objectUrl, destination, asset.getHash(), entry.getKey());

            done++;
            listener.onGroupProgress(done, total);

        }

    }

}
