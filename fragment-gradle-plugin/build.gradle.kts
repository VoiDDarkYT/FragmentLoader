plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "dev.fragmentcode"
version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-commons:9.6")
    implementation("com.google.code.gson:gson:2.11.0")
}

gradlePlugin {
    website = "https://github.com/VoiDDarkYT/FragmentLoader"
    vcsUrl = "https://github.com/VoiDDarkYT/FragmentLoader"

    plugins {
        create("fragmentPlugin") {
            id = "dev.fragmentcode.fragment-gradle"
            displayName = "Fragment Gradle Plugin"
            description = "Automatically downloads and remaps Minecraft for Fragment Loader mod development"
            tags = listOf("minecraft", "modding", "fragment")
            implementationClass = "dev.fragmentcode.gradle.FragmentGradlePlugin"
        }
    }
}

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Отключаем строгую проверку javadoc — комментарии на русском
// и теги вида <version> не являются валидным HTML
tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
}
