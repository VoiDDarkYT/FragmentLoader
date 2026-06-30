package dev.fragmentcode.loader.mods;

import dev.fragmentcode.loader.classloader.FragmentClassLoader;
import dev.fragmentcode.transformer.mixin.AsmMixinScanner;
import dev.fragmentcode.transformer.mixin.MixinMetadata;
import dev.fragmentcode.transformer.mixin.MixinRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Находит .jar файлы в папке mods/ (рядом с .minecraft), регистрирует
 * найденные &#64;Mixin-классы в MixinRegistry, и добавляет jar'ы в
 * classpath FragmentClassLoader.
 *
 * Порядок действий важен — решает проблему "курицы и яйца":
 *   1. Сканируем байткод КАЖДОГО .class файла внутри каждого mod jar'а
 *      через AsmMixinScanner (без загрузки класса через JVM!) —
 *      это позволяет узнать, какие методы нужно патчить на public,
 *      ДО того как класс будет реально загружен.
 *   2. Регистрируем найденные MixinMetadata в MixinRegistry.
 *   3. Добавляем jar'ы в classpath FragmentClassLoader (addModJar) —
 *      классы из них будут реально загружены позже, естественным
 *      образом, когда на них кто-то сослался (обычно сам Minecraft,
 *      загружая mainClass, который через mixin'ы окажется связан
 *      с классами мода).
 *
 * Если бы мы вместо этого сначала ЗАГРУЖАЛИ классы через classLoader,
 * а потом сканировали их через рефлексию — было бы поздно патчить
 * видимость private-методов, так как байткод класса уже определён
 * в JVM на момент рефлексивного сканирования.
 */
public final class ModDiscovery {

    private ModDiscovery() {
    }

    public static void discoverAndRegister(FragmentClassLoader classLoader, MixinRegistry registry) throws IOException {

        Path modsDirectory = resolveModsDirectory();

        // Диагностический лог - абсолютный путь критичен для понимания,
        // совпадает ли реальная рабочая директория процесса с тем, что
        // ожидает пользователь (особенно важно при запуске через внешний
        // launcher типа Prism/TLauncher, где рабочая директория задаётся
        // самим launcher'ом, а не нашим кодом).
        System.out.println("[ModDiscovery] Looking for mods in: " + modsDirectory.toAbsolutePath());

        if (!Files.isDirectory(modsDirectory)) {
            // Создаём папку mods/ при первом запуске, даже если в ней
            // пока нет ни одного мода - чтобы пользователю было сразу
            // очевидно, куда класть .jar файлы модов, без необходимости
            // создавать папку руками или читать документацию.
            Files.createDirectories(modsDirectory);
            System.out.println("[ModDiscovery] mods/ directory did not exist, created it at: " + modsDirectory.toAbsolutePath());
            return;
        }

        List<Path> modJars = listJarFiles(modsDirectory);

        System.out.println("[ModDiscovery] Found " + modJars.size() + " mod jar(s): " + modJars);

        AsmMixinScanner scanner = new AsmMixinScanner();

        for (Path modJar : modJars) {

            scanJarForMixins(modJar, scanner, registry);
            classLoader.addModJar(modJar);

        }

    }

    private static Path resolveModsDirectory() {

        // Рабочая директория процесса игры — см. FragmentLauncher, который
        // устанавливает ProcessBuilder.directory() на installation.getGameDirectory(),
        // т.е. на корень .minecraft.
        return Path.of("mods");

    }

    private static List<Path> listJarFiles(Path modsDirectory) throws IOException {

        try (Stream<Path> stream = Files.list(modsDirectory)) {

            return stream
                    .filter(path -> path.toString().endsWith(".jar"))
                    .toList();

        }

    }

    private static void scanJarForMixins(Path modJar, AsmMixinScanner scanner, MixinRegistry registry) throws IOException {

        try (JarFile jarFile = new JarFile(modJar.toFile())) {

            var entries = jarFile.entries();

            while (entries.hasMoreElements()) {

                JarEntry entry = entries.nextElement();

                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }

                byte[] classBytecode;

                try (InputStream in = jarFile.getInputStream(entry)) {
                    classBytecode = in.readAllBytes();
                }

                MixinMetadata metadata = scanner.scan(classBytecode);

                if (metadata != null) {
                    registry.register(metadata);
                }

            }

        }

    }

}
