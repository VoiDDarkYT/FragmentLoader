package dev.fragmentcode.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.nio.file.Path;

/**
 * Fragment Gradle Plugin.
 *
 * Добавляет в проект мода:
 *   1. DSL-блок fragment { minecraftVersion = "..." }
 *   2. Автоматически скачивает и ремаппирует Minecraft jar
 *   3. Добавляет его как compileOnly зависимость
 *   4. Добавляет fragment-api как compileOnly зависимость
 *
 * Кэш: ~/.gradle/caches/fragment/<version>/
 *   - minecraft-<version>.jar           — оригинальный obfuscated jar
 *   - client_mappings.txt               — Mojang ProGuard mappings
 *   - minecraft-<version>-mapped.jar    — результат ремаппинга (главный артефакт)
 *   - minecraft-<version>-mapped.sha1   — маркер актуальности кэша
 */
public class FragmentGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {

        FragmentExtension extension = project.getExtensions()
                .create("fragment", FragmentExtension.class);

        // После того как build.gradle.kts полностью выполнился (все блоки
        // fragment { } прочитаны) — настраиваем зависимости.
        project.afterEvaluate(p -> {

            String minecraftVersion = extension.getMinecraftVersion();

            if (minecraftVersion == null || minecraftVersion.isBlank()) {
                throw new IllegalStateException(
                        "[Fragment] minecraftVersion is not set. " +
                        "Add: fragment { minecraftVersion = \"1.21.7\" }"
                );
            }

            setupDependencies(project, minecraftVersion);

        });

    }

    private void setupDependencies(Project project, String minecraftVersion) {

        MinecraftProvider provider = new MinecraftProvider();

        try {

            // Скачиваем и ремаппируем Minecraft
            Path mappedJar = provider.provideRemappedJar(minecraftVersion);

            // Скачиваем fragment-api
            Path apiJar = provider.provideFragmentApi();

            // Добавляем оба как compileOnly
            project.getDependencies().add("compileOnly",
                    project.files(mappedJar.toFile()));

            project.getDependencies().add("compileOnly",
                    project.files(apiJar.toFile()));

            project.getLogger().lifecycle(
                    "[Fragment] Minecraft {} mapped jar added to compileOnly classpath", minecraftVersion
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "[Fragment] Failed to provide Minecraft " + minecraftVersion + ": " + e.getMessage(), e
            );
        }

    }

}
