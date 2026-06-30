package dev.fragmentcode.transformer.mixin;

import dev.fragmentcode.api.mixin.AtType;
import dev.fragmentcode.api.mixin.Shift;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

/**
 * Читает &#64;Mixin/&#64;Inject аннотации напрямую из байткода класса
 * через ASM (ClassReader + аннотационные визиторы), БЕЗ загрузки класса
 * через JVM classloader.
 *
 * Нужен для решения проблемы "курицы и яйца" в ModDiscovery: чтобы
 * патчить видимость (private -&gt; public) &#64;Inject-методов мод-класса
 * в момент его реальной загрузки через FragmentClassLoader, нужно ЗНАТЬ
 * заранее (до загрузки), какие методы патчить — а узнать это через
 * обычную рефлексию (MixinScanner) можно только ПОСЛЕ загрузки класса,
 * когда патчить уже поздно. AsmMixinScanner разрывает этот цикл, читая
 * метаданные прямо из .class файла на диске.
 *
 * Результат идентичен по структуре MixinScanner (тот же MixinMetadata),
 * но получается без загрузки класса в JVM.
 */
public final class AsmMixinScanner {

    private static final String MIXIN_ANNOTATION_DESC = "Ldev/fragmentcode/api/mixin/Mixin;";
    private static final String INJECT_ANNOTATION_DESC = "Ldev/fragmentcode/api/mixin/Inject;";

    public MixinMetadata scan(byte[] classBytecode) {

        ClassReader reader = new ClassReader(classBytecode);

        MixinClassVisitor visitor = new MixinClassVisitor();
        reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        if (visitor.targetClassInternalName == null) {
            // Класс не помечен @Mixin - не относится к mixin-системе.
            return null;
        }

        MixinMetadata metadata = new MixinMetadata(visitor.targetClassInternalName, visitor.mixinClassInternalName);

        for (InjectPoint point : visitor.injectPoints) {
            metadata.addInjectPoint(point);
        }

        return metadata;

    }

    /**
     * Визитор класса: ищет &#64;Mixin на самом классе, и &#64;Inject на
     * каждом методе.
     */
    private static final class MixinClassVisitor extends ClassVisitor {

        String mixinClassInternalName;
        String targetClassInternalName;
        final List<InjectPoint> injectPoints = new ArrayList<>();

        MixinClassVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.mixinClassInternalName = name;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {

            if (!descriptor.equals(MIXIN_ANNOTATION_DESC)) {
                return null;
            }

            return new AnnotationVisitor(Opcodes.ASM9) {

                @Override
                public void visit(String name, Object value) {

                    if (name.equals("value") && value instanceof String readableClassName) {
                        targetClassInternalName = readableClassName.replace('.', '/');
                    }

                }

            };

        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName, String methodDescriptor, String signature, String[] exceptions) {

            return new MethodVisitor(Opcodes.ASM9) {

                @Override
                public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {

                    if (!annotationDescriptor.equals(INJECT_ANNOTATION_DESC)) {
                        return null;
                    }

                    return new InjectAnnotationVisitor(methodName, methodDescriptor);

                }

            };

        }

        /**
         * Визитор для разбора одной &#64;Inject аннотации, включая
         * вложенную &#64;At аннотацию.
         */
        private final class InjectAnnotationVisitor extends AnnotationVisitor {

            private final String modMethodName;
            private final String modMethodDescriptor;
            private String targetMethodName;

            InjectAnnotationVisitor(String modMethodName, String modMethodDescriptor) {
                super(Opcodes.ASM9);
                this.modMethodName = modMethodName;
                this.modMethodDescriptor = modMethodDescriptor;
            }

            @Override
            public void visit(String name, Object value) {

                if (name.equals("method") && value instanceof String methodNameValue) {
                    this.targetMethodName = methodNameValue;
                }

            }

            @Override
            public AnnotationVisitor visitAnnotation(String name, String descriptor) {

                if (!name.equals("at")) {
                    return null;
                }

                return new AtAnnotationVisitor();

            }

            /**
             * Визитор для вложенной &#64;At(value=..., target=..., ordinal=..., shift=...).
             */
            private final class AtAnnotationVisitor extends AnnotationVisitor {

                private AtType atType = AtType.HEAD;
                private String atTarget = "";
                private int atOrdinal = 0;
                private Shift shift = Shift.BEFORE;

                AtAnnotationVisitor() {
                    super(Opcodes.ASM9);
                }

                @Override
                public void visit(String name, Object value) {

                    if (name.equals("target") && value instanceof String s) {
                        atTarget = s;
                    } else if (name.equals("ordinal") && value instanceof Integer i) {
                        atOrdinal = i;
                    }

                }

                @Override
                public void visitEnum(String name, String descriptor, String enumValue) {

                    if (name.equals("value")) {
                        atType = AtType.valueOf(enumValue);
                    } else if (name.equals("shift")) {
                        shift = Shift.valueOf(enumValue);
                    }

                }

                @Override
                public void visitEnd() {

                    InjectPoint point = new InjectPoint(
                            targetMethodName,
                            "",
                            atType,
                            atTarget,
                            atOrdinal,
                            shift,
                            mixinClassInternalName,
                            modMethodName,
                            modMethodDescriptor
                    );

                    injectPoints.add(point);

                }

            }

        }

    }

}
