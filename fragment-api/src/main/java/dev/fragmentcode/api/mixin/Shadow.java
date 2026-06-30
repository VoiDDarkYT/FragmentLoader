package dev.fragmentcode.api.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Объявляет поле или метод как "теневой" — то есть он на самом деле
 * существует в целевом классе (даже если там private), а здесь мы просто
 * описываем его, чтобы transformer знал: при компиляции мода в байткод
 * обращения к этому полю/методу нужно перенаправить на поле/метод
 * оригинального класса, а не искать его в самом mixin-классе.
 *
 * Без &#64;Shadow методы &#64;Inject и &#64;Overwrite не смогут читать или менять
 * внутреннее состояние оригинального класса (например приватное поле
 * "window" в MinecraftClient).
 *
 * Пример:
 * <pre>
 * &#64;Mixin("net.minecraft.client.MinecraftClient")
 * public class ExampleMixin {
 *
 *     &#64;Shadow
 *     private Window window;
 *
 *     &#64;Overwrite(method = "setWindowTitle")
 *     private void setWindowTitle(String title) {
 *         this.window.setTitle("Fragment - " + title);
 *     }
 * }
 * </pre>
 *
 * Поле window здесь не хранит реальное значение в mixin-классе — это просто
 * "окно" в оригинальный объект. Значение не нужно присваивать, его нельзя
 * читать напрямую через обычную Java-семантику до того, как transformer
 * перепишет байткод.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Shadow {
}
