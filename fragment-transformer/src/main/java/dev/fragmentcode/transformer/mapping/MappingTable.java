package dev.fragmentcode.transformer.mapping;

import java.util.HashMap;
import java.util.Map;

/**
 * Полная таблица маппингов для всех классов игры, распарсенная из
 * client_mappings (client.txt).
 *
 * Поддерживает поиск как по readable, так и по obfuscated имени класса -
 * ASM Remapper при ремаппинге байткода работает в направлении
 * "obfuscated -> readable" (он получает оригинальный байткод с
 * obfuscated именами и должен решить, как их переименовать), поэтому
 * основной метод поиска - by obfuscated name, а readable-направление
 * используется при построении самой таблицы и для отладки.
 */
public final class MappingTable {

    private final Map<String, ClassMapping> byObfuscatedClassName = new HashMap<>();
    private final Map<String, ClassMapping> byReadableClassName = new HashMap<>();

    // Глобальный индекс "имя+дескриптор -> MappingEntry" без привязки
    // к конкретному owner-классу - используется для ремаппинга
    // invokedynamic-ссылок (лямбды/method references), где базовый ASM
    // API (двухаргументная версия mapInvokeDynamicMethodName) не даёт
    // напрямую узнать owner метода. См. MojangRemapper.mapInvokeDynamicMethodName.
    private final Map<String, MappingEntry> globalMethodIndex = new HashMap<>();

    public void addClass(ClassMapping classMapping) {
        byObfuscatedClassName.put(classMapping.getObfuscatedClassName(), classMapping);
        byReadableClassName.put(classMapping.getReadableClassName(), classMapping);
    }

    public ClassMapping findByObfuscatedName(String obfuscatedName) {
        return byObfuscatedClassName.get(obfuscatedName);
    }

    public ClassMapping findByReadableName(String readableName) {
        return byReadableClassName.get(readableName);
    }

    public int classCount() {
        return byObfuscatedClassName.size();
    }

    /**
     * Ищет метод по имени+дескриптору среди ВСЕХ классов (без учёта
     * owner) - используется только для invokedynamic-ремаппинга, где
     * owner лямбда-метода недоступен напрямую через двухаргументный
     * ASM API. Если несколько классов содержат метод с одинаковым
     * именем+дескриптором, возвращается первый найденный (на практике
     * для лямбда-методов это редкая коллизия, так как дескриптор
     * включает захваченные параметры, специфичные для конкретной лямбды).
     */
    public MappingEntry findAnyMethodByObfuscated(String obfuscatedName, String obfuscatedDescriptor) {
        return globalMethodIndex.get(obfuscatedName + obfuscatedDescriptor);
    }

    /**
     * Ищет метод по obfuscated имени+дескриптору, начиная с указанного
     * класса и поднимаясь по цепочке суперклассов, если не найден
     * напрямую - нужно для разрешения invokestatic/invokevirtual
     * ссылок на УНАСЛЕДОВАННЫЕ методы: Java-компилятор иногда записывает
     * owner вызова как подкласс, даже если метод реально объявлен в
     * родительском классе (легальное поведение JVM для статических
     * методов в частности).
     *
     * Подъём ограничен 50 уровнями - защита от случайного бесконечного
     * цикла, если граф наследования содержит ошибку (не должно
     * происходить в нормальном случае, так как java.lang.Object всегда
     * является конечной точкой иерархии, и у java.lang.Object нет записи
     * в MappingTable, что естественно прерывает цикл).
     */
    public MappingEntry findMethodWithInheritance(String obfuscatedOwner, String obfuscatedName, String obfuscatedDescriptor) {

        java.util.Deque<String> toVisit = new java.util.ArrayDeque<>();
        java.util.Set<String> visited = new java.util.HashSet<>();

        toVisit.add(obfuscatedOwner);

        while (!toVisit.isEmpty() && visited.size() < 200) {

            String currentOwner = toVisit.poll();

            if (currentOwner == null || !visited.add(currentOwner)) {
                continue;
            }

            ClassMapping mapping = byObfuscatedClassName.get(currentOwner);

            if (mapping == null) {
                continue;
            }

            MappingEntry entry = mapping.findMethodByObfuscated(obfuscatedName, obfuscatedDescriptor);

            if (entry != null) {
                return entry;
            }

            // Метод может быть объявлен в суперклассе ИЛИ в одном из
            // реализуемых интерфейсов (включая default-методы, или
            // случаи когда компилятор записал owner = реализующий класс,
            // хотя метод физически объявлен в интерфейсе) - обходим оба
            // пути, продолжая поиск вверх по всей иерархии.
            if (mapping.getObfuscatedSuperClassName() != null) {
                toVisit.add(mapping.getObfuscatedSuperClassName());
            }

            toVisit.addAll(mapping.getObfuscatedInterfaceNames());

        }

        return null;

    }

