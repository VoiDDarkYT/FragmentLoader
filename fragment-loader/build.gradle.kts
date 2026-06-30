plugins {
    java
}

dependencies {
    implementation(project(":fragment-api"))
    implementation(project(":fragment-installer"))
    implementation(project(":fragment-transformer"))
}

// fragment-transformer зависит на ASM через implementation (не api), что
// по умолчанию не делает ASM видимым на compile classpath fragment-loader.
// fragment-loader сейчас не импортирует ASM-классы напрямую (только
// классы fragment-transformer, которые сами оборачивают ASM), поэтому
// это не вызывает ошибок компиляции. Если в будущем понадобится прямой
// доступ к ASM из fragment-loader, замени implementation(":fragment-transformer")
// выше на api(":fragment-transformer") в build.gradle.kts fragment-transformer.

// Этот fat jar (включающий fragment-api, fragment-installer,
// fragment-transformer, fragment-loader, gson и ASM) публикуется на
// GitHub Releases и используется внешними launcher'ами (Prism, TLauncher
// и т.д.) через .json-профиль, сгенерированный
// dev.fragmentcode.installer.profile.GenerateProfileCommand. В отличие от
// fragment-bootstrap.jar, здесь НЕ указывается Main-Class в манифесте -
// mainClass для запуска задаётся самим .json-профилем (PrismEntryPoint),
// а не манифестом этого jar'а.
tasks.register<Jar>("fatJar") {

    archiveBaseName.set("fragment-loader")

    from({
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        }
    })

    with(tasks.jar.get())

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

}
