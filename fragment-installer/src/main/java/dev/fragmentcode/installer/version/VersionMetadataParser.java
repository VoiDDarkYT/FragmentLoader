package dev.fragmentcode.installer.version;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Centralized Gson instance for parsing version.json files.
 * Registers the custom ArgumentsDeserializer needed for the mixed
 * string/object arrays in "arguments.game" and "arguments.jvm".
 */
public final class VersionMetadataParser {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ArgumentList.class, new ArgumentsDeserializer())
            .create();

    private VersionMetadataParser() {
    }

    public static VersionMetadata parse(String json) {
        return GSON.fromJson(json, VersionMetadata.class);
    }

    public static Gson gson() {
        return GSON;
    }

}
