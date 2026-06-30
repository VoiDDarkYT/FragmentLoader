package dev.fragmentcode.transformer.mapping;

/**
 * Одна запись маппинга метода или поля внутри класса.
 *
 * Для полей: descriptor содержит только тип возвращаемого значения
 * (так как у поля нет параметров) - используется как часть ключа на
 * случай переименования полей с одинаковым именем но разным типом
 * (редкий случай, но возможен при наследовании/перегрузке).
 *
 * Для методов: descriptor - это JVM-дескриптор параметров и возврата,
 * например "(I)V" для "void method(int)" - необходим, потому что
 * ProGuard mappings могут схлопывать РАЗНЫЕ перегруженные методы
 * в ОДНО obfuscated имя (например все "a"), и без дескриптора нельзя
 * однозначно определить, какой именно метод имеется в виду.
 */
public final class MappingEntry {

    private final String readableName;
    private final String obfuscatedName;
    private final String descriptor;

    public MappingEntry(String readableName, String obfuscatedName, String descriptor) {
        this.readableName = readableName;
        this.obfuscatedName = obfuscatedName;
        this.descriptor = descriptor;
    }

    public String getReadableName() {
        return readableName;
    }

    public String getObfuscatedName() {
        return obfuscatedName;
    }

    public String getDescriptor() {
        return descriptor;
    }

}
