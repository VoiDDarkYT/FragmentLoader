package dev.fragmentcode.installer.version;

import dev.fragmentcode.installer.rules.Rule;

import java.util.List;

/**
 * Один элемент из "arguments.game" или "arguments.jvm".
 *
 * В JSON это может быть:
 *   1. Просто строка:           "--username"
 *   2. Условный объект:         { "rules": [...], "value": "--demo" }
 *                                "value" может быть строкой ИЛИ массивом строк.
 *
 * Этот класс — унифицированное представление обоих случаев после парсинга
 * (см. ArgumentsDeserializer, который превращает оба варианта JSON в этот класс).
 */
public final class Argument {

    private final List<Rule> rules; // null, если аргумент безусловный
    private final List<String> values; // всегда список, даже если значение одно

    public Argument(List<Rule> rules, List<String> values) {
        this.rules = rules;
        this.values = values;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public List<String> getValues() {
        return values;
    }

    public boolean isConditional() {
        return rules != null;
    }

}
