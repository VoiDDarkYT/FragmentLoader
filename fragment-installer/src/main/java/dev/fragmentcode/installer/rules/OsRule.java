package dev.fragmentcode.installer.rules;

/**
 * Соответствует блоку "os": { "name": "linux", "arch": "x86" } внутри rules.
 * Оба поля опциональны в JSON — если поле null, оно не участвует в проверке.
 */
public final class OsRule {

    private String name;
    private String arch;
    private String version;

    public String getName() {
        return name;
    }

    public String getArch() {
        return arch;
    }

    public String getVersion() {
        return version;
    }

}
