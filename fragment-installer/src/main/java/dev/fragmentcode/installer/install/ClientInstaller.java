package dev.fragmentcode.installer.install;

import dev.fragmentcode.installer.download.DownloadException;
import dev.fragmentcode.installer.download.DownloadListener;
import dev.fragmentcode.installer.download.FileDownloader;
import dev.fragmentcode.installer.layout.GameDirectoryLayout;
import dev.fragmentcode.installer.version.VersionMetadata;

import java.nio.file.Path;

/**
 * Скачивает client.jar - сам клиент Minecraft (без библиотек и ресурсов).
 */
public final class ClientInstaller {

    private final GameDirectoryLayout layout;
    private final FileDownloader downloader;

    public ClientInstaller(GameDirectoryLayout layout, DownloadListener listener) {
        this.layout = layout;
        this.downloader = new FileDownloader(listener);
    }

    public Path install(VersionMetadata metadata) throws DownloadException {

        var clientDownload = metadata.getDownloads().getClient();
        Path destination = layout.getClientJar(metadata.getId());

        downloader.download(
                clientDownload.getUrl(),
                destination,
                clientDownload.getSha1(),
                metadata.getId() + ".jar"
        );

        return destination;

    }

}
