package dev.fragmentcode.bootstrap;

import dev.fragmentcode.bootstrap.logging.FragmentLogger;
import dev.fragmentcode.bootstrap.logging.LoggerFactory;

public final class FragmentApplication {

    private final FragmentLogger logger =
            LoggerFactory.getLogger("Bootstrap");

    public void start() {

        logger.info("Starting Fragment Loader");
        logger.info("Version: 0.0.1");
        logger.info("Java: " + Runtime.version());
        logger.info("Operating System: " + System.getProperty("os.name"));

    }

}