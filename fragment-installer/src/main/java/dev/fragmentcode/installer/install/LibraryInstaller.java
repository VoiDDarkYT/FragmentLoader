package dev.fragmentcode.installer.install;

import dev.fragmentcode.installer.download.DownloadException;
import dev.fragmentcode.installer.download.DownloadListener;
import dev.fragmentcode.installer.download.FileDownloader;
import dev.fragmentcode.installer.layout.GameDirectoryLayout;
import dev.fragmentcode.installer.rules.RuleEvaluator;
import dev.fragmentcode.installer.version.Library;
import dev.fragmentcode.installer.version.VersionMetadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Обрабатывает блок "libraries" из version.json:
 *   1. Фильтрует библиотеки, не подходящие для текущей ОС (через RuleEvaluator).
 *   2. Скачивает .jar файл каждой подходящей библиотеки в libraries/.
 *   3. Если библиотека - natives (classifier "natives-*"), распаковывает её
 *      содержимое в папку natives/ версии (там лежат .so/.dll/.dylib файлы,
 *      которые JVM подключает через -Djava.library.path).
 *
 * Обычные (не-natives) библиотеки идут в classpath как jar-файлы.
 * Natives НЕ идут в classpath - их .class файлов там нет, только бинарники.
 */
public final class LibraryInstaller {

    private final GameDirectoryLayout layout;
    private final RuleEvaluator ruleEvaluator;
    private final FileDownloader downloader;

    public LibraryInstaller(GameDirectoryLayout layout, DownloadListener listener) {
        this.layout = layout;
        this.ruleEvaluator = new RuleEvaluator();
        this.downloader = new FileDownloader(listener);
    }

    /**
     * Скачивает все применимые библиотеки, распаковывает natives.
     *
     * @return список путей к .jar файлам обычных (не-natives) библиотек -
     *         именно они идут в classpath при запуске.
     */
    public List<Path> install(VersionMetadata metadata, String versionId) throws DownloadException {

        List<Path> classpathJars = new ArrayList<>();
        Path nativesDir = layout.getNativesDirectory(versionId);

        for (Library library : metadata.getLibraries()) {

            if (!ruleEvaluator.isAllowed(library.getRules())) {
                continue;
            }

            if (library.isNatives() && !matchesCurrentArchitecture(library)) {
                continue;
            }

            if (library.getDownloads() == null || library.getDownloads().getArtifact() == null) {
                continue;
            }

            var artifact = library.getDownloads().getArtifact();
            Path destination = layout.getLibraryFile(artifact.getPath());

            downloader.download(
                    artifact.getUrl(),
                    destination,
                    artifact.getSha1(),
                    library.getName()
            );

            if (library.isNatives()) {
                extractNatives(destination, nativesDir);
            } else {
                classpathJars.add(destination);
            }

        }

        return classpathJars;

    }

    /**
     * Дополнительная (помимо RuleEvaluator) проверка для natives-библиотек:
     * сравнивает суффикс classifier (например "windows-x86", "windows-arm64",
     * или просто "windows" для обычного 64-битного варианта) с реальной
     * архитектурой текущей машины.
     *
     * Нужна, потому что некоторые version.json не указывают явный "arch"
     * в rules для x86-варианта на Windows - там rules идентичны обычному
     * 64-битному варианту, и без этой доп. проверки RuleEvaluator пропускал
     * бы оба варианта как разрешённые одновременно.
     *
     * Суффикс без явного "-x86"/"-arm64" (просто "windows", "linux",
     * "macos") считается стандартным 64-битным вариантом и разрешён
     * для x86_64.
     */
    private boolean matchesCurrentArchitecture(Library library) {

        String suffix = library.getNativesClassifierSuffix();

        if (suffix == null) {
            return true;
        }

        boolean suffixIsX86 = suffix.endsWith("-x86") || suffix.equals("x86");
        boolean suffixIsArm64 = suffix.endsWith("-arm64") || suffix.equals("arm64");

        String currentArch = ruleEvaluator.getCurrentArch();

        if (suffixIsX86) {
            return currentArch.equals("x86");
        }

        if (suffixIsArm64) {
            return currentArch.equals("arm64");
        }

        // Суффикс без явного arch (например просто "windows" или "linux")
        // - это стандартный 64-битный вариант, подходит для x86_64.
        return currentArch.equals("x86_64");

    }

    /**
     * Распаковывает native-библиотеку (jar, содержащий .so/.dll/.dylib)
     * в папку natives. META-INF и прочие не-бинарные файлы пропускаются.
     */
    private void extractNatives(Path nativesJar, Path targetDir) throws DownloadException {

        try {

            Files.createDirectories(targetDir);

            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(nativesJar))) {

                ZipEntry entry;

                while ((entry = zip.getNextEntry()) != null) {

                    if (entry.isDirectory() || entry.getName().startsWith("META-INF")) {
                        continue;
                    }

                    Path outFile = targetDir.resolve(Path.of(entry.getName()).getFileName().toString());

                    Files.copy(zip, outFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                }

            }

        } catch (IOException e) {
            throw new DownloadException("Failed to extract natives from " + nativesJar, e);
        }

    }

}
