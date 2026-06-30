package dev.fragmentcode.transformer.remap;

import dev.fragmentcode.transformer.mapping.ClassMapping;
import dev.fragmentcode.transformer.mapping.MappingEntry;
import dev.fragmentcode.transformer.mapping.MappingTable;
import org.objectweb.asm.commons.Remapper;

/**
 * ASM Remapper, который при ремаппинге байткода заменяет obfuscated имена
 * классов/методов/полей на читаемые (Mojang official names), используя
 * заранее распарсенную MappingTable.
 *
 * ASM вызывает эти методы для каждого упоминания класса/метода/поля
 * внутри байткода (включая ссылки на ДРУГИЕ классы - например вызов
 * "minecraftClient.getWindow()" внутри байткода другого класса тоже
 * проходит через map()/mapMethodName()), поэтому одна правильно
 * настроенная MappingTable обеспечивает консистентный ремаппинг сразу
 * по всем классам, ссылающимся друг на друга.
 *
 * Классы, для которых нет записи в MappingTable (например классы из
 * java.lang, библиотек LWJGL/Guava/Netty и т.д. - они не обфусцированы
 * Mojang'ом и поэтому отсутствуют в client_mappings), остаются без
 * изменений - это ожидаемое и безопасное поведение по умолчанию (метод
 * Remapper.map возвращает оригинальное имя, если не переопределён).
 */
public final class MojangRemapper extends Remapper {

    private final MappingTable mappingTable;

    public MojangRemapper(MappingTable mappingTable) {
        this.mappingTable = mappingTable;
    }

    @Override
    public String map(String obfuscatedInternalName) {

        ClassMapping mapping = mappingTable.findByObfuscatedName(obfuscatedInternalName);

        if (mapping == null) {
            return obfuscatedInternalName;
        }

        return mapping.getReadableClassName();

    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {

        // Конструкторы и статические инициализаторы не имеют readable
        // замены - их имя фиксировано JVM-спецификацией.
        if (name.equals("<init>") || name.equals("<clinit>")) {
            return name;
        }

        // Поиск с подъёмом по иерархии наследования - Java-компилятор
        // иногда записывает Methodref с owner = подкласс, даже если
        // метод реально объявлен в родительском классе (легальное
        // поведение JVM, особенно для статических методов). Без подъёма
        // по иерархии такие методы остаются обфусцированными после того
        // как реальный владелец метода переименован - что приводит к
        // NoSuchMethodError при попытке вызвать метод под старым именем
        // через новый (переименованный) класс.
        MappingEntry entry = mappingTable.findMethodWithInheritance(owner, name, descriptor);

        if (entry == null) {
            return name;
        }

        return entry.getReadableName();

    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {

        // Поиск с подъёмом по иерархии наследования - аналогично
        // mapMethodName, поля тоже могут быть объявлены в родительском
        // классе, но доступны через подкласс (например protected/public
        // поле базового класса) - см. findFieldWithInheritance.
        MappingEntry entry = mappingTable.findFieldWithInheritance(owner, name);

        if (entry == null) {
            return name;
        }

        return entry.getReadableName();

    }

}
