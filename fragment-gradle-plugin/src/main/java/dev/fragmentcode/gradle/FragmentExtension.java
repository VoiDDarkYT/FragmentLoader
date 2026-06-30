package dev.fragmentcode.gradle;

/**
 * DSL-блок для настройки Fragment в build.gradle.kts мода:
 *
 * fragment {
 *     minecraftVersion = "1.21.7"
 * }
 */
public abstract class FragmentExtension {

    private String minecraftVersion;

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }

    // Kotlin property-style setter: minecraftVersion = "1.21.7"
    public void minecraftVersion(String version) {
        this.minecraftVersion = version;
    }

}
