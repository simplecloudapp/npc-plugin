dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.simpleCloudApi)

    implementation(rootProject.libs.simpleCloudPluginApi)

    implementation(project(":npc-shared"))
}