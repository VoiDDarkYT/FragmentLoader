package dev.fragmentcode.api.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Перехватывает значение локальной переменной в указанной точке метода
 * (см. &#64;At), передаёт его в метод-мод, и записывает ВОЗВРАЩЁННОЕ
 * значение обратно в ту же переменную — то есть можно не просто прочитать,
 * а реально подменить значение, влияя на дальнейшую логику оригинального
 * метода.
 *
 * Так как реальный client.jar Mojang не содержит debug-информацию с
 * именами локальных переменных (LocalVariableTable отсутствует в
 * production-сборках), переменная указывается не по имени, а по:
 *   - типу параметра самого метода-мода (определяет, переменную какого
 *     типа искать — например float, int, java.lang.String)
 *   - ordinal — какая по счёту переменная этого типа в данной точке
 *     (считая с 0, по умолчанию первая)
 *
 * Метод, помеченный &#64;ModifyVariable, должен:
 *   - принимать РОВНО ОДИН параметр — текущее значение переменной
 *   - возвращать значение ТОГО ЖЕ типа — новое значение переменной
 *
 * Пример — обнулить отрицательный урон после вызова calculateDamage:
 * <pre>
 * &#64;Mixin("net.minecraft.entity.LivingEntity")
 * public class DamageMixin {
 *
 *     &#64;ModifyVariable(
 *         method = "hurt",
 *         at = &#64;At(value = AtType.INVOKE, target = "calculateDamage", shift = Shift.AFTER)
 *     )
 *     private float fixDamage(float damage) {
 *         return damage &lt; 0 ? 0 : damage;
 *     }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ModifyVariable {

    /**
     * Имя метода в целевом классе, внутри которого перехватываем переменную.
     */
    String method();

    /**
     * Точка внутри метода, где нужно перехватить значение переменной.
     */
    At at();

    /**
     * Какая по счёту переменная нужного типа (определяемого по типу
     * параметра метода-мода) в данной точке. 0 = первая (по умолчанию).
     */
    int ordinal() default 0;

}
