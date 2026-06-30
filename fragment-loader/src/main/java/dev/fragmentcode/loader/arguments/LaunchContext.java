package dev.fragmentcode.loader.arguments;

import dev.fragmentcode.loader.auth.GameProfile;

import java.nio.file.Path;
import java.util.List;

/**
 * Собирает в одном месте все значения, которые подставляются вместо
 * ${placeholder}-ов в arguments.game / arguments.jvm из version.json.
 *
 * Список placeholder-ов взят из реального version.json 1.21.7:
 *   ${auth_player_name}, ${version_name}, ${game_directory}, ${assets_root},
 *   ${assets_index_name}, ${auth_uuid}, ${auth_access_token}, ${clientid},
 *   ${auth_xuid}, ${user_type}, ${version_type}, ${natives_directory},
 *   ${classpath}
 *
 * clientId и auth_xuid не имеют смысла в offline-режиме - подставляем
 * пустую строку, игра не проверяет эти поля при offline-запуске.
 */
public final class LaunchContext {

    private final GameProfile profile;
    private final Path gameDirectory;
    private final Path assetsRoot;
    private final String assetsIndexName;
    private final Path nativesDirectory;
    private final List<Path> classpath;
    private final String versionName;
    private final String versionType;

    public LaunchContext(
            GameProfile profile,
            Path gameDirectory,
            Path assetsRoot,
            String assetsIndexName,
            Path nativesDirectory,
            List<Path> classpath,
            String versionName,
            String versionType
    ) {
        this.profile = profile;
        this.gameDirectory = gameDirectory;
        this.assetsRoot = assetsRoot;
        this.assetsIndexName = assetsIndexName;
        this.nativesDirectory = nativesDirectory;
        this.classpath = classpath;
        this.versionName = versionName;
        this.versionType = versionType;
    }

    public String resolve(String placeholder) {

        return switch (placeholder) {
            case "auth_player_name" -> profile.getUsername();
            case "version_name" -> versionName;
            case "game_directory" -> gameDirectory.toString();
            case "assets_root" -> assetsRoot.toString();
            case "assets_index_name" -> assetsIndexName;
            case "auth_uuid" -> profile.getUuid().toString().replace("-", "");
            case "auth_access_token" -> profile.getAccessToken();
            case "clientid" -> "";
            case "auth_xuid" -> "";
            case "user_type" -> "msa";
            case "version_type" -> versionType;
            case "natives_directory" -> nativesDirectory.toString();
            case "classpath" -> buildClasspathString();
            default -> "";
        };

    }

    private String buildClasspathString() {

        String separator = System.getProperty("os.name").toLowerCase().contains("win") ? ";" : ":";

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < classpath.size(); i++) {

            if (i > 0) {
                sb.append(separator);
            }

            sb.append(classpath.get(i));

        }

        return sb.toString();

    }

}
