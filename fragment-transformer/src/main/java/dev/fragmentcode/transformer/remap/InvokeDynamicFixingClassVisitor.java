package dev.fragmentcode.transformer.remap;

import dev.fragmentcode.transformer.mapping.ClassMapping;
import dev.fragmentcode.transformer.mapping.MappingEntry;
import dev.fragmentcode.transformer.mapping.MappingTable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Перехватывает visitInvokeDynamicInsn и исправляет имя метода-реализации
 * лямбды (захваченное внутри Handle bootstrap-аргумента), используя его
 * РЕАЛЬНЫЙ дескриптор (а не дескриптор интерфейса invokedynamic-сайта).
 *
 * Контекст проблемы: стандартный ASM Remapper.mapInvokeDynamicMethodName
 * получает имя/дескриптор САМОГО invokedynamic-сайта (то есть дескриптор
 * функционального интерфейса, который реализует лямбда - например
 * Supplier.get():()Ljava/lang/Object;), а НЕ дескриптор фактического
 * метода-реализации (например V705.lambda$new$0():()Lcom/mojang/datafixers/types/Type;).
 * Эти два дескриптора отличаются (особенно при захвате типизированных
 * значений и erasure обобщённых типов в функциональных интерфейсах),
 * из-за чего поиск по дескриптору invokedynamic-сайта в MappingTable не
 * находит нужную запись, и имя метода-реализации остаётся obfuscated -
 * что приводит к NoSuchMethodError при попытке связать лямбду в runtime
 * (см. реальный случай: V705.lambda$registerTypes$25 вызывает
 * V705.lambda$new$0(), но второй остаётся с именем "a").
 *
 * Решение: достать РЕАЛЬНЫЙ Handle лямбды из bootstrapMethodArguments
 * (для стандартного java.lang.invoke.LambdaMetafactory это второй
 * аргумент - сам implementation method handle), и искать в MappingTable
 * по owner+name+desc именно ЭТОГО handle, а не по name/desc самого
 * invokedynamic-сайта.
 */
final class InvokeDynamicFixingClassVisitor extends ClassVisitor {

    private final MappingTable mappingTable;

    InvokeDynamicFixingClassVisitor(int api, ClassVisitor classVisitor, MappingTable mappingTable) {
        super(api, classVisitor);
        this.mappingTable = mappingTable;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {

        MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);

        return new MethodVisitor(Opcodes.ASM9, delegate) {

            @Override
            public void visitInvokeDynamicInsn(String invokeDynName, String invokeDynDescriptor, Handle bootstrapMethod, Object... bootstrapMethodArguments) {

                Object[] fixedArguments = bootstrapMethodArguments.clone();

                for (int i = 0; i < fixedArguments.length; i++) {

                    if (fixedArguments[i] instanceof Handle implementationHandle) {
                        fixedArguments[i] = remapImplementationHandleIfNeeded(implementationHandle);
                    }

                }

                super.visitInvokeDynamicInsn(invokeDynName, invokeDynDescriptor, bootstrapMethod, fixedArguments);

            }

        };

    }

    /**
     * Переименовывает имя метода внутри Handle (если это метод, чей
     * owner+obfuscated-имя+дескриптор присутствуют в MappingTable),
     * оставляя сам Handle структурно совпадающим (тип хендла, owner,
     * дескриптор) - меняется только имя метода на читаемое.
     *
     * Сам owner-класс Handle переименовывается отдельно стандартным
     * механизмом ClassRemapper/Remapper.map() при визите остальной части
     * байткода - здесь правится только имя МЕТОДА, которое Remapper API
     * пропускает для bootstrap-аргументов lambda metafactory.
     */
    private Handle remapImplementationHandleIfNeeded(Handle handle) {

        ClassMapping owningClass = mappingTable.findByObfuscatedName(handle.getOwner());

        if (owningClass == null) {
            return handle;
        }

        MappingEntry entry = owningClass.findMethodByObfuscated(handle.getName(), handle.getDesc());

        if (entry == null) {
            return handle;
        }

        return new Handle(
                handle.getTag(),
                handle.getOwner(),
                entry.getReadableName(),
                handle.getDesc(),
                handle.isInterface()
        );

    }

}
