package dev.fragmentcode.transformer.mixin;

import dev.fragmentcode.api.mixin.AtType;
import dev.fragmentcode.api.mixin.Shift;

/**
 * Распарсенные данные одного метода, помеченного &#64;Inject в mixin-классе.
 *
 * targetMethodName/targetMethodDescriptor — метод в КЛАССЕ-ЦЕЛИ, в который
 * вставляется код (например "render" в "net/minecraft/client/MinecraftClient").
 *
 * modOwnerInternalName/modMethodName/modMethodDescriptor — метод в самом
 * mixin-классе мода, который нужно ВЫЗВАТЬ в точке инъекции (например
 * "onSetTitle" в "com/example/ExampleMixin").
 */
public final class InjectPoint {

    private final String targetMethodName;
    private final String targetMethodDescriptor;

    private final AtType atType;
    private final String atTarget;
    private final int atOrdinal;
    private final Shift shift;

    private final String modOwnerInternalName;
    private final String modMethodName;
    private final String modMethodDescriptor;

    public InjectPoint(
            String targetMethodName,
            String targetMethodDescriptor,
            AtType atType,
            String atTarget,
            int atOrdinal,
            Shift shift,
            String modOwnerInternalName,
            String modMethodName,
            String modMethodDescriptor
    ) {
        this.targetMethodName = targetMethodName;
        this.targetMethodDescriptor = targetMethodDescriptor;
        this.atType = atType;
        this.atTarget = atTarget;
        this.atOrdinal = atOrdinal;
        this.shift = shift;
        this.modOwnerInternalName = modOwnerInternalName;
        this.modMethodName = modMethodName;
        this.modMethodDescriptor = modMethodDescriptor;
    }

    public String getTargetMethodName() {
        return targetMethodName;
    }

    public String getTargetMethodDescriptor() {
        return targetMethodDescriptor;
    }

    public AtType getAtType() {
        return atType;
    }

    public String getAtTarget() {
        return atTarget;
    }

    public int getAtOrdinal() {
        return atOrdinal;
    }

    public Shift getShift() {
        return shift;
    }

    public String getModOwnerInternalName() {
        return modOwnerInternalName;
    }

    public String getModMethodName() {
        return modMethodName;
    }

    public String getModMethodDescriptor() {
        return modMethodDescriptor;
    }

}
