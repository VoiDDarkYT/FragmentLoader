package dev.fragmentcode.transformer.mixin;

import dev.fragmentcode.api.transform.ClassTransformer;
import dev.fragmentcode.transformer.mixin.apply.InjectApplier;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Set;

/**
 * Реализация ClassTransformer (интерфейс из fragment-api, который
 * FragmentClassLoader вызывает перед определением каждого класса) —
 * применяет все зарегистрированные mixin'ы для данного класса.
 *
 * Если для класса нет зарегистрированных mixin'ов (подавляющее
 * большинство классов игры), байткод возвращается без изменений —
 * лишняя нагрузка минимальна (один lookup в HashMap MixinRegistry).
 *
 * Использует ASM tree API (ClassNode/MethodNode), а не стримовый
 * ClassVisitor, потому что &#64;Inject(at = INVOKE) требует ПОИСКА
 * конкретной инструкции внутри метода — что естественно делать на
 * дереве инструкций, а не "на лету" во время визита потока байткода.
 */
public final class MixinClassTransformer implements ClassTransformer {

    private final MixinRegistry registry;
    private final InjectApplier injectApplier = new InjectApplier();
    private final ClassLoader classLoaderForFrameComputation;

    /**
     * @param classLoaderForFrameComputation  classloader, через который
     *        ClassWriter будет искать классы при пересчёте stack map
     *        frames (COMPUTE_FRAMES) — должен быть тот же classloader,
     *        что загружает классы игры (обычно сам FragmentClassLoader,
     *        который вызывает этот transform()), а не classloader самого
     *        fragment-transformer — иначе ASM не найдёт классы Minecraft
     *        при вычислении иерархии для frames.
     */
    public MixinClassTransformer(MixinRegistry registry, ClassLoader classLoaderForFrameComputation) {
        this.registry = registry;
        this.classLoaderForFrameComputation = classLoaderForFrameComputation;
    }

    @Override
    public byte[] transform(String className, byte[] originalBytecode) {

        String internalName = className.replace('.', '/');

        boolean isTarget = registry.hasMixinsFor(internalName);
        Set<String> methodsToPublicize = registry.getMethodsToPublicize(internalName);
        boolean isModClassNeedingPatch = !methodsToPublicize.isEmpty();

        if (!isTarget && !isModClassNeedingPatch) {
            return originalBytecode;
        }

        ClassReader reader = new ClassReader(originalBytecode);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);

        if (isTarget) {

            List<MixinMetadata> mixins = registry.getMixinsFor(internalName);

            for (MixinMetadata mixin : mixins) {
                applyMixin(classNode, mixin);
            }

        }

        if (isModClassNeedingPatch) {
            publicizeMethods(classNode, methodsToPublicize);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {

            @Override
            protected ClassLoader getClassLoader() {
                return classLoaderForFrameComputation;
            }

        };

        classNode.accept(writer);

        return writer.toByteArray();

    }

    /**
     * Меняет модификатор доступа указанных методов мод-класса на public,
     * убирая private/protected — &#64;Inject/&#64;ModifyVariable методы
     * пишутся автором мода как private (привычный, безопасный по
     * умолчанию модификатор в обычном Java-коде), но вызываются из
     * target-класса через INVOKESTATIC из ДРУГОГО jar'а/пакета, что
     * требует видимости как минимум public между classloader'ами/модулями.
     *
     * Это снимает с автора мода необходимость помнить про модификаторы
     * доступа специально для mixin-методов.
     */
    private void publicizeMethods(ClassNode classNode, Set<String> methodsToPublicize) {

        for (MethodNode method : classNode.methods) {

            String key = method.name + method.desc;

            if (!methodsToPublicize.contains(key)) {
                continue;
            }

            method.access &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);
            method.access |= Opcodes.ACC_PUBLIC;

        }

    }

    private void applyMixin(ClassNode classNode, MixinMetadata mixin) {

        for (InjectPoint point : mixin.getInjectPoints()) {

            MethodNode targetMethod = findMethod(classNode, point.getTargetMethodName());

            if (targetMethod == null) {
                throw new MixinException(
                        "@Inject target method not found: " + point.getTargetMethodName()
                                + " in class " + classNode.name
                                + " (mixin: " + mixin.getMixinClassInternalName() + ")"
                );
            }

            injectApplier.apply(targetMethod, point);

        }

    }

    /**
     * Ищет метод по имени без учёта дескриптора — известное ограничение
     * текущей версии (см. MixinScanner): если в target-классе несколько
     * перегрузок с одинаковым именем, используется первая найденная.
     * Для подавляющего большинства методов Minecraft (особенно после
     * ремаппинга в читаемые имена) это не проблема, так как разные по
     * смыслу методы обычно имеют разные читаемые имена.
     */
    private MethodNode findMethod(ClassNode classNode, String methodName) {

        for (MethodNode method : classNode.methods) {

            if (method.name.equals(methodName)) {
                return method;
            }

        }

        return null;

    }

}
