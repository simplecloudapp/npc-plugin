plugins {
    `java-library`
    alias(libs.plugins.shadow)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    implementation(libs.packetevents.spigot)
    implementation(libs.entitylib.spigot)
    implementation(libs.bundles.packet.adventure)

    constraints {
        implementation(libs.entitylib.api) { version { strictly(libs.versions.entitylib.get()) } }
        implementation(libs.entitylib.common) { version { strictly(libs.versions.entitylib.get()) } }
    }
}

configurations.runtimeClasspath {
    exclude(group = "com.google.code.gson")
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
    relocate("net.kyori", "app.simplecloud.npc.relocate.packets.kyori")
    dependencies {
        exclude(dependency("org.jetbrains:annotations"))
    }
    exclude("**/module-info.class")
    exclude("META-INF/maven/**")
}

tasks.named("jar") { enabled = false }
