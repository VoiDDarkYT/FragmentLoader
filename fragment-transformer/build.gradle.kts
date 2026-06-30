plugins {
    java
}

dependencies {
    implementation(project(":fragment-api"))
    implementation(project(":fragment-installer"))

    // ASM core - чтение/запись/трансформация байткода классов
    implementation("org.ow2.asm:asm:9.7")
    // ASM commons - содержит ClassRemapper/Remapper, используемые для
    // ремаппинга obfuscated имён в читаемые по mojang mappings
    implementation("org.ow2.asm:asm-commons:9.7")
    // ASM tree - представление класса как дерева узлов (нужно для
    // более сложных трансформаций mixin/inject в fragment-transformer)
    implementation("org.ow2.asm:asm-tree:9.7")
}
