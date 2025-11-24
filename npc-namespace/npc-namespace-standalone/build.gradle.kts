dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.bundles.simpleCloud)

    implementation(project(":npc-shared"))
}