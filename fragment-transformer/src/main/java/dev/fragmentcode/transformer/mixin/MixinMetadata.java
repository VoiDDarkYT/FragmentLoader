package dev.fragmentcode.transformer.mixin;

import java.util.ArrayList;
import java.util.List;

/**
 * Все данные, извлечённые из одного класса, помеченного &#64;Mixin.
 *
 * targetClassInternalName — класс-цель в JVM internal form
 * (например "net/minecraft/client/MinecraftClient"), уже после ремаппинга,
 * так как mixin-классы пишутся с использованием читаемых имён.
 *
 * Сейчас содержит только &#64;Inject точки — &#64;Overwrite/&#64;Shadow/
 * &#64;ModifyVariable будут добавлены отдельными следующими шагами.
 */
public final class MixinMetadata {

    private final String targetClassInternalName;
    private final String mixinClassInternalName;
    private final List<InjectPoint> injectPoints = new ArrayList<>();

    public MixinMetadata(String targetClassInternalName, String mixinClassInternalName) {
        this.targetClassInternalName = targetClassInternalName;
        this.mixinClassInternalName = mixinClassInternalName;
    }

    public String getTargetClassInternalName() {
        return targetClassInternalName;
    }

    public String getMixinClassInternalName() {
        return mixinClassInternalName;
    }

    public void addInjectPoint(InjectPoint point) {
        injectPoints.add(point);
    }

    public List<InjectPoint> getInjectPoints() {
        return injectPoints;
    }

}
