package dev.fragmentcode.installer.install;

import dev.fragmentcode.installer.version.VersionMetadata;

import java.nio.file.Path;
import java.util.List;

/**
 * Результат полной установки версии игры - всё, что нужно fragment-loader'у,
 * чтобы собрать classpath и запустить main-class.
 */
public final class GameInstallation {

    private final VersionMetadata metadata;
    private final Path clientJar;
    private final List<Path> libraryJars;
    private final Path nativesDirectory;
    private final Path assetsDirectory;
    private final Path gameDirectory;
    private final Path mappingsFile;

    public GameInstallation(
            VersionMetadata metadata,
            Path clientJar,
            List<Path> libraryJars,
            Path nativesDirectory,
            Path assetsDirectory,
            Path gameDirectory,
            Path mappingsFile
    ) {
        this.metadata = metadata;
        this.clientJar = clientJar;
        this.libraryJars = libraryJars;
        this.nativesDirectory = nativesDirectory;
        this.assetsDirectory = assetsDirectory;
        this.gameDirectory = gameDirectory;
        this.mappingsFile = mappingsFile;
    }

    public Path getMappingsFile() {
        return mappingsFile;
    }

    public VersionMetadata getMetadata() {
        return metadata;
    }

    public Path getClientJar() {
        return clientJar;
    }

    public List<Path> getLibraryJars() {
        return libraryJars;
    }

    public Path getNativesDirectory() {
        return nativesDirectory;
    }

    public Path getAssetsDirectory() {
        return assetsDirectory;
    }

    public Path getGameDirectory() {
        return gameDirectory;
    }

    /**
     * Полный classpath для запуска: client.jar + все обычные библиотеки.
     */
    public List<Path> buildClasspath() {

        List<Path> classpath = new java.util.ArrayList<>(libraryJars);
        classpath.add(clientJar);

        return classpath;

    }

}
