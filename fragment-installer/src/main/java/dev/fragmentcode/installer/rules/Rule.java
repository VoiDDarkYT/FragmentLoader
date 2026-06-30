package dev.fragmentcode.installer.rules;

/**
 * Одно правило из массива "rules" у library или у аргумента.
 * Пример: { "action": "allow", "os": { "name": "linux" } }
 *
 * action может быть "allow" или "disallow".
 * Если блок "os" отсутствует — правило применяется к любой ОС.
 */
public final class Rule {

    private String action;
    private OsRule os;

    public String getAction() {
        return action;
    }

    public OsRule getOs() {
        return os;
    }

    public boolean isAllow() {
        return "allow".equals(action);
    }

}
