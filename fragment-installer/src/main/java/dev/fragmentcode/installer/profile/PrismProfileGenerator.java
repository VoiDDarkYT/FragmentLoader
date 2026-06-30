package dev.fragmentcode.installer.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.fragmentcode.installer.download.DownloadException;
import dev.fragmentcode.installer.manifest.ManifestFetcher;
import dev.fragmentcode.installer.manifest.VersionManifest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Генерирует .json-профиль версии для ВНЕШНИХ launcher'ов (Prism, TLauncher,
 * официальный Minecraft Launcher), совместимый с их стандартным форматом
 * version.json - аналогично тому, что делает Fabric Installer.
 *
 * Подход: берём реальный vanilla version.json как JsonObject (не через
 * наши строго типизированные модели VersionMetadata - так безопаснее,
 * потому что сохраняются абсолютно все поля, включая те, что мы могли
 * не предусмотреть, например "logging" или "complianceLevel"), и вносим
 * минимальные изменения:
 *   1. id -> "Fragment Loader <версия>" (чтобы отличался в списке версий)
 *   2. mainClass -> dev.fragmentcode.loader.launcher.PrismEntryPoint
 *      (вместо net.minecraft.client.main.Main - наш entry point сам
 *      создаст FragmentClassLoader и вызовет настоящий vanilla Main)
 *   3. В "libraries" ДОБАВЛЯЕМ (не заменяем) записи для наших jar'ов
 *      (fragment-api, fragment-installer, fragment-loader, gson) -
 *      внешний launcher должен будет их тоже скачать/найти и включить
 *      в classpath, как и любую обычную vanilla-библиотеку.
 *
 * Результат сохраняется как .json файл, который пользователь кладёт
 * в свою .minecraft/versions/<id>/<id>.json (или мы делаем это сами,
 * если знаем путь к .minecraft внешнего launcher'а).
 */
public final class PrismProfileGenerator {

    private static final String PROFILE_ID = "Fragment Loader 1.21.7";
    private static final String ENTRY_POINT_CLASS = "dev.fragmentcode.loader.launcher.PrismEntryPoint";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ManifestFetcher manifestFetcher = new ManifestFetcher();

    /**
     * @param ownLibraries  список наших собственных library-записей,
     *                      которые нужно добавить в профиль (см.
     *                      OwnLibraryEntry.buildAll() для готового набора).
     */
    public JsonObject generate(String vanillaVersionId, List<JsonObject> ownLibraries)
            throws DownloadException, ProfileGenerationException {

        VersionManifest manifest = manifestFetcher.fetchVersionManifest();
        VersionManifest.VersionEntry entry = manifest.findVersion(vanillaVersionId);

        if (entry == null) {
            throw new ProfileGenerationException("Version not found in manifest: " + vanillaVersionId);
        }

        String vanillaJson = manifestFetcher.fetchRawText(entry.getUrl());
        JsonObject profile = gson.fromJson(vanillaJson, JsonObject.class);

        profile.addProperty("id", PROFILE_ID);
        profile.addProperty("mainClass", ENTRY_POINT_CLASS);
        // "modified" - тот же тип, что использует сам Fabric для своих
        // сгенерированных профилей, отличает их от обычных "release".
        profile.addProperty("type", "modified");

        JsonArray libraries = profile.getAsJsonArray("libraries");

        for (JsonObject ownLibrary : ownLibraries) {
            libraries.add(ownLibrary);
        }

        return profile;

    }

    public void saveToFile(JsonObject profile, Path destination) throws ProfileGenerationException {

        try {

            Path parent = destination.toAbsolutePath().getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(destination, gson.toJson(profile));

        } catch (IOException e) {
            throw new ProfileGenerationException("Failed to write profile to " + destination, e);
        }

    }

}
