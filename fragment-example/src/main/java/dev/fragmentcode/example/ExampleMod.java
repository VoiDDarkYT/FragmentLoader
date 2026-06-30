package dev.fragmentcode.example;

import dev.fragmentcode.api.mixin.At;
import dev.fragmentcode.api.mixin.AtType;
import dev.fragmentcode.api.mixin.Inject;
import dev.fragmentcode.api.mixin.Mixin;

/**
 * Первый тестовый мод для Fragment Loader.
 *
 * Цепляется за net.minecraft.client.main.Main.main(String[]) — это точка
 * входа самого Minecraft, единственный метод, чьё имя и сигнатуру можно
 * знать наверняка без доступа к Mojang mappings (это публичная точка
 * входа Java-приложения, она не меняется между версиями и не зависит
 * от внутренней структуры игры).
 *
 * Инъекция в HEAD — до того как Minecraft успеет сделать что-либо ещё —
 * наглядно доказывает, что:
 *   1. ModDiscovery нашёл этот jar в папке mods/
 *   2. AsmMixinScanner правильно распознал &#64;Mixin/&#64;Inject
 *   3. MixinClassTransformer успешно патчит байткод target-класса
 *   4. Видимость private static метода ниже была автоматически
 *      исправлена на public (иначе был бы IllegalAccessError)
 */
@Mixin("net.minecraft.client.main.Main")
public class ExampleMod {

    @Inject(method = "main", at = @At(AtType.HEAD))
    private static void onGameStart(String[] args) {

        System.out.println();
        System.out.println("=================================================");
        System.out.println("  FRAGMENT LOADER MOD SYSTEM WORKS!");
        System.out.println("  This message was injected via @Inject(HEAD)");
        System.out.println("  into net.minecraft.client.main.Main.main()");
        System.out.println("=================================================");
        System.out.println();

    }

}
