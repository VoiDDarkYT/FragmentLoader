package dev.fragmentcode.api.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Вставляет вызов метода-мода в указанную точку оригинального метода
 * (см. &#64;At — начало, конец, или рядом с конкретным вызовом другого
 * метода). Оригинальная логика метода НЕ удаляется и продолжает
 * выполняться — мы просто добавляем свой код рядом.
 *
 * Метод, помеченный &#64;Inject, должен:
 *   - быть void
 *   - принимать те же параметры, что и оригинальный метод (можно без них,
 *     если параметры не нужны)
 *
 * Примеры:
 * <pre>
 * &#64;Mixin("net.minecraft.client.MinecraftClient")
 * public class ExampleMixin {
 *
 *     // В начало метода
 *     &#64;Inject(method = "setWindowTitle", at = &#64;At(AtType.HEAD))
 *     private void onSetTitle(String title) {
 *         System.out.println("Игра пытается установить заголовок: " + title);
 *     }
 *
 *     // После конкретного вызова внутри метода
 *     &#64;Inject(method = "render", at = &#64;At(value = AtType.INVOKE, target = "drawRect", shift = Shift.AFTER))
 *     private void afterDrawRect() {
 *         System.out.println("drawRect только что выполнился");
 *     }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Inject {

    /**
     * Имя метода в целевом классе, в который вставляем код.
     * Без параметров — если в целевом классе несколько перегрузок
     * с этим именем, понадобится уточнение через descriptor() (добавим позже).
     */
    String method();

    /**
     * Точка внутри метода, куда вставить код.
     */
    At at();

}
