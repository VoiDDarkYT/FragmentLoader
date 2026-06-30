package dev.fragmentcode.loader.launcher;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Точка входа для НОВОГО процесса игры (запускается через ProcessBuilder
 * из FragmentLauncher, а не в той же JVM, что bootstrap).
 *
 * Так делает любой нормальный launcher (официальный, Prism, TLauncher) -
 * сама игра всегда работает в отдельном процессе со своими JVM-флагами
 * (-Djava.library.path, -Xmx и т.д.), заданными ПРИ СТАРТЕ этой JVM,
 * а не через System.setProperty постфактум - именно постфактум-подход
 * не сработал с GLFW/LWJGL, так как java.library.path кэшируется JVM
 * при первом обращении к native loader'у и не обновляется в runtime.
 *
 * Аргументы командной строки этого main():
 *   args[0]            - mainClass игры (например net.minecraft.client.main.Main)
 *   args[1]            - classpath игры (client.jar + все libraries),
 *                        склеенный через File.pathSeparator
 *   args[2..]          - аргументы для самой игры (--username, --uuid и т.д.)
 *
 * Используется только нашим собственным fragment-bootstrap.jar - когда
 * запуск идёт через внешний launcher (Prism и т.д.), используется
 * PrismEntryPoint, у которого другой контракт аргументов (см. его javadoc).
 *
 * java.library.path для natives задаётся не здесь, а через -D флаг при
 * запуске этой же JVM (см. FragmentLauncher.buildProcessCommand) - то есть
 * к моменту, когда выполняется этот main(), java.library.path уже
 * правильный с самого старта JVM.
 */
public final class GameProcessEntryPoint {

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            throw new IllegalArgumentException(
                    "Usage: GameProcessEntryPoint <mainClass> <classpath> [gameArgs...]"
            );
        }

        String mainClassName = args[0];
        String classpathString = args[1];

        String[] gameArgs = new String[args.length - 2];
        System.arraycopy(args, 2, gameArgs, 0, gameArgs.length);

        List<Path> classpath = parseClasspath(classpathString);

        GameLauncherCore.launchMainClass(mainClassName, classpath, gameArgs);

    }

    private static List<Path> parseClasspath(String classpathString) {

        List<Path> result = new ArrayList<>();

        for (String entry : classpathString.split(java.io.File.pathSeparator)) {

            if (!entry.isBlank()) {
                result.add(Path.of(entry));
            }

        }

        return result;

    }

}
