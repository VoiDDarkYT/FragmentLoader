package dev.fragmentcode.installer.version;

import dev.fragmentcode.installer.rules.Rule;

import java.util.List;
import java.util.Map;

/**
 * Одна запись из массива "libraries" в version.json. Пример:
 *
 * {
 *   "name": "org.lwjgl:lwjgl-glfw:3.3.3:natives-linux",
 *   "downloads": {
 *     "artifact": { "path": "...", "sha1": "...", "size": ..., "url": "..." }
 *   },
 *   "rules": [ { "action": "allow", "os": { "name": "linux" } } ]
 * }
 *
 * "name" имеет формат maven coordinates: group:artifact:version[:classifier]
 * Библиотеки с classifier вроде "natives-linux" — это платформенные .so/.dll/.dylib,
 * упакованные в jar; их нужно распаковать в отдельную папку (natives directory),
 * а не просто положить в classpath.
 */
public final class Library {

    private String name;
    private LibraryDownloads downloads;
    private List<Rule> rules;

    public String getName() {
        return name;
    }

    public LibraryDownloads getDownloads() {
        return downloads;
    }

    public List<Rule> getRules() {
        return rules;
    }

    /**
     * Натив-библиотека определяется по наличию classifier вида "natives-*"
     * в имени (после второго ":").
     */
    public boolean isNatives() {
        return name != null && name.contains(":natives");
    }

    /**
     * Возвращает суффикс classifier после "natives-", например "windows",
     * "windows-x86", "windows-arm64", "linux", "macos-arm64".
     * Возвращает null, если это не natives-библиотека.
     *
     * Используется как дополнительная (помимо RuleEvaluator/rules) проверка
     * архитектуры: некоторые version.json от Mojang не указывают явный
     * "arch" в rules для x86-вариантов на Windows (rules там идентичны
     * обычному 64-битному варианту), из-за чего RuleEvaluator пропускает
     * ОБА варианта как разрешённые, и оба распаковываются в одну и ту же
     * папку natives, перезаписывая друг друга - в зависимости от порядка
     * в списке остаётся то 32-битный, то 64-битный .dll. Имя classifier
     * (явно содержащее "-x86" или "-arm64") - надёжный сигнал архитектуры
     * независимо от того, насколько подробно заполнены rules.
     */
    public String getNativesClassifierSuffix() {

        if (!isNatives()) {
            return null;
        }

        int idx = name.indexOf(":natives-");
        return name.substring(idx + ":natives-".length());

    }

    public static final class LibraryDownloads {

        private DownloadInfo artifact;
        private Map<String, DownloadInfo> classifiers;

        public DownloadInfo getArtifact() {
            return artifact;
        }

        public Map<String, DownloadInfo> getClassifiers() {
            return classifiers;
        }

    }

}
