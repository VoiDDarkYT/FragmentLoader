package dev.fragmentcode.installer.version;

/**
 * Повторяющийся блок вида:
 * { "sha1": "...", "size": 12345, "url": "https://..." }
 *
 * Используется и для client.jar, и для каждой library, и для assetIndex и т.д.
 */
public final class DownloadInfo {

    private String sha1;
    private long size;
    private String url;
    private String path; // присутствует только у library artifacts

    public String getSha1() {
        return sha1;
    }

    public long getSize() {
        return size;
    }

    public String getUrl() {
        return url;
    }

    public String getPath() {
        return path;
    }

}
