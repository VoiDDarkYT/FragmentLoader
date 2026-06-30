package dev.fragmentcode.installer.rules;

import java.util.List;

/**
 * Решает, должно ли что-то (library, jvm-аргумент, game-аргумент)
 * применяться на ТЕКУЩЕЙ операционной системе, на основе списка rules
 * из version.json.
 *
 * Логика Mojang (повторяем её точно, иначе на части ОС будут лишние/
 * отсутствующие библиотеки):
 *   - Если rules нет вообще -> разрешено всегда.
 *   - Иначе проходим правила по порядку, последнее совпавшее правило
 *     определяет результат. Изначально считаем "запрещено", если есть
 *     хотя бы одно правило с условием os (иначе оно бы не имело смысла).
 *
 * На практике у Mojang всегда либо нет rules (= всегда разрешено),
 * либо ровно одно правило "allow" с конкретной os — этого достаточно
 * для нашей реализации.
 */
public final class RuleEvaluator {

    private final String currentOsName;
    private final String currentArch;

    public RuleEvaluator() {

        this.currentOsName = detectOsName();
        this.currentArch = detectArch();

    }

    public boolean isAllowed(List<Rule> rules) {

        if (rules == null || rules.isEmpty()) {
            return true;
        }

        boolean allowed = false;

        for (Rule rule : rules) {

            if (matchesOs(rule.getOs())) {
                allowed = rule.isAllow();
            }

        }

        return allowed;

    }

    public String getCurrentOsName() {
        return currentOsName;
    }

    public String getCurrentArch() {
        return currentArch;
    }

    private boolean matchesOs(OsRule os) {

        if (os == null) {
            // Правило без условия os применяется к любой платформе.
            return true;
        }

        if (os.getName() != null && !os.getName().equals(currentOsName)) {
            return false;
        }

        if (os.getArch() != null && !os.getArch().equals(currentArch)) {
            return false;
        }

        return true;

    }

    private String detectOsName() {

        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return "windows";
        }

        if (os.contains("mac")) {
            return "osx";
        }

        return "linux";

    }

    private String detectArch() {

        String arch = System.getProperty("os.arch").toLowerCase();

        if (arch.equals("x86") || arch.equals("i386") || arch.equals("i686")) {
            return "x86";
        }

        if (arch.equals("amd64") || arch.equals("x86_64")) {
            return "x86_64";
        }

        if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "arm64";
        }

        return arch;

    }

}
