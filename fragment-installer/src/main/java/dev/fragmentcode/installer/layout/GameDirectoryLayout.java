package dev.fragmentcode.installer.layout;

import java.nio.file.Path;

/**
 * Вычисляет стандартные пути внутри .minecraft-совместимой папки.
 * Структура полностью повторяет official launcher, чтобы переиспользовать
 * файлы, которые пользователь уже скачал официальным способом:
 *
 *   <root>/
 *     versions/<id>/<id>.jar
 *     versions/<id>/<id>.json
 *     versions/<id>/natives/         (распакованные платформенные библиотеки)
 *     libraries/<maven path>.jar
 *     assets/indexes/<assetIndexId>.json
 *     assets/objects/<hash 2 chars>/<full hash>
 */
public final class GameDirectoryLayout {

    private final Path root;

    public GameDirectoryLayout(Path root) {
        this.root = root;
    }

    public Path getRoot() {
        return root;
    }

    public Path getVersionDirectory(String versionId) {
        return root.resolve("versions").resolve(versionId);
    }

    public Path getClientJar(String versionId) {
        return getVersionDirectory(versionId).resolve(versionId + ".jar");
    }

    public Path getVersionJson(String versionId) {
        return getVersionDirectory(versionId).resolve(versionId + ".json");
    }

    public Path getNativesDirectory(String versionId) {
        return getVersionDirectory(versionId).resolve("natives");
    }

    public Path getLibrariesDirectory() {
        return root.resolve("libraries");
    }

    public Path getLibraryFile(String relativePath) {
        return getLibrariesDirectory().resolve(relativePath);
    }

    public Path getAssetsDirectory() {
        return root.resolve("assets");
    }

    public Path getAssetIndexFile(String assetIndexId) {
        return getAssetsDirectory().resolve("indexes").resolve(assetIndexId + ".json");
    }

    public Path getAssetObjectFile(String hashPrefix, String fullHash) {
        return getAssetsDirectory().resolve("objects").resolve(hashPrefix).resolve(fullHash);
    }

}
