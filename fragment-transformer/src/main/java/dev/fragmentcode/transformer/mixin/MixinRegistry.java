package dev.fragmentcode.transformer.mixin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Реестр всех зарегистрированных mixin-классов, индексированный по
 * имени target-класса (JVM internal form) — позволяет быстро узнать,
 * есть ли для загружаемого класса какие-то mixin'ы, без перебора всех
 * зарегистрированных mixin'ов на каждой загрузке класса.
 *
 * Несколько разных mixin-классов МОГУТ указывать на один и тот же
 * target — например два разных мода патчат один и тот же класс
 * Minecraft с разными целями, поэтому значение — список, а не один
 * элемент.
 *
 * Также хранит набор (modClass, modMethod) пар, методы которых должны
 * стать public при загрузке САМОГО мод-класса — &#64;Inject методы
 * пишутся автором мода как private (привычно для большинства Java-кода),
 * но вызываются из target-класса через INVOKESTATIC, что требует
 * видимости как минимум package-private/public между разными jar'ами
 * (mod-класс и target-класс обычно в разных, несвязанных пакетах).
 */
public final class MixinRegistry {

    private final Map<String, List<MixinMetadata>> byTargetClass = new HashMap<>();
    private final Map<String, Set<String>> publicMethodsByModClass = new HashMap<>();

    public void register(MixinMetadata metadata) {

        byTargetClass
                .computeIfAbsent(metadata.getTargetClassInternalName(), k -> new ArrayList<>())
                .add(metadata);

        for (InjectPoint point : metadata.getInjectPoints()) {

            publicMethodsByModClass
                    .computeIfAbsent(point.getModOwnerInternalName(), k -> new HashSet<>())
                    .add(point.getModMethodName() + point.getModMethodDescriptor());

        }

    }

    /**
     * Возвращает список mixin'ов для данного target-класса, или пустой
     * список, если для этого класса ничего не зарегистрировано (самый
     * частый случай — подавляющее большинство классов игры не патчится
     * никаким модом).
     */
    public List<MixinMetadata> getMixinsFor(String targetClassInternalName) {
        return byTargetClass.getOrDefault(targetClassInternalName, List.of());
    }

    public boolean hasMixinsFor(String targetClassInternalName) {
        return byTargetClass.containsKey(targetClassInternalName);
    }

    /**
     * Возвращает набор "имя+дескриптор" методов, которые нужно сделать
     * public при загрузке данного мод-класса. Пустой набор, если этот
     * класс не является источником ни одной &#64;Inject точки.
     */
    public Set<String> getMethodsToPublicize(String modClassInternalName) {
        return publicMethodsByModClass.getOrDefault(modClassInternalName, Set.of());
    }

}
