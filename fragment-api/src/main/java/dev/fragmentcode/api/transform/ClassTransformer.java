package dev.fragmentcode.api.transform;

/**
 * Хук, который FragmentClassLoader вызывает перед тем, как определить
 * (define) класс в JVM. Позволяет изменить байткод класса "на лету" -
 * это основа всей mixin-системы (inject/overwrite из fragment-api.mixin).
 *
 * Реализация (fragment-transformer) читает байткод через ASM, ищет,
 * есть ли для этого класса зарегистрированные mixin-классы, и если есть -
 * применяет изменения и возвращает новый байт-массив. Если изменений нет -
 * возвращает bytecode без изменений.
 *
 * Это единственная точка связи между fragment-loader (который ничего
 * не знает про ASM/mixins) и fragment-transformer (который ничего
 * не знает про то, как загружаются классы) - они общаются только
 * через этот интерфейс, определённый в fragment-api, от которого
 * оба зависят.
 */
public interface ClassTransformer {

    /**
     * @param className   полное имя класса (с точками, например
     *                    "net.minecraft.client.MinecraftClient")
     * @param originalBytecode оригинальный байткод класса, как он лежит
     *                          в jar-файле
     * @return изменённый (или тот же самый) байткод. Никогда не null -
     *         если изменений нет, нужно вернуть originalBytecode как есть.
     */
    byte[] transform(String className, byte[] originalBytecode);

    /**
     * Реализация-заглушка, которая ничего не меняет. Используется,
     * пока fragment-transformer ещё не подключён к loader'у.
     */
    ClassTransformer NO_OP = (className, originalBytecode) -> originalBytecode;

}
