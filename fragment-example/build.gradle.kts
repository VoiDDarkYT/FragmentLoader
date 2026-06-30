plugins {
    java
}

dependencies {
    compileOnly(project(":fragment-api"))
}

// Готовый jar для папки mods/ - содержит только классы мода, без
// fragment-api (он только compileOnly, так как уже есть в classpath
// игры через fragment-loader - не нужно дублировать его внутри jar мода).
tasks.jar {
    archiveBaseName.set("fragment-example")
}
