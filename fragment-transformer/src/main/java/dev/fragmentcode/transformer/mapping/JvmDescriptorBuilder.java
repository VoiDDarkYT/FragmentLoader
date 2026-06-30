package dev.fragmentcode.transformer.mapping;

/**
 * Конвертирует типы Java, как они записаны в mappings (например
 * "net.minecraft.world.phys.Vec3", "int", "float[]"), в формат JVM
 * descriptor (например "Lnet/minecraft/world/phys/Vec3;", "I", "[F").
 *
 * Нужно, потому что mappings записаны в человекочитаемом Java-синтаксисе,
 * а ASM/JVM работают с дескрипторами - без конвертации нельзя построить
 * ключ для поиска метода (readableName + descriptor), который однозначно
 * отличает перегруженные методы.
 */
public final class JvmDescriptorBuilder {

    private JvmDescriptorBuilder() {
    }

    /**
     * Строит полный дескриптор метода из списка типов параметров (в
     * Java-синтаксисе, через запятую, как в mappings) и типа возврата.
     *
     * Пример: paramTypes="int,float", returnType="void" -> "(IF)V"
     */
    public static String buildMethodDescriptor(String paramTypesJoined, String returnType) {

        StringBuilder descriptor = new StringBuilder("(");

        if (!paramTypesJoined.isEmpty()) {

            for (String paramType : splitTopLevelCommas(paramTypesJoined)) {
                descriptor.append(toFieldDescriptor(paramType.trim()));
            }

        }

        descriptor.append(")");
        descriptor.append(toFieldDescriptor(returnType.trim()));

        return descriptor.toString();

    }

    /**
     * Дескриптор одного поля (используется и как "тип возврата" в ключе
     * для полей, чтобы соответствовать структуре ключа методов).
     */
    public static String buildFieldDescriptor(String fieldType) {
        return toFieldDescriptor(fieldType.trim());
    }

    /**
     * Конвертирует один Java-тип в JVM field descriptor.
     * Поддерживает примитивы, массивы (с произвольной вложенностью []),
     * и ссылочные типы (по полному имени с точками -> internal name
     * со слешами).
     */
    private static String toFieldDescriptor(String javaType) {

        int arrayDepth = 0;
        String baseType = javaType;

        while (baseType.endsWith("[]")) {
            arrayDepth++;
            baseType = baseType.substring(0, baseType.length() - 2).trim();
        }

        String descriptor = switch (baseType) {
            case "void" -> "V";
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "char" -> "C";
            case "short" -> "S";
            case "int" -> "I";
            case "long" -> "J";
            case "float" -> "F";
            case "double" -> "D";
            default -> "L" + baseType.replace('.', '/') + ";";
        };

        return "[".repeat(arrayDepth) + descriptor;

    }

    /**
     * Разбивает список параметров через запятую, игнорируя запятые
     * внутри generic-типов - на практике ProGuard mappings описывают
     * erased (raw) типы метода без generics, но top-level split всё
     * равно делается аккуратно на случай нестандартных записей.
     */
    private static java.util.List<String> splitTopLevelCommas(String input) {

        java.util.List<String> result = new java.util.ArrayList<>();
        int depth = 0;
        int start = 0;

        for (int i = 0; i < input.length(); i++) {

            char c = input.charAt(i);

            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (c == ',' && depth == 0) {
                result.add(input.substring(start, i));
                start = i + 1;
            }

        }

        result.add(input.substring(start));

        return result;

    }

}
