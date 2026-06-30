package dev.fragmentcode.installer.version;

/**
 * Блок "arguments" из version.json:
 * { "game": [...], "jvm": [...] }
 *
 * Оба поля разбираются через ArgumentsDeserializer, так как содержат
 * смешанные строки/объекты (см. Argument).
 */
public final class Arguments {

    private ArgumentList game;
    private ArgumentList jvm;

    public ArgumentList getGame() {
        return game;
    }

    public ArgumentList getJvm() {
        return jvm;
    }

}
