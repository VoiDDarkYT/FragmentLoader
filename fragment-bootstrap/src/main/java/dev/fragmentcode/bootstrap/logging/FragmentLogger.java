package dev.fragmentcode.bootstrap.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class FragmentLogger {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String name;

    FragmentLogger(String name) {
        this.name = name;
    }

    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    public void warn(String message) {
        log(LogLevel.WARN, message);
    }

    public void error(String message) {
        log(LogLevel.ERROR, message);
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    private void log(LogLevel level, String message) {

        String time = LocalDateTime.now().format(FORMAT);

        System.out.printf(
                "[%s] [%s/%s] %s%n",
                time,
                name,
                level,
                message
        );

    }

}