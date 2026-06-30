plugins {
    java
}

dependencies {
    implementation(project(":fragment-loader"))
}

tasks.jar {

    manifest {
        attributes["Main-Class"] = "dev.fragmentcode.bootstrap.FragmentBootstrap"
    }

    // Fat jar: включаем содержимое всех runtime-зависимостей (fragment-api,
    // fragment-installer, fragment-transformer, fragment-loader, gson, ASM)
    // прямо внутрь итогового fragment-bootstrap.jar, чтобы его можно было
    // запускать одной командой `java -jar fragment-bootstrap.jar` без
    // отдельной настройки classpath.
    from({
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        }
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

}
