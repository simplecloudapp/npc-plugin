dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.bundles.simpleCloud)

    compileOnly("io.lumine:Mythic-Dist:5.6.1")

    implementation(project(":npc-shared"))
}