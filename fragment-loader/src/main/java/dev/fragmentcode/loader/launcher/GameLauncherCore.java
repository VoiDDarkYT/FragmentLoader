package dev.fragmentcode.loader.launcher;

import dev.fragmentcode.loader.classloader.FragmentClassLoader;
import dev.fragmentcode.loader.mods.ModDiscovery;
import dev.fragmentcode.transformer.mixin.MixinClassTransformer;
import dev.fragmentcode.transformer.mixin.MixinRegistry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

/**
 * Общая логика запуска игры через FragmentClassLoader, используется обоими
 * сценариями запуска:
 *   - GameProcessEntryPoint: наш собственный fragment-bootstrap.jar
 *     запускает игру в новом процессе через ProcessBuilder
 *   - PrismEntryPoint: внешний launcher (Prism/TLauncher/официальный) читает
 *     .json-профиль и сам вызывает mainClass с уже готовым classpath
 *
 * Вынесено отдельно, чтобы не дублировать создание classloader'а и
 * reflection-вызов main() в обоих entry point'ах.
 *
 * С этого шага также подключает mixin-движок (fragment-transformer):
 *   1. Создаёт FragmentClassLoader (пока без transformer'а — "курица и яйцо",
 *      см. ниже).
 *   2. ModDiscovery сканирует байткод mod-jar'ов через ASM (БЕЗ загрузки
 *      классов через JVM), регистрируя &#64;Mixin/&#64;Inject данные
 *      в MixinRegistry — порядок важен для корректного патчинга видимости
 *      мод-методов (см. javadoc ModDiscovery).
 *   3. Создаёт MixinClassTransformer, передавая ему уже созданный
 *      classLoader (нужен для ASM COMPUTE_FRAMES).
 *   4. Присваивает transformer обратно в classLoader через setClassTransformer.
 */
public final class GameLauncherCore {

    private GameLauncherCore() {
    }

    public static void launchMainClass(String mainClassName, List<Path> classpath, String[] gameArgs) throws Exception {

        FragmentClassLoader classLoader = new FragmentClassLoader(
                classpath,
                GameLauncherCore.class.getClassLoader()
        );

        MixinRegistry mixinRegistry = new MixinRegistry();
        ModDiscovery.discoverAndRegister(classLoader, mixinRegistry);

        classLoader.setClassTransformer(new MixinClassTransformer(mixinRegistry, classLoader));

        // Многие библиотеки (LWJGL, Log4j, ServiceLoader-based код) ищут
        // ресурсы и реализации через "context classloader" текущего потока,
        // а не через classloader, который их непосредственно загрузил.
        Thread.currentThread().setContextClassLoader(classLoader);

        Class<?> mainClass = classLoader.loadClass(mainClassName);
        Method mainMethod = mainClass.getMethod("main", String[].class);

        try {
            mainMethod.invoke(null, new Object[]{gameArgs});
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Game crashed: " + e.getCause(), e.getCause());
        }

    }

}
