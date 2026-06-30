package dev.fragmentcode.installer.install;

import dev.fragmentcode.installer.download.DownloadException;
import dev.fragmentcode.installer.download.DownloadListener;
import dev.fragmentcode.installer.layout.GameDirectoryLayout;
import dev.fragmentcode.installer.manifest.ManifestFetcher;
import dev.fragmentcode.installer.manifest.VersionManifest;
import dev.fragmentcode.installer.version.VersionMetadata;

import java.nio.file.Path;
import java.util.List;

/**
 * Главная точка входа модуля fragment-installer.
 *
 * Полный процесс установки версии игры:
 *   1. Скачать version_manifest_v2.json, найти запись по versionId.
 *   2. Скачать соответствующий version.json (VersionMetadata).
 *   3. Скачать client.jar.
 *   4. Скачать все применимые для текущей ОС libraries, распаковать natives.
 *   5. Скачать assetIndex и все объекты ресурсов.
 *
 * Результат - готовый GameInstallation с classpath и путями, которые
 * fragment-loader использует для запуска игры.
 */
public final class GameInstaller {

    private final GameDirectoryLayout layout;
    private final DownloadListener listener;
    private final ManifestFetcher manifestFetcher;

    public GameInstaller(Path gameRoot, DownloadListener listener) {
        this.layout = new GameDirectoryLayout(gameRoot);
        this.listener = listener;
        this.manifestFetcher = new ManifestFetcher();
    }

    public GameInstallation install(String versionId) throws DownloadException, InstallException {

        VersionManifest manifest = manifestFetcher.fetchVersionManifest();
        VersionManifest.VersionEntry entry = manifest.findVersion(versionId);

        if (entry == null) {
            throw new InstallException("Version not found in manifest: " + versionId);
        }

        VersionMetadata metadata = manifestFetcher.fetchVersionMetadata(entry.getUrl());

        ClientInstaller clientInstaller = new ClientInstaller(layout, listener);
        Path clientJar = clientInstaller.install(metadata);

        LibraryInstaller libraryInstaller = new LibraryInstaller(layout, listener);
        List<Path> libraryJars = libraryInstaller.install(metadata, versionId);

        AssetsInstaller assetsInstaller = new AssetsInstaller(layout, listener);
        assetsInstaller.install(metadata.getAssetIndex());

        MappingsInstaller mappingsInstaller = new MappingsInstaller(layout, listener);
        Path mappingsFile = mappingsInstaller.install(metadata);

        return new GameInstallation(
                metadata,
                clientJar,
                libraryJars,
                layout.getNativesDirectory(versionId),
                layout.getAssetsDirectory(),
                layout.getRoot(),
                mappingsFile
        );

    }

}
