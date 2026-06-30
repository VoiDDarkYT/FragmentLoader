package dev.fragmentcode.installer.manifest;

import java.util.List;

/**
 * Соответствует структуре https://piston-meta.mojang.com/mc/game/version_manifest_v2.json
 *
 * Пример содержимого:
 * {
 *   "latest": { "release": "1.21.7", "snapshot": "..." },
 *   "versions": [
 *     { "id": "1.21.7", "type": "release", "url": "https://...1.21.7.json",
 *       "time": "...", "releaseTime": "...", "sha1": "...", "complianceLevel": 1 }
 *   ]
 * }
 */
public final class VersionManifest {

    private Latest latest;
    private List<VersionEntry> versions;

    public Latest getLatest() {
        return latest;
    }

    public List<VersionEntry> getVersions() {
        return versions;
    }

    /**
     * Ищет версию по id (например "1.21.7"). Возвращает null, если версия
     * не найдена в манифесте.
     */
    public VersionEntry findVersion(String id) {

        if (versions == null) {
            return null;
        }

        for (VersionEntry entry : versions) {
            if (entry.getId().equals(id)) {
                return entry;
            }
        }

        return null;

    }

    public static final class Latest {

        private String release;
        private String snapshot;

        public String getRelease() {
            return release;
        }

        public String getSnapshot() {
            return snapshot;
        }

    }

    public static final class VersionEntry {

        private String id;
        private String type;
        private String url;
        private String sha1;

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public String getUrl() {
            return url;
        }

        public String getSha1() {
            return sha1;
        }

    }

}
