package dev.fragmentcode.transformer.mixin.apply;

import dev.fragmentcode.api.mixin.Shift;
import dev.fragmentcode.transformer.mixin.InjectPoint;
import dev.fragmentcode.transformer.mixin.MixinException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Вставляет вызов мод-метода в указанную точку (HEAD/TAIL/INVOKE) внутри
 * байткода целевого метода, используя ASM tree API (MethodNode), которое
 * представляет инструкции как изменяемый список узлов — удобно для
 * поиска нужного места и вставки новых инструкций, в отличие от
 * стримового MethodVisitor API.
 *
 * Метод-мод вызывается как СТАТИЧЕСКИЙ (INVOKESTATIC) — см. ограничение
 * в MixinScanner: mixin-классы не инстанцируются, их методы вызываются
 * напрямую как утилитарные функции.
 *
 * Если у мод-метода есть параметры, совпадающие по типу с параметрами
 * target-метода (для HEAD/TAIL) — они передаются как есть (загружаются
 * через ALOAD/ILOAD и т.д. с того же индекса слота, что у target-метода,
 * так как параметры остаются на тех же слотах на протяжении всего метода).
 */
public final class InjectApplier {

    public void apply(MethodNode targetMethod, InjectPoint point) {

        switch (point.getAtType()) {

            case HEAD -> applyAtHead(targetMethod, point);
            case TAIL -> applyAtTail(targetMethod, point);
            case INVOKE -> applyAtInvoke(targetMethod, point);

        }

    }

    private void applyAtHead(MethodNode targetMethod, InjectPoint point) {

        InsnList callInstructions = buildModMethodCall(targetMethod, point);

        targetMethod.instructions.insert(callInstructions);

    }

    private void applyAtTail(MethodNode targetMethod, InjectPoint point) {

        // TAIL = перед КАЖДЫМ return-подобным выходом из метода (RETURN,
        // IRETURN, ARETURN и т.д.) — метод может иметь несколько точек
        // выхода (например несколько return в разных ветках if/else),
        // и инъекция должна сработать перед каждой из них.
        for (AbstractInsnNode insn : targetMethod.instructions.toArray()) {

            if (isReturnInstruction(insn)) {

                InsnList callInstructions = buildModMethodCall(targetMethod, point);
                targetMethod.instructions.insertBefore(insn, callInstructions);

            }

        }

    }

    private void applyAtInvoke(MethodNode targetMethod, InjectPoint point) {

        MethodInsnNode anchor = findInvokeAnchor(targetMethod, point.getAtTarget(), point.getAtOrdinal());

        if (anchor == null) {
            throw new MixinException(
                    "Could not find INVOKE anchor '" + point.getAtTarget()
                            + "' (ordinal " + point.getAtOrdinal() + ") in method "
                            + targetMethod.name + targetMethod.desc
            );
        }

        InsnList callInstructions = buildModMethodCall(targetMethod, point);

        if (point.getShift() == Shift.BEFORE) {
            targetMethod.instructions.insertBefore(anchor, callInstructions);
        } else {
            targetMethod.instructions.insert(anchor, callInstructions);
        }

    }

    /**
     * Ищет N-й (по ordinal, считая с 0) вызов метода с именем target
     * внутри списка инструкций target-метода.
     */
    private MethodInsnNode findInvokeAnchor(MethodNode targetMethod, String targetMethodName, int ordinal) {

        int seen = 0;

        for (AbstractInsnNode insn : targetMethod.instructions.toArray()) {

            if (insn instanceof MethodInsnNode methodInsn && methodInsn.name.equals(targetMethodName)) {

                if (seen == ordinal) {
                    return methodInsn;
                }

                seen++;

            }

        }

        return null;

    }

    private boolean isReturnInstruction(AbstractInsnNode insn) {

        int opcode = insn.getOpcode();

        return opcode == Opcodes.RETURN
                || opcode == Opcodes.IRETURN
                || opcode == Opcodes.LRETURN
                || opcode == Opcodes.FRETURN
                || opcode == Opcodes.DRETURN
                || opcode == Opcodes.ARETURN;

    }

    /**
     * Строит список инструкций, который загружает параметры target-метода
     * (если они нужны мод-методу) и вызывает мод-метод как static.
     *
     * Возвращаемое значение мод-метода (если не void) отбрасывается через
     * POP/POP2 — &#64;Inject задуман как side-effect-only хук, который не
     * меняет результат target-метода (для подмены значений используется
     * отдельная аннотация &#64;ModifyVariable).
     */
    private InsnList buildModMethodCall(MethodNode targetMethod, InjectPoint point) {

        InsnList instructions = new InsnList();

        Type[] modParamTypes = Type.getArgumentTypes(point.getModMethodDescriptor());
        Type[] targetParamTypes = Type.getArgumentTypes(targetMethod.desc);

        boolean isTargetStatic = (targetMethod.access & Opcodes.ACC_STATIC) != 0;
        int firstParamSlot = isTargetStatic ? 0 : 1; // слот 0 — "this" для нестатических методов

        if (modParamTypes.length > targetParamTypes.length) {
            throw new MixinException(
                    "@Inject method " + point.getModOwnerInternalName() + "." + point.getModMethodName()
                            + " has more parameters than target method " + targetMethod.name
            );
        }

        int slot = firstParamSlot;

        for (int i = 0; i < modParamTypes.length; i++) {

            instructions.add(new VarInsnNode(targetParamTypes[i].getOpcode(Opcodes.ILOAD), slot));
            slot += targetParamTypes[i].getSize();

        }

        instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                point.getModOwnerInternalName(),
                point.getModMethodName(),
                point.getModMethodDescriptor(),
                false
        ));

        Type modReturnType = Type.getReturnType(point.getModMethodDescriptor());

        if (modReturnType.getSize() == 1) {
            instructions.add(new InsnNode(Opcodes.POP));
        } else if (modReturnType.getSize() == 2) {
            instructions.add(new InsnNode(Opcodes.POP2));
        }

        return instructions;

    }

}
