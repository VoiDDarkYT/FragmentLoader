package dev.fragmentcode.transformer.mapping;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбирает текст client_mappings (ProGuard формат, как публикует Mojang)
 * в MappingTable.
 *
 * Формат файла (см. реальный пример, полученный из Mojang client.txt):
 *
 *   com.mojang.blaze3d.audio.Channel -> dft:
 *       org.apache.logging.log4j.Logger LOGGER -> a
 *       int source -> b
 *       31:37:com.mojang.blaze3d.audio.Channel create() -> a
 *       22:42:void lambda$matrixMode$35(int) -> lambda$matrixMode$35
 *
 * Три вида строк:
 *   1. Заголовок класса (без отступа, заканчивается ":"):
 *      "<readable.Class.Name> -> <obfuscatedName>:"
 *   2. Поле (с отступом, без номеров строк и без скобок):
 *      "<type> <name> -> <obfuscatedName>"
 *   3. Метод (с отступом, может начинаться с "<start>:<end>:", есть скобки):
 *      "[<start>:<end>:]<returnType> <name>(<paramTypes>) -> <obfuscatedName>"
 *
 * Строки комментариев (начинаются с "#") и пустые строки игнорируются.
 */
public final class MappingFileParser {

    // Заголовок класса: "a.b.C -> xyz:"
    private static final Pattern CLASS_HEADER = Pattern.compile(
            "^([\\w.$]+)\\s*->\\s*([\\w.$]+):$"
    );

    // Метод: опциональные номера строк "12:34:", тип возврата, имя,
    // параметры в скобках, "-> obfName"
    private static final Pattern METHOD_LINE = Pattern.compile(
            "^(?:\\d+:\\d+:)?([\\w.$\\[\\]]+)\\s+([\\w$]+)\\(([^)]*)\\)\\s*->\\s*([\\w$]+)$"
    );

    // Поле: тип, имя, "-> obfName" (без скобок, без номеров строк)
    private static final Pattern FIELD_LINE = Pattern.compile(
            "^([\\w.$\\[\\]]+)\\s+([\\w$]+)\\s*->\\s*([\\w$]+)$"
    );

    public MappingTable parse(String mappingFileContent) {

        MappingTable table = new MappingTable();
        ClassMapping currentClass = null;

        for (String rawLine : mappingFileContent.split("\n")) {

            String line = rawLine.stripTrailing();

            if (line.isBlank() || line.stripLeading().startsWith("#")) {
                continue;
            }

            boolean indented = line.startsWith(" ") || line.startsWith("\t");
            String trimmed = line.trim();

            if (!indented) {

                ClassMapping parsed = tryParseClassHeader(trimmed);

                if (parsed != null) {

                    if (currentClass != null) {
                        table.addClass(currentClass);
                    }

                    currentClass = parsed;

                }

                continue;

            }

            if (currentClass == null) {
                // Строка с отступом до объявления класса - повреждённый
                // файл или строка, которую мы не распознали; пропускаем.
                continue;
            }

            tryParseMember(trimmed, currentClass);

        }

        if (currentClass != null) {
            table.addClass(currentClass);
        }

        table.buildReverseIndex();

        return table;

    }

    private ClassMapping tryParseClassHeader(String line) {

        Matcher matcher = CLASS_HEADER.matcher(line);

        if (!matcher.matches()) {
            return null;
        }

        String readableName = matcher.group(1).replace('.', '/');
        String obfuscatedName = matcher.group(2).replace('.', '/');

        return new ClassMapping(readableName, obfuscatedName);

    }

    private void tryParseMember(String line, ClassMapping currentClass) {

        Matcher methodMatcher = METHOD_LINE.matcher(line);

        if (methodMatcher.matches()) {

            String returnType = methodMatcher.group(1);
            String methodName = methodMatcher.group(2);
            String paramTypes = methodMatcher.group(3);
            String obfuscatedName = methodMatcher.group(4);

            String descriptor = JvmDescriptorBuilder.buildMethodDescriptor(paramTypes, returnType);

            currentClass.addMethod(methodName, descriptor, obfuscatedName);

            return;

        }

        Matcher fieldMatcher = FIELD_LINE.matcher(line);

        if (fieldMatcher.matches()) {

            String fieldType = fieldMatcher.group(1);
            String fieldName = fieldMatcher.group(2);
            String obfuscatedName = fieldMatcher.group(3);

            String descriptor = JvmDescriptorBuilder.buildFieldDescriptor(fieldType);

            currentClass.addField(fieldName, descriptor, obfuscatedName);

        }

        // Строки, не подошедшие ни под один паттерн (например непредвиденный
        // формат), молча пропускаются - это безопаснее, чем бросать
        // исключение на каждой версии игры с малейшим изменением формата.

    }

}
