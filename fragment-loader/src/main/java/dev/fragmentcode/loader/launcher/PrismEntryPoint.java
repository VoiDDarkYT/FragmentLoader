package dev.fragmentcode.loader.launcher;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Точка входа, используемая когда игра запускается через ВНЕШНИЙ launcher
 * (Prism, TLauncher, официальный) на основе .json-профиля версии,
 * сгенерированного нашим PrismProfileGenerator (см. fragment-installer).
 *
 * Контракт совершенно другой, чем у GameProcessEntryPoint:
 *   - Внешний launcher сам читает .json-профиль, сам скачивает vanilla
 *     client.jar + все libraries (включая наши, прописанные в профиле),
 *     и запускает JVM как: java <jvm-args> -cp <готовый classpath>
 *     dev.fragmentcode.loader.launcher.PrismEntryPoint <game-args...>
 *   - args здесь - это УЖЕ чистые game-аргументы (--username, --uuid и т.д.),
 *     без нашего служебного mainClass/classpath в начале (в отличие от
 *     GameProcessEntryPoint, где их добавляет наш собственный
 *     ProcessBuilder).
 *   - classpath JVM уже настроен самим launcher'ом через "-cp" (Prism
 *     передаёт его как обычный classpath, не через "-jar") - поэтому
 *     System.getProperty("java.class.path") здесь надёжен, в отличие от
 *     сценария "java -jar fat.jar" в FragmentLauncher.
 *   - java.library.path для natives также уже выставлен launcher'ом
 *     (это стандартный ${natives_directory} placeholder в arguments.jvm,
 *     который умеет разворачивать любой launcher, читающий version.json).
 *
 * mainClass игры захардкожен как vanilla net.minecraft.client.main.Main,
 * так как в Prism-сценарии нет смысла передавать его как аргумент -
 * наш собственный .json-профиль всегда указывает именно на этот класс
 * как на финальную цель.
 */
public final class PrismEntryPoint {

    private static final String MINECRAFT_MAIN_CLASS = "net.minecraft.client.main.Main";

    public static void main(String[] args) throws Exception {

        List<Path> classpath = parseClasspath(System.getProperty("java.class.path"));

        GameLauncherCore.launchMainClass(MINECRAFT_MAIN_CLASS, classpath, args);

    }

    private static List<Path> parseClasspath(String classpathString) {

        List<Path> result = new ArrayList<>();

        if (classpathString == null) {
            return result;
        }

        for (String entry : classpathString.split(java.io.File.pathSeparator)) {

            if (!entry.isBlank()) {
                result.add(Path.of(entry));
            }

        }

        return result;

    }

}
