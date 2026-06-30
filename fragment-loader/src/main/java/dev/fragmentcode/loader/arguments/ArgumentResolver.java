package dev.fragmentcode.loader.arguments;

import dev.fragmentcode.installer.rules.Rule;
import dev.fragmentcode.installer.rules.RuleEvaluator;
import dev.fragmentcode.installer.version.Argument;
import dev.fragmentcode.installer.version.ArgumentList;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разворачивает ArgumentList (из version.json) в финальный список строк,
 * готовый передать в ProcessBuilder/Method.invoke:
 *
 *   1. Для каждого Argument решает, нужно ли его включать:
 *      - если есть rules с условием по os -> проверяем через RuleEvaluator
 *      - если условие по features (demo, custom resolution) -> ПРОПУСКАЕМ
 *        этот аргумент целиком (договорились не поддерживать features пока)
 *      - без условий -> всегда включается
 *   2. Подставляет ${placeholder} в каждое значение через LaunchContext.
 */
public final class ArgumentResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}");

    private final RuleEvaluator ruleEvaluator = new RuleEvaluator();

    public List<String> resolve(ArgumentList argumentList, LaunchContext context) {

        List<String> result = new ArrayList<>();

        if (argumentList == null) {
            return result;
        }

        for (Argument argument : argumentList.getArguments()) {

            if (!shouldInclude(argument)) {
                continue;
            }

            for (String rawValue : argument.getValues()) {
                result.add(substitutePlaceholders(rawValue, context));
            }

        }

        return result;

    }

    private boolean shouldInclude(Argument argument) {

        if (!argument.isConditional()) {
            return true;
        }

        for (Rule rule : argument.getRules()) {

            if (rule.getOs() == null) {
                // Правило без "os" в нашем сценарии означает условие по
                // "features" (demo/custom resolution) - мы их не поддерживаем
                // сейчас, поэтому такой аргумент всегда пропускаем.
                return false;
            }

        }

        return ruleEvaluator.isAllowed(argument.getRules());

    }

    private String substitutePlaceholders(String raw, LaunchContext context) {

        Matcher matcher = PLACEHOLDER.matcher(raw);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {

            String placeholder = matcher.group(1);
            String value = context.resolve(placeholder);

            matcher.appendReplacement(result, Matcher.quoteReplacement(value));

        }

        matcher.appendTail(result);

        return result.toString();

    }

}
