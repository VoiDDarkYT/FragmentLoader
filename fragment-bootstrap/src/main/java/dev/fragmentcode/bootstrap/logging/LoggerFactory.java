package dev.fragmentcode.bootstrap.logging;

public final class LoggerFactory {

    private LoggerFactory() {
    }

    public static FragmentLogger getLogger(Class<?> clazz) {
        return new FragmentLogger(clazz.getSimpleName());
    }

    public static FragmentLogger getLogger(String name) {
        return new FragmentLogger(name);
    }

}