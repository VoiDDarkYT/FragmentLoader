package dev.fragmentcode.installer.version;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.fragmentcode.installer.rules.Rule;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Mojang в "arguments.game" / "arguments.jvm" смешивает в одном JSON-массиве
 * обычные строки и объекты вида { "rules": [...], "value": "..." | [...] }.
 * Стандартный Gson не умеет десериализовать такой "полиморфный" массив
 * автоматически — поэтому пишем здесь вручную, элемент за элементом.
 *
 * Регистрируется в GsonBuilder через registerTypeAdapter(ArgumentList.class, ...),
 * см. VersionMetadataParser.
 */
public final class ArgumentsDeserializer implements JsonDeserializer<ArgumentList> {

    @Override
    public ArgumentList deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        List<Argument> result = new ArrayList<>();

        if (!json.isJsonArray()) {
            return new ArgumentList(result);
        }

        JsonArray array = json.getAsJsonArray();

        for (JsonElement element : array) {

            if (element.isJsonPrimitive()) {
                // Случай 1: обычная строка, без условий
                String value = element.getAsString();
                result.add(new Argument(null, List.of(value)));
                continue;
            }

            if (element.isJsonObject()) {
                // Случай 2: объект с rules + value (строка или массив строк)
                JsonObject obj = element.getAsJsonObject();

                List<Rule> rules = context.deserialize(obj.get("rules"), RuleListType.LIST_OF_RULE);

                JsonElement valueElement = obj.get("value");
                List<String> values = new ArrayList<>();

                if (valueElement.isJsonArray()) {
                    for (JsonElement v : valueElement.getAsJsonArray()) {
                        values.add(v.getAsString());
                    }
                } else {
                    values.add(valueElement.getAsString());
                }

                result.add(new Argument(rules, values));
            }

        }

        return new ArgumentList(result);

    }

    /**
     * Вспомогательный держатель типа для context.deserialize — Gson требует
     * java.lang.reflect.Type, а не просто Class, чтобы корректно понять
     * generic-список List<Rule>.
     */
    private static final class RuleListType {
        static final Type LIST_OF_RULE =
                com.google.gson.reflect.TypeToken.getParameterized(List.class, Rule.class).getType();
    }

}
