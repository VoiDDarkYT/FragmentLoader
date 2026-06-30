package dev.fragmentcode.transformer.remap;

import dev.fragmentcode.transformer.mapping.ClassMapping;
import dev.fragmentcode.transformer.mapping.MappingTable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Ремаппит весь client.jar целиком, заменяя obfuscated имена классов,
 * методов и полей на читаемые (Mojang official mappings) - аналогично
 * тому, что делает TinyRemapper у Fabric, но проще (без поддержки
 * множественных слоёв mappings, нам нужен только один - readable).
 *
 * В отличие от ремаппинга "на лету" в момент загрузки класса, здесь весь
 * jar ремаппится ОДИН РАЗ при установке версии, результат сохраняется
 * на диск, и при каждом следующем запуске игры loader просто использует
 * уже готовый remapped jar - реальный ремаппинг не повторяется.
 *
 * Не использует ClassWriter.COMPUTE_FRAMES (см. BytecodeRemapper) -
 * чистое переименование не требует разрешения classpath, поэтому
 * libraryJars сейчас не используется напрямую в самом ремаппинге
 * (параметр оставлен в сигнатуре для совместимости вызывающего кода
 * и на случай будущих нужд, например диагностики).
 */
public final class JarRemapper {

    private final MappingTable mappingTable;

    public JarRemapper(MappingTable mappingTable) {
        this.mappingTable = mappingTable;
    }

