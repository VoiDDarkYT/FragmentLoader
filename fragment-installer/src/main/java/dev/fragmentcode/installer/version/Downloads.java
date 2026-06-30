package dev.fragmentcode.installer.version;

/**
 * Блок "downloads" из version.json:
 * {
 *   "client": { sha1, size, url },
 *   "client_mappings": { sha1, size, url },
 *   "server": {...},
 *   "server_mappings": {...}
 * }
 */
public final class Downloads {

    private DownloadInfo client;
    private DownloadInfo client_mappings;
    private DownloadInfo server;
    private DownloadInfo server_mappings;

    public DownloadInfo getClient() {
        return client;
    }

    public DownloadInfo getClientMappings() {
        return client_mappings;
    }

    public DownloadInfo getServer() {
        return server;
    }

    public DownloadInfo getServerMappings() {
        return server_mappings;
    }

}
