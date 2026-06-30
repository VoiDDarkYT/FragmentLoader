package dev.fragmentcode.transformer.mixin;

import dev.fragmentcode.api.mixin.At;
import dev.fragmentcode.api.mixin.Inject;
import dev.fragmentcode.api.mixin.Mixin;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Читает &#64;Mixin/&#64;Inject аннотации с класса через стандартную Java
 * рефлексию (mixin-классы — это обычные, нормально скомпилированные
 * классы, загруженные обычным classloader'ом, в отличие от классов
 * client.jar, которые мы патчим как сырой байткод).
 *
 * Результат — MixinMetadata с описанием, что и куда нужно "вживить"
 * при трансформации байткода target-класса.
 */
public final class MixinScanner {

    /**
     * @param mixinClass  класс мода, помеченный &#64;Mixin
     * @return MixinMetadata, или null если класс не помечен &#64;Mixin
     */
    public MixinMetadata scan(Class<?> mixinClass) {

        Mixin mixinAnnotation = mixinClass.getAnnotation(Mixin.class);

        if (mixinAnnotation == null) {
            return null;
        }

        String targetClassInternalName = mixinAnnotation.value().replace('.', '/');
        String mixinClassInternalName = mixinClass.getName().replace('.', '/');

        MixinMetadata metadata = new MixinMetadata(targetClassInternalName, mixinClassInternalName);

        for (Method method : mixinClass.getDeclaredMethods()) {
            scanInject(method, metadata);
        }

        return metadata;

    }

    private void scanInject(Method method, MixinMetadata metadata) {

        Inject injectAnnotation = method.getAnnotation(Inject.class);

        if (injectAnnotation == null) {
            return;
        }

        if (!Modifier.isStatic(method.getModifiers())) {
            throw new MixinException(
                    "@Inject method must be static: " + method.getDeclaringClass().getName()
                            + "." + method.getName()
                            + " (mixin methods are invoked without an instance of the mixin class)"
            );
        }

        At at = injectAnnotation.at();

        String modOwnerInternalName = method.getDeclaringClass().getName().replace('.', '/');
        String modMethodName = method.getName();
        String modMethodDescriptor = org.objectweb.asm.Type.getMethodDescriptor(method);

        // targetMethodDescriptor оставляем пустым на этом этапе — поиск
        // целевого метода по имени без точного дескриптора реализован
        // в MixinClassTransformer (известное ограничение первой версии:
        // если в target-классе несколько перегрузок с одинаковым именем,
        // используется первая найденная — аналогично текущему поведению
        // MojangRemapper для унаследованных методов).
        InjectPoint point = new InjectPoint(
                injectAnnotation.method(),
                "",
                at.value(),
                at.target(),
                at.ordinal(),
                at.shift(),
                modOwnerInternalName,
                modMethodName,
                modMethodDescriptor
        );

        metadata.addInjectPoint(point);

    }

}
