package dev.fragmentcode.installer.profile;

import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.List;

/**
 * Утилита для генерации .json-профиля Fragment Loader, совместимого
 * с внешними launcher'ами (Prism, TLauncher, официальный).
 *
 * Запускается ОДНОКРАТНО (вручную, не на каждом старте игры) после того
 * как новая версия fragment-loader опубликована как GitHub Release:
 *
 *   java -cp fragment-installer.jar dev.fragmentcode.installer.profile.GenerateProfileCommand \
 *        <output.json> <github-release-url> <version>
 *
 * Пример:
 *   java -cp ... GenerateProfileCommand \
 *        "Fragment Loader 1.21.7.json" \
 *        https://github.com/VoiDDarkYT/FragmentLoader/releases/download/0.0.1/fragment-loader-0.0.1.jar \
 *        0.0.1
 *
 * Результат - готовый .json файл, который пользователь кладёт в
 * .minecraft/versions/Fragment Loader 1.21.7/Fragment Loader 1.21.7.json
 * (имя папки и файла должны совпадать с полем "id" внутри файла).
 */
public final class GenerateProfileCommand {

    private static final String VANILLA_VERSION = "1.21.7";

    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            System.err.println("Usage: GenerateProfileCommand <output.json> <github-release-url> <version>");
            System.exit(1);
            return;
        }

        Path outputPath = Path.of(args[0]);
        String releaseUrl = args[1];
        String version = args[2];

        System.out.println("Fetching vanilla " + VANILLA_VERSION + " version.json...");

        PrismProfileGenerator generator = new PrismProfileGenerator();

        System.out.println("Computing sha1/size for " + releaseUrl + " (downloading once)...");
        List<JsonObject> ownLibraries = OwnLibraryEntry.buildAll(releaseUrl, version);

        JsonObject profile = generator.generate(VANILLA_VERSION, ownLibraries);

        generator.saveToFile(profile, outputPath);

        System.out.println("Profile written to " + outputPath.toAbsolutePath());
        System.out.println("Copy it to: .minecraft/versions/Fragment Loader 1.21.7/Fragment Loader 1.21.7.json");

    }

}
