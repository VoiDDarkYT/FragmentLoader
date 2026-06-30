package dev.fragmentcode.installer.install;

import dev.fragmentcode.installer.download.DownloadException;
import dev.fragmentcode.installer.download.DownloadListener;
import dev.fragmentcode.installer.download.FileDownloader;
import dev.fragmentcode.installer.layout.GameDirectoryLayout;
import dev.fragmentcode.installer.version.VersionMetadata;

import java.nio.file.Path;

/**
 * Скачивает client_mappings.txt (Mojang official ProGuard mappings) -
 * текстовый файл, описывающий соответствие readable <-> obfuscated имён
 * классов/методов/полей. Используется fragment-transformer для
 * ремаппинга client.jar в читаемую форму (см. JarRemapper).
 *
 * Лицензия Mojang на эти mappings разрешает использование для разработки,
 * но запрещает редистрибуцию файла "complete and unmodified" - поэтому
 * мы только скачиваем его себе при установке, а не публикуем где-либо.
 */
public final class MappingsInstaller {

    private final GameDirectoryLayout layout;
    private final FileDownloader downloader;

    public MappingsInstaller(GameDirectoryLayout layout, DownloadListener listener) {
        this.layout = layout;
        this.downloader = new FileDownloader(listener);
    }

    public Path install(VersionMetadata metadata) throws DownloadException {

        var mappingsDownload = metadata.getDownloads().getClientMappings();
        Path destination = layout.getVersionDirectory(metadata.getId()).resolve("client_mappings.txt");

        downloader.download(
                mappingsDownload.getUrl(),
                destination,
                mappingsDownload.getSha1(),
                "client mappings"
        );

        return destination;

    }

}
