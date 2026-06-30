package dev.fragmentcode.installer.version;

/**
 * Блок "assetIndex" из version.json:
 * { "id": "26", "sha1": "...", "size": ..., "totalSize": ..., "url": "..." }
 *
 * "id" — версия индекса ресурсов (не путать с версией игры).
 * "url" ведёт на отдельный JSON со списком всех файлов-ресурсов (assets).
 */
public final class AssetIndexInfo {

    private String id;
    private String sha1;
    private long size;
    private long totalSize;
    private String url;

    public String getId() {
        return id;
    }

    public String getSha1() {
        return sha1;
    }

    public long getSize() {
        return size;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public String getUrl() {
        return url;
    }

}
