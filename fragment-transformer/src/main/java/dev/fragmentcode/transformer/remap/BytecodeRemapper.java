package dev.fragmentcode.transformer.remap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;

/**
 * Применяет MojangRemapper к сырому байткоду класса.
 *
 * Стандартный ASM pipeline для ремаппинга:
 *   ClassReader (читает байты)
 *   -> InvokeDynamicFixingClassVisitor (исправляет имена методов внутри
 *      lambda Handle - см. javadoc этого класса для деталей проблемы)
 *   -> ClassRemapper (визитор, переписывающий имена через Remapper)
 *   -> ClassWriter (собирает новые байты)
 *
 * ПОРЯДОК ВАЖЕН: InvokeDynamicFixingClassVisitor должен идти ДО
 * ClassRemapper, потому что наш visitor ищет метод в MappingTable по
 * OBFUSCATED owner+name+desc (то есть по тому, что физически записано
 * в оригинальном байткоде) - если бы ClassRemapper сработал первым, он
 * уже переименовал бы owner в readable форму, и наш поиск по obfuscated
 * имени класса перестал бы находить совпадения.
 *
 * Используется ClassWriter.COMPUTE_MAXS, а НЕ COMPUTE_FRAMES:
 *   - Чистый ремаппинг ТОЛЬКО переименовывает классы/методы/поля, не
 *     меняя структуру байткода (порядок инструкций, форму стека,
 *     количество локальных переменных) - оригинальные stack map frames
 *     остаются структурно валидными, их достаточно скопировать с
 *     переименованными ссылками (это ClassRemapper делает автоматически
 *     через remapper.map() при визите frame-данных из ClassReader).
 *   - COMPUTE_FRAMES требует разрешения общего суперкласса для каждого
 *     слияния типов в стеке через ClassWriter.getCommonSuperClass(),
 *     который пытается ЗАГРУЗИТЬ оба класса через переданный classloader.
 *     На момент ремаппинга client.jar классы ещё ОБФУСЦИРОВАНЫ на диске -
 *     ссылка на класс по ЧИТАЕМОМУ имени (уже переименованному текущим
 *     ремаппингом) не найдёт его, так как остальные классы jar'а ещё
 *     не ремаппнуты физически. Это классическая проблема курицы и яйца,
 *     которая раньше проявлялась как TypeNotPresentException/
 *     ClassNotFoundException на классах, ещё не обработанных в текущем
 *     проходе JarRemapper.
 */
public final class BytecodeRemapper {

    private final MojangRemapper remapper;
    private final dev.fragmentcode.transformer.mapping.MappingTable mappingTable;

    public BytecodeRemapper(MojangRemapper remapper, dev.fragmentcode.transformer.mapping.MappingTable mappingTable) {
        this.remapper = remapper;
        this.mappingTable = mappingTable;
    }

    public byte[] remap(byte[] originalBytecode) {

        ClassReader reader = new ClassReader(originalBytecode);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        ClassRemapper classRemapper = new ClassRemapper(writer, remapper);
        InvokeDynamicFixingClassVisitor invokeDynamicFixer =
                new InvokeDynamicFixingClassVisitor(Opcodes.ASM9, classRemapper, mappingTable);

        reader.accept(invokeDynamicFixer, ClassReader.EXPAND_FRAMES);

        return writer.toByteArray();

    }

}
