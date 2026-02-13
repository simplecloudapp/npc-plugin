dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.simpleCloudApi)

    implementation(rootProject.libs.simpleCloudPluginApi)

    compileOnly("io.lumine:Mythic-Dist:5.11.2")

    implementation(project(":npc-shared"))
}