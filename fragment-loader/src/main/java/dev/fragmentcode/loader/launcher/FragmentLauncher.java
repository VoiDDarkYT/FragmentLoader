package dev.fragmentcode.loader.launcher;

import dev.fragmentcode.installer.download.ConsoleDownloadListener;
import dev.fragmentcode.installer.download.DownloadException;
import dev.fragmentcode.installer.environment.FragmentEnvironment;
import dev.fragmentcode.installer.install.GameInstallation;
import dev.fragmentcode.installer.install.GameInstaller;
import dev.fragmentcode.installer.install.InstallException;
import dev.fragmentcode.loader.arguments.ArgumentResolver;
import dev.fragmentcode.loader.arguments.LaunchContext;
import dev.fragmentcode.loader.auth.GameProfile;
import dev.fragmentcode.loader.auth.OfflineAuthentication;
import dev.fragmentcode.loader.remap.RemappingService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Главный сценарий запуска игры:
 *   1. Установить (или переиспользовать уже установленную) версию через
 *      GameInstaller - получаем client.jar, libraries, natives, assets.
 *   2. Создать offline GameProfile (фейковый, но валидный по формату).
 *   3. Собрать LaunchContext со всеми значениями для ${placeholder}-ов.
 *   4. Развернуть arguments.jvm / arguments.game в финальные списки строк
 *      через ArgumentResolver.
 *   5. Запустить игру в ОТДЕЛЬНОМ процессе через ProcessBuilder - именно
 *      так делает любой нормальный launcher (официальный, Prism, TLauncher).
 *
 * Почему отдельный процесс, а не вызов main() через рефлексию в этой же
 * JVM (как было раньше): java.library.path (нужен LWJGL/GLFW для поиска
 * .dll/.so файлов) кэшируется JVM при первом обращении к native loader'у
 * и НЕ обновляется через System.setProperty в уже запущенной JVM. Передать
 * его можно только как -D флаг ПРИ СТАРТЕ новой JVM - отсюда необходимость
 * в новом процессе.
 */
public final class FragmentLauncher {

    private static final String VERSION_ID = "1.21.7";

    public void launch() throws LaunchException {

        try {

            Path gameRoot = new FragmentEnvironment().getMinecraftDirectory();

            GameInstaller installer = new GameInstaller(gameRoot, new ConsoleDownloadListener());

            System.out.println("Installing/verifying " + VERSION_ID + " ...");
            GameInstallation installation = installer.install(VERSION_ID);

            Path remappedClientJar = new RemappingService().ensureRemapped(installation);

            GameProfile profile = new OfflineAuthentication().createProfile("Player");

            LaunchContext context = new LaunchContext(
                    profile,
                    installation.getGameDirectory(),
                    installation.getAssetsDirectory(),
                    installation.getMetadata().getAssets(),
                    installation.getNativesDirectory(),
                    buildClasspathWithRemappedClient(installation, remappedClientJar),
                    installation.getMetadata().getId(),
                    installation.getMetadata().getType()
            );

            ArgumentResolver argumentResolver = new ArgumentResolver();

            List<String> gameArgs = argumentResolver.resolve(
                    installation.getMetadata().getArguments().getGame(),
                    context
            );

            launchInNewProcess(installation, remappedClientJar, gameArgs);

        } catch (DownloadException e) {
            throw new LaunchException("Failed to download game files: " + e.getMessage(), e);
        } catch (InstallException e) {
            throw new LaunchException("Failed to install game: " + e.getMessage(), e);
        }

    }

    private void launchInNewProcess(GameInstallation installation, Path remappedClientJar, List<String> gameArgs) throws LaunchException {

        List<String> command = buildProcessCommand(installation, remappedClientJar, gameArgs);

        ProcessBuilder processBuilder = new ProcessBuilder(command);

        // Перенаправляем вывод и ввод процесса игры в нашу консоль,
        // чтобы видеть логи игры (и крэш-стектрейсы) в том же терминале.
        processBuilder.inheritIO();
        processBuilder.directory(installation.getGameDirectory().toFile());

        try {

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new LaunchException("Game process exited with code " + exitCode);
            }

        } catch (IOException e) {
            throw new LaunchException("Failed to start game process: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LaunchException("Interrupted while waiting for game process", e);
        }

    }

    /**
     * Собирает команду запуска нового процесса:
     *   java -Djava.library.path=<natives> -cp <наш classpath> GameProcessEntryPoint <mainClass> <classpath игры> <game args...>
     *
     * Наш classpath (для самого процесса JVM) должен содержать fragment-loader,
     * fragment-api, fragment-installer и gson - всё, что нужно, чтобы
     * GameProcessEntryPoint и FragmentClassLoader смогли загрузиться.
     * Это НЕ то же самое, что classpath игры (client.jar + libraries) -
     * тот передаётся как обычный аргумент (args[1]) и используется только
     * внутри FragmentClassLoader, а не как classpath самой JVM.
     */
    private List<String> buildProcessCommand(GameInstallation installation, Path remappedClientJar, List<String> gameArgs) throws LaunchException {

        List<String> command = new ArrayList<>();

        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        command.add(javaBin);

        command.add("-Djava.library.path=" + installation.getNativesDirectory());
        command.add("-Dorg.lwjgl.librarypath=" + installation.getNativesDirectory());

        command.add("-cp");
        command.add(resolveOwnJarPath());

        command.add(GameProcessEntryPoint.class.getName());

        command.add(installation.getMetadata().getMainClass());
        command.add(buildClasspathString(buildClasspathWithRemappedClient(installation, remappedClientJar)));

        command.addAll(gameArgs);

        return command;

    }

    /**
     * Находит путь к jar-файлу (или директории с классами), из которого
     * сейчас выполняется код, через CodeSource - это надёжно работает
     * независимо от того, как был запущен процесс (java -jar fat.jar
     * или java -cp classes:lib1.jar:lib2.jar ...).
     *
     * System.getProperty("java.class.path") здесь не подходит: при запуске
     * через "java -jar fragment-bootstrap.jar" это свойство НЕ содержит
     * путь к самому jar-файлу (classpath в этом случае берётся из
     * Class-Path в манифесте/самого jar, а не из java.class.path) - именно
     * это было причиной "ClassNotFoundException: GameProcessEntryPoint"
     * в новом процессе.
     */
    private String resolveOwnJarPath() throws LaunchException {

        try {

            return Path.of(
                    FragmentLauncher.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).toString();

        } catch (java.net.URISyntaxException | NullPointerException e) {
            throw new LaunchException("Could not determine own jar location", e);
        }

    }

    /**
     * Строит classpath игры, заменяя оригинальный (obfuscated) client.jar
     * на remapped версию - все остальные элементы (libraries) остаются
     * без изменений, так как они не обфусцированы Mojang'ом и не нуждаются
     * в ремаппинге.
     */
    private List<Path> buildClasspathWithRemappedClient(GameInstallation installation, Path remappedClientJar) {

        List<Path> classpath = new ArrayList<>(installation.getLibraryJars());
        classpath.add(remappedClientJar);

        return classpath;

    }

    private String buildClasspathString(List<Path> classpath) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < classpath.size(); i++) {

            if (i > 0) {
                sb.append(java.io.File.pathSeparator);
            }

            sb.append(classpath.get(i));

        }

        return sb.toString();

    }

}