    /**
     * Аналог findMethodWithInheritance, но для полей - поля тоже могут
     * быть объявлены в родительском классе и доступны через подкласс
     * (например protected/public поле базового класса), и Java-компилятор
     * записывает Fieldref с owner = подкласс в таких случаях, точно так
     * же как для унаследованных статических методов (см. findMethodWithInheritance).
     * Без подъёма по иерархии такие поля остаются обфусцированными после
     * переименования реального класса-владельца, что приводит к
     * NoSuchFieldError при попытке доступа к полю под старым именем.
     */
    public MappingEntry findFieldWithInheritance(String obfuscatedOwner, String obfuscatedName) {

        java.util.Deque<String> toVisit = new java.util.ArrayDeque<>();
        java.util.Set<String> visited = new java.util.HashSet<>();

        toVisit.add(obfuscatedOwner);

        while (!toVisit.isEmpty() && visited.size() < 200) {

            String currentOwner = toVisit.poll();

            if (currentOwner == null || !visited.add(currentOwner)) {
                continue;
            }

            ClassMapping mapping = byObfuscatedClassName.get(currentOwner);

            if (mapping == null) {
                continue;
            }

            MappingEntry entry = mapping.findFieldByObfuscated(obfuscatedName);

            if (entry != null) {
                return entry;
            }

            if (mapping.getObfuscatedSuperClassName() != null) {
                toVisit.add(mapping.getObfuscatedSuperClassName());
            }

            toVisit.addAll(mapping.getObfuscatedInterfaceNames());

        }

        return null;

    }

    /**
     * Строит обратный (obfuscated) индекс для всех классов - вызывается
     * один раз после того как все классы добавлены через addClass().
     *
     * Для каждого метода/поля их readable descriptor (например
     * "(Lnet/minecraft/world/phys/Vec3;)V") конвертируется в obfuscated
     * descriptor (например "(Ldfk;)V") путём замены каждого readable
     * internal name внутри дескриптора на соответствующий obfuscated -
     * это нужно, потому что ASM Remapper ищет методы/поля по тому, что
     * видит В РЕАЛЬНОМ (obfuscated) байткоде, а не по readable именам.
     */
    public void buildReverseIndex() {

        for (ClassMapping classMapping : byReadableClassName.values()) {

            for (MappingEntry method : classMapping.getAllMethods()) {

                String obfuscatedDescriptor = remapDescriptor(method.getDescriptor());
                classMapping.registerObfuscatedMethod(obfuscatedDescriptor, method);

                globalMethodIndex.put(method.getObfuscatedName() + obfuscatedDescriptor, method);

            }

            for (MappingEntry field : classMapping.getAllFields()) {
                classMapping.registerObfuscatedField(field);
            }

        }

    }

    /**
     * Заменяет каждое readable internal class name внутри дескриптора
     * (формат "Lpackage/ClassName;") на соответствующее obfuscated имя,
     * если такой класс есть в таблице маппингов. Примитивы, массивы
     * и классы без маппинга (например java.lang.String) остаются как есть.
     */
    private String remapDescriptor(String descriptor) {

        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < descriptor.length()) {

            char c = descriptor.charAt(i);

            if (c == 'L') {

                int semicolon = descriptor.indexOf(';', i);
                String internalName = descriptor.substring(i + 1, semicolon);

                ClassMapping referenced = byReadableClassName.get(internalName);
                String replacement = referenced != null ? referenced.getObfuscatedClassName() : internalName;

                result.append('L').append(replacement).append(';');

                i = semicolon + 1;

            } else {

                result.append(c);
                i++;

            }

        }

        return result.toString();

    }

}
