package dev.fragmentcode.installer.download;

public final class DownloadException extends Exception {

    public DownloadException(String message) {
        super(message);
    }

    public DownloadException(String message, Throwable cause) {
        super(message, cause);
    }

}
