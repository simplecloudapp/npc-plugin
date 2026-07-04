import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.minotaur)
}

dependencies {
    implementation(rootProject.libs.bundles.cloud)
    implementation(rootProject.libs.bundles.configurate)

    implementation(rootProject.libs.simplecloud.plugin)

    implementation(project(":npc-shared"))
    implementation(project(":npc-namespace:npc-namespace-citizens"))
    implementation(project(":npc-namespace:npc-namespace-standalone"))
    implementation(project(":npc-namespace:npc-namespace-fancynpcs"))
    implementation(project(":npc-namespace:npc-namespace-mythicmobs"))
    implementation(project(":npc-namespace:npc-namespace-znpcs-plus"))
}

tasks.named("shadowJar", ShadowJar::class) {
    mergeServiceFiles()
    archiveFileName.set("simplecloud-npc.jar")
}

modrinth {
    token.set(project.findProperty("modrinthToken") as String? ?: System.getenv("MODRINTH_TOKEN"))
    projectId.set("eyNPY9oJ")
    versionNumber.set(rootProject.version.toString())
    versionType.set("release")
    uploadFile.set(tasks.shadowJar)
    gameVersions.addAll(
        "1.20",
        "1.20.1",
        "1.20.2",
        "1.20.3",
        "1.20.4",
        "1.20.5",
        "1.20.6",
        "1.21",
        "1.21.1",
        "1.21.2",
        "1.21.3",
        "1.21.4",
        "1.21.5",
        "1.21.6",
        "1.21.7",
        "1.21.8",
        "1.21.9",
        "1.21.10",
        "1.21.11",
        "26.1",
        "26.1.1",
        "26.1.2",
        "26.2",
    )
    loaders.addAll("paper", "purpur")
    changelog.set("https://docs.simplecloud.app/changelog")
    syncBodyFrom.set(rootProject.file("README.md").readText())
}
