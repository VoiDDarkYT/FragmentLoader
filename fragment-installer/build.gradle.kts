plugins {
    java
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
}

// fat jar для удобного запуска утилит из этого модуля напрямую, например
// dev.fragmentcode.installer.profile.GenerateProfileCommand - без него
// пришлось бы собирать classpath из gson вручную при каждом запуске.
tasks.register<Jar>("fatJar") {

    archiveBaseName.set("fragment-installer")

    manifest {
        attributes["Main-Class"] = "dev.fragmentcode.installer.profile.GenerateProfileCommand"
    }

    from({
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        }
    })

    with(tasks.jar.get())

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

}
