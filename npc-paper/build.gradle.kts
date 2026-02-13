import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.minotaur)
}

dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.simpleCloudApi)

    implementation(rootProject.libs.bundles.cloudPaper)
    implementation(rootProject.libs.interfacesApi)
    implementation(rootProject.libs.simpleCloudPluginApi)

    implementation(project(":npc-shared"))
    implementation(project(":npc-namespace:npc-namespace-citizens"))
    implementation(project(":npc-namespace:npc-namespace-standalone"))
    implementation(project(":npc-namespace:npc-namespace-fancynpcs"))
    implementation(project(":npc-namespace:npc-namespace-mythicmobs"))
}

tasks.named("shadowJar", ShadowJar::class) {
    mergeServiceFiles()
    archiveFileName.set("simplecloud-npc.jar")
}

modrinth {
    token.set(project.findProperty("modrinthToken") as String? ?: System.getenv("MODRINTH_TOKEN"))
    projectId.set("eyNPY9oJ")
    versionNumber.set(rootProject.version.toString())
    versionType.set("beta")
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
        "1.21.11"
    )
    loaders.add("paper")
    changelog.set("https://docs.simplecloud.app/changelog")
    syncBodyFrom.set(rootProject.file("README.md").readText())
}
