import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.named

plugins {
    alias(libs.plugins.minotaur)
}

dependencies {
    compileOnly(rootProject.libs.paperApi)
    implementation(rootProject.libs.bundles.simpleCloudController)
    implementation(rootProject.libs.bundles.cloudPaper)
    implementation(rootProject.libs.interfacesApi)

    implementation(project(":npc-shared"))
    implementation(project(":npc-namespace:npc-namespace-citizens"))
    implementation(project(":npc-namespace:npc-namespace-standalone"))
    implementation(project(":npc-namespace:npc-namespace-playernpc"))
    implementation(project(":npc-namespace:npc-namespace-fancynpcs"))
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
        "1.21.4"
    )
    loaders.add("paper")
    changelog.set("https://docs.simplecloud.app/changelog")
    syncBodyFrom.set(rootProject.file("README.md").readText())
}