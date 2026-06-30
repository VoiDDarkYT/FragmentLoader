package dev.fragmentcode.installer.version;

import java.util.Map;

/**
 * Модель файла, на который ссылается version.json -> assetIndex -> url
 * (например https://piston-meta.mojang.com/v1/packages/.../26.json).
 *
 * Структура:
 * {
 *   "objects": {
 *     "icons/icon_16x16.png": { "hash": "...", "size": 3665 },
 *     ...тысячи записей...
 *   }
 * }
 *
 * Имя ключа (например "icons/icon_16x16.png") - это виртуальный путь файла
 * внутри .minecraft/assets/virtual/... (используется не во всех версиях),
 * а реальный физический путь файла на диске строится из hash:
 *   .minecraft/assets/objects/<первые 2 символа hash>/<полный hash>
 */
public final class AssetIndex {

    private Map<String, AssetObject> objects;

    public Map<String, AssetObject> getObjects() {
        return objects;
    }

    public static final class AssetObject {

        private String hash;
        private long size;

        public String getHash() {
            return hash;
        }

        public long getSize() {
            return size;
        }

        /**
         * Папка-префикс для физического пути на диске — первые 2 символа hash.
         * Так делает сам Mojang launcher, чтобы не складывать сотни тысяч
         * файлов в одну папку.
         */
        public String getHashPrefix() {
            return hash.substring(0, 2);
        }

    }

}
