package dev.fragmentcode.api.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Полностью заменяет тело оригинального метода своим. В отличие от &#64;Inject,
 * оригинальная логика метода исчезает целиком — выполняется только код мода.
 *
 * Сигнатура метода (имя параметров не важно, но их количество, типы и
 * возвращаемый тип) должна совпадать с оригинальным методом — это и есть
 * тот метод, который мы заменяем.
 *
 * Пример — изменить заголовок окна на свой, без обходных путей:
 * <pre>
 * &#64;Mixin("net.minecraft.client.MinecraftClient")
 * public class ExampleMixin {
 *
 *     &#64;Overwrite(method = "setWindowTitle")
 *     private void setWindowTitle(String title) {
 *         // оригинальный код метода больше не выполняется,
 *         // выполняется только этот код
 *         this.window.setTitle("Fragment - " + title);
 *     }
 * }
 * </pre>
 *
 * Если внутри тебе нужно вызывать другие поля/методы оригинального класса,
 * которые не публичные — используем &#64;Shadow (добавим отдельно).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Overwrite {

    /**
     * Имя метода в целевом классе, тело которого заменяем.
     */
    String method();

}
