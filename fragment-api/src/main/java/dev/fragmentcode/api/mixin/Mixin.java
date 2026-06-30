package dev.fragmentcode.api.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Помечает класс как "mixin" — набор изменений для другого класса игры.
 *
 * Пример:
 * <pre>
 * &#64;Mixin("net.minecraft.client.MinecraftClient")
 * public class ExampleMixin {
 *     ...
 * }
 * </pre>
 *
 * Сам класс ExampleMixin никогда не загружается как самостоятельный класс
 * в игре — transformer читает его методы и "вживляет" их в target.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mixin {

    /**
     * Полное имя класса-цели (того, который будем патчить).
     * Используем читаемое имя из Mojang mappings, например:
     * "net.minecraft.client.MinecraftClient"
     */
    String value();

}
