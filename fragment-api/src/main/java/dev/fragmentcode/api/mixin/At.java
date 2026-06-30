package dev.fragmentcode.api.mixin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Описывает точную точку внутри метода, куда вставляется код &#64;Inject
 * или откуда перехватывается значение в &#64;ModifyVariable.
 *
 * Примеры использования:
 * <pre>
 * &#64;At(value = AtType.HEAD)
 * &#64;At(value = AtType.TAIL)
 * &#64;At(value = AtType.INVOKE, target = "drawRect")                    // перед первым вызовом drawRect
 * &#64;At(value = AtType.INVOKE, target = "drawRect", ordinal = 1)       // перед вторым вызовом
 * &#64;At(value = AtType.INVOKE, target = "drawRect", shift = Shift.AFTER) // после вызова
 * </pre>
 *
 * target — простое (без owner/сигнатуры) читаемое имя метода-якоря,
 * например "drawRect". Если внутри целевого метода несколько вызовов
 * с этим именем (разные перегрузки или просто несколько вызовов того же
 * метода), ordinal выбирает, какой по счёту использовать (считая с 0,
 * по умолчанию — первый).
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface At {

    /**
     * Тип точки: HEAD, TAIL или INVOKE.
     */
    AtType value();

    /**
     * Простое имя метода-якоря. Обязателен только когда value = INVOKE,
     * игнорируется для HEAD/TAIL.
     */
    String target() default "";

    /**
     * Какой по счёту вызов target-метода использовать, если их несколько
     * внутри целевого метода. 0 = первый (по умолчанию).
     */
    int ordinal() default 0;

    /**
     * Вставлять код до или после найденной точки. По умолчанию BEFORE.
     * Имеет смысл только для value = INVOKE — для HEAD/TAIL это
     * избыточно (они уже однозначно "до всего" / "после всего").
     */
    Shift shift() default Shift.BEFORE;

}
