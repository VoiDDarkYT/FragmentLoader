package dev.fragmentcode.loader;

import dev.fragmentcode.loader.launcher.FragmentLauncher;
import dev.fragmentcode.loader.launcher.LaunchException;

public final class FragmentLoader {

    public void start() throws LaunchException {
        new FragmentLauncher().launch();
    }
}