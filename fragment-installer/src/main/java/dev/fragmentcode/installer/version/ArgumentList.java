package dev.fragmentcode.installer.version;

import java.util.List;

/**
 * Обёртка над List<Argument>, нужна только как "именованный тип" для
 * регистрации в Gson (registerTypeAdapter требует конкретный Type,
 * а не "голый" List<Argument>, который Java на runtime не отличает
 * от List<String> из-за erasure).
 */
public final class ArgumentList {

    private final List<Argument> arguments;

    public ArgumentList(List<Argument> arguments) {
        this.arguments = arguments;
    }

    public List<Argument> getArguments() {
        return arguments;
    }

}
