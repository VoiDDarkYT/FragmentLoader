package dev.fragmentcode.installer.environment;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Определяет стандартные пути окружения, в первую очередь - путь
 * к .minecraft-совместимой папке на текущей ОС. Используется
 * fragment-installer (куда ставить файлы) и fragment-loader (откуда
 * их читать при запуске) - единая точка правды, чтобы оба модуля
 * всегда указывали на одну и ту же папку.
 */
public final class FragmentEnvironment {

    private final Path workingDirectory;
    private final Path minecraftDirectory;

    public FragmentEnvironment() {

        this.workingDirectory = Path.of("").toAbsolutePath();

        this.minecraftDirectory = detectMinecraftDirectory();

    }

    private Path detectMinecraftDirectory() {

        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");

        if (os.contains("win")) {
            return Path.of(home, "AppData", "Roaming", ".minecraft");
        }

        if (os.contains("mac")) {
            return Path.of(home, "Library", "Application Support", "minecraft");
        }

        return Path.of(home, ".minecraft");

    }

    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    public Path getMinecraftDirectory() {
        return minecraftDirectory;
    }

    public boolean minecraftExists() {
        return Files.exists(minecraftDirectory);
    }

}
