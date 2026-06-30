package dev.fragmentcode.installer.version;

import java.util.List;

/**
 * Полная модель файла version.json конкретной версии Minecraft
 * (например https://piston-meta.mojang.com/v1/packages/.../1.21.7.json).
 *
 * Содержит всё необходимое, чтобы собрать classpath и аргументы запуска:
 *   - downloads.client      -> ссылка на client.jar
 *   - libraries              -> список зависимостей (с учётом OS-правил)
 *   - assetIndex              -> индекс ресурсов игры
 *   - mainClass               -> точка входа (net.minecraft.client.main.Main)
 *   - arguments.jvm/game      -> аргументы запуска JVM и самой игры
 *   - javaVersion             -> минимально нужная версия Java
 */
public final class VersionMetadata {

    private String id;
    private String type;
    private String mainClass;
    private String assets; // id индекса ресурсов, например "26"
    private int minimumLauncherVersion;

    private Downloads downloads;
    private AssetIndexInfo assetIndex;
    private List<Library> libraries;
    private Arguments arguments;
    private JavaVersionInfo javaVersion;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getMainClass() {
        return mainClass;
    }

    public String getAssets() {
        return assets;
    }

    public int getMinimumLauncherVersion() {
        return minimumLauncherVersion;
    }

    public Downloads getDownloads() {
        return downloads;
    }

    public AssetIndexInfo getAssetIndex() {
        return assetIndex;
    }

    public List<Library> getLibraries() {
        return libraries;
    }

    public Arguments getArguments() {
        return arguments;
    }

    public JavaVersionInfo getJavaVersion() {
        return javaVersion;
    }

    public static final class JavaVersionInfo {

        private String component;
        private int majorVersion;

        public String getComponent() {
            return component;
        }

        public int getMajorVersion() {
            return majorVersion;
        }

    }

}
