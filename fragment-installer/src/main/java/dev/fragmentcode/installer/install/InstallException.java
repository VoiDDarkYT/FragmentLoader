package dev.fragmentcode.installer.install;

public final class InstallException extends Exception {

    public InstallException(String message) {
        super(message);
    }

    public InstallException(String message, Throwable cause) {
        super(message, cause);
    }

}
