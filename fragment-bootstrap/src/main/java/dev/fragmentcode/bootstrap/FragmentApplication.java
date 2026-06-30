package dev.fragmentcode.bootstrap;

import dev.fragmentcode.bootstrap.logging.FragmentLogger;
import dev.fragmentcode.bootstrap.logging.LoggerFactory;
import dev.fragmentcode.loader.FragmentLoader;
import dev.fragmentcode.loader.launcher.LaunchException;

public final class FragmentApplication {

    private final FragmentLogger logger =
            LoggerFactory.getLogger("Bootstrap");

    public void start() {

        logger.info("Starting Fragment Loader");

        try {

            new FragmentLoader().start();

        } catch (LaunchException e) {

            logger.error(e.getMessage());

        }

    }

}