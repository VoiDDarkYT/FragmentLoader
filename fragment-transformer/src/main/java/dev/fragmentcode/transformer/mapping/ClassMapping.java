package dev.fragmentcode.transformer.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Все маппинги полей и методов одного класса.
 *
 * readableClassName / obfuscatedClassName используют JVM internal form
 * (со слешами, например "net/minecraft/client/MinecraftClient"), а не
 * Java-form (с точками) - это формат, который ожидает ASM при работе
 * с байткодом, поэтому конвертация из формата mappings (с точками)
 * происходит один раз при парсинге, а не на каждом обращении.
 */
public final class ClassMapping {

    private final String readableClassName;
    private final String obfuscatedClassName;

    // Obfuscated имя суперкласса - заполняется отдельно (не из mappings,
    // а из реального байткода jar'а, см. JarRemapper.scanClassHierarchy),
    // так как client_mappings.txt сам по себе не содержит информацию о
    // наследовании, только имена/сигнатуры методов и полей. Нужно для
    // разрешения ссылок на УНАСЛЕДОВАННЫЕ статические методы - Java
    // компилятор иногда записывает invokestatic с owner = подкласс, даже
    // если метод реально объявлен в родительском классе (легальное
    // поведение JVM для статических методов), и без подъёма по иерархии
    // наследования такие методы остаются необфусцированными после
    // переименования родителя - см. MappingTable.findMethodWithInheritance.
    private String obfuscatedSuperClassName;

    // Obfuscated имена РЕАЛИЗУЕМЫХ интерфейсов - метод может быть
    // объявлен непосредственно в интерфейсе (а не только унаследован от
    // суперкласса через extends), и invokevirtual/invokeinterface может
    // ссылаться на owner = реализующий класс, хотя метод физически
    // объявлен в интерфейсе. Без учёта интерфейсов (только extends)
    // такие методы не находятся при подъёме по иерархии.
    private List<String> obfuscatedInterfaceNames = List.of();

    // Ключ для методов: readableName + descriptor (один readable-метод
    // может существовать только с одной сигнатурой в одном классе,
    // переопределения с разными параметрами - это разные методы Java,
    // поэтому readableName одного метода может повторяться только если
    // меняется descriptor - типичная перегрузка).
    private final Map<String, MappingEntry> methodsByReadableKey = new HashMap<>();

    // Аналогично для полей, но без параметров в дескрипторе.
    private final Map<String, MappingEntry> fieldsByReadableKey = new HashMap<>();

    // Обратные индексы по obfuscated имени+дескриптору - нужны ASM
    // Remapper'у, который видит СЫРОЙ obfuscated байткод и должен найти
    // readable-замену. Заполняются отдельным проходом ПОСЛЕ того как вся
    // MappingTable собрана (см. MappingTable.buildReverseIndex), потому
    // что дескриптор метода/поля может ссылаться на ДРУГИЕ классы, чьи
    // obfuscated имена нужно знать заранее, чтобы построить obfuscated
    // descriptor из readable descriptor.
    private final Map<String, MappingEntry> methodsByObfuscatedKey = new HashMap<>();
    private final Map<String, MappingEntry> fieldsByObfuscatedKey = new HashMap<>();

    public ClassMapping(String readableClassName, String obfuscatedClassName) {
        this.readableClassName = readableClassName;
        this.obfuscatedClassName = obfuscatedClassName;
    }

    public String getReadableClassName() {
        return readableClassName;
    }

    public String getObfuscatedClassName() {
        return obfuscatedClassName;
    }

    public void setObfuscatedSuperClassName(String obfuscatedSuperClassName) {
        this.obfuscatedSuperClassName = obfuscatedSuperClassName;
    }

    public String getObfuscatedSuperClassName() {
        return obfuscatedSuperClassName;
    }

    public void setObfuscatedInterfaceNames(List<String> obfuscatedInterfaceNames) {
        this.obfuscatedInterfaceNames = obfuscatedInterfaceNames;
    }

    public List<String> getObfuscatedInterfaceNames() {
        return obfuscatedInterfaceNames;
    }

    public void addMethod(String readableName, String descriptor, String obfuscatedName) {
        String key = readableName + descriptor;
        methodsByReadableKey.put(key, new MappingEntry(readableName, obfuscatedName, descriptor));
    }

    public void addField(String readableName, String descriptor, String obfuscatedName) {
        fieldsByReadableKey.put(readableName, new MappingEntry(readableName, obfuscatedName, descriptor));
    }

    /**
     * Ищет obfuscated имя метода по readable имени и дескриптору.
     * Возвращает null, если такого метода нет в этом классе (может быть
     * унаследован из суперкласса - такой случай обрабатывается на уровне
     * MappingTable, который умеет проверять иерархию классов).
     */
    public MappingEntry findMethod(String readableName, String descriptor) {
        return methodsByReadableKey.get(readableName + descriptor);
    }

    public MappingEntry findField(String readableName) {
        return fieldsByReadableKey.get(readableName);
    }

    public List<MappingEntry> getAllMethods() {
        return new ArrayList<>(methodsByReadableKey.values());
    }

    public List<MappingEntry> getAllFields() {
        return new ArrayList<>(fieldsByReadableKey.values());
    }

    /**
     * Регистрирует запись в обратном (obfuscated) индексе. Вызывается
     * MappingTable во время постобработки, когда obfuscated-дескриптор
     * уже построен (с учётом ремаппинга типов внутри самого дескриптора).
     */
    void registerObfuscatedMethod(String obfuscatedDescriptor, MappingEntry entry) {
        methodsByObfuscatedKey.put(entry.getObfuscatedName() + obfuscatedDescriptor, entry);
    }

    void registerObfuscatedField(MappingEntry entry) {
        fieldsByObfuscatedKey.put(entry.getObfuscatedName(), entry);
    }

    public MappingEntry findMethodByObfuscated(String obfuscatedName, String obfuscatedDescriptor) {
        return methodsByObfuscatedKey.get(obfuscatedName + obfuscatedDescriptor);
    }

    public MappingEntry findFieldByObfuscated(String obfuscatedName) {
        return fieldsByObfuscatedKey.get(obfuscatedName);
    }

}