    /**
     * @param sourceJar       оригинальный obfuscated client.jar
     * @param libraryJars     все library jar'ы (сейчас не используются
     *                        напрямую — оставлены в сигнатуре для
     *                        совместимости и возможного будущего
     *                        использования)
     * @param destinationJar  куда сохранить результат - новый jar с
     *                        читаемыми именами классов/методов/полей
     */
    public void remapJar(Path sourceJar, List<Path> libraryJars, Path destinationJar) throws IOException {

        // Предварительный проход: заполняем граф наследования (obfuscated
        // superName для каждого ClassMapping) ДО основного ремаппинга -
        // нужно для разрешения ссылок на УНАСЛЕДОВАННЫЕ статические
        // методы (Java-компилятор иногда записывает invokestatic с
        // owner = подкласс, даже если метод реально объявлен в
        // родительском классе - легальное поведение JVM). Без этой
        // информации MojangRemapper.mapMethodName не может подняться по
        // иерархии при поиске метода, который не найден напрямую в
        // указанном owner-классе - см. findMethodWithInheritance.
        scanClassHierarchy(sourceJar);

        MojangRemapper remapper = new MojangRemapper(mappingTable);
        BytecodeRemapper bytecodeRemapper = new BytecodeRemapper(remapper, mappingTable);

        Files.createDirectories(destinationJar.toAbsolutePath().getParent());

        java.util.jar.Manifest manifest = new java.util.jar.Manifest();
        manifest.getMainAttributes().put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarFile inputJar = new JarFile(sourceJar.toFile());
             JarOutputStream outputJar = new JarOutputStream(Files.newOutputStream(destinationJar), manifest)) {

            var entries = inputJar.entries();

            while (entries.hasMoreElements()) {

                JarEntry entry = entries.nextElement();

                if (entry.isDirectory()) {
                    continue;
                }

                if (isSignatureRelatedEntry(entry.getName())) {
                    // META-INF/MANIFEST.MF и файлы подписи (*.SF, *.RSA,
                    // *.DSA, *.EC) содержат хеши оригинальных .class
                    // файлов (digest по алгоритмам типа SHA-384). Так как
                    // мы меняем содержимое .class файлов при ремаппинге,
                    // эти хеши становятся неверными - перенос их как есть
                    // приводил к SecurityException ("digest error") при
                    // попытке JVM проверить подпись изменённого jar'а.
                    // Подпись для модифицированного jar'а смысла не имеет
                    // в любом случае - пропускаем эти записи целиком.
                    continue;
                }

                if (!entry.getName().endsWith(".class")) {
                    // Остальные ресурсы (например встроенные .json и т.д.)
                    // копируются как есть, без изменений - ремаппинг
                    // касается только байткода.
                    copyEntryAsIs(inputJar, entry, outputJar);
                    continue;
                }

                remapClassEntry(inputJar, entry, outputJar, bytecodeRemapper);

            }

        }

    }

    /**
     * Читает заголовок (без code/инструкций) каждого .class файла в jar'е,
     * чтобы узнать его obfuscated superName, и записывает эту информацию
     * в соответствующий ClassMapping. Используется ClassReader.SKIP_CODE
     * | SKIP_DEBUG | SKIP_FRAMES - нам нужен только заголовок класса
     * (имя + superName), не весь байткод методов, что делает этот проход
     * быстрым даже для тысяч классов.
     *
     * Классы, отсутствующие в MappingTable (не обфусцированные Mojang'ом
     * библиотечные классы), просто пропускаются - их иерархия наследования
     * не нужна для разрешения наших mappings.
     */
    private void scanClassHierarchy(Path sourceJar) throws IOException {

        try (JarFile jarFile = new JarFile(sourceJar.toFile())) {

            var entries = jarFile.entries();

            while (entries.hasMoreElements()) {

                JarEntry entry = entries.nextElement();

                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }

                byte[] classBytecode;

                try (InputStream in = jarFile.getInputStream(entry)) {
                    classBytecode = in.readAllBytes();
                }

                recordSuperClass(classBytecode);

            }

        }

    }

    private void recordSuperClass(byte[] classBytecode) {

        ClassReader reader = new ClassReader(classBytecode);

        reader.accept(new ClassVisitor(Opcodes.ASM9) {

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {

                ClassMapping mapping = mappingTable.findByObfuscatedName(name);

                if (mapping == null) {
                    return;
                }

                if (superName != null) {
                    mapping.setObfuscatedSuperClassName(superName);
                }

                if (interfaces != null && interfaces.length > 0) {
                    mapping.setObfuscatedInterfaceNames(java.util.List.of(interfaces));
                }

            }

        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

    }

    /**
     * Определяет, относится ли файл к подписи/манифесту jar'а -
     * MANIFEST.MF содержит digest-хеши классов (становятся неверными
     * после ремаппинга), а *.SF/*.RSA/*.DSA/*.EC - это сама цифровая
     * подпись, бессмысленная для изменённого jar'а.
     */
    private boolean isSignatureRelatedEntry(String entryName) {

        if (!entryName.startsWith("META-INF/")) {
            return false;
        }

        String upperName = entryName.toUpperCase();

        return upperName.equals("META-INF/MANIFEST.MF")
                || upperName.endsWith(".SF")
                || upperName.endsWith(".RSA")
                || upperName.endsWith(".DSA")
                || upperName.endsWith(".EC");

    }

    private void remapClassEntry(
            JarFile inputJar,
            JarEntry entry,
            JarOutputStream outputJar,
            BytecodeRemapper bytecodeRemapper
    ) throws IOException {

        byte[] originalBytecode;

        try (InputStream in = inputJar.getInputStream(entry)) {
            originalBytecode = in.readAllBytes();
        }

        byte[] remappedBytecode = bytecodeRemapper.remap(originalBytecode);

        // Имя класса внутри jar'а тоже должно стать читаемым - иначе
        // .class файл лежал бы под старым (obfuscated) путём, а внутри
        // содержал бы уже переименованный класс, что не совпадает с тем,
        // что ожидает JVM (путь файла в jar должен соответствовать
        // полному имени класса).
        String obfuscatedInternalName = entry.getName().substring(0, entry.getName().length() - ".class".length());
        String readableInternalName = remapInternalNameForJarPath(obfuscatedInternalName);

        JarEntry outputEntry = new JarEntry(readableInternalName + ".class");
        outputJar.putNextEntry(outputEntry);
        outputJar.write(remappedBytecode);
        outputJar.closeEntry();

    }

    private String remapInternalNameForJarPath(String obfuscatedInternalName) {

        var mapping = mappingTable.findByObfuscatedName(obfuscatedInternalName);

        if (mapping == null) {
            return obfuscatedInternalName;
        }

        return mapping.getReadableClassName();

    }

    private void copyEntryAsIs(JarFile inputJar, JarEntry entry, JarOutputStream outputJar) throws IOException {

        try (InputStream in = inputJar.getInputStream(entry)) {

            outputJar.putNextEntry(new JarEntry(entry.getName()));
            in.transferTo(outputJar);
            outputJar.closeEntry();

        }

    }

}
