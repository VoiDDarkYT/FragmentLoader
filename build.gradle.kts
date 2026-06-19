plugins {
    java
}

allprojects {
    group = "com.fragmentmc"
    version = "0.0.1"
}

subprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}