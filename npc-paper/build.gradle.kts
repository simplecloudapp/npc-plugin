import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.named

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