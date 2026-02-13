dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.simpleCloudApi)

    implementation(rootProject.libs.simpleCloudPluginApi)

    compileOnly("net.citizensnpcs:citizens-main:2.0.35-SNAPSHOT")

    implementation(project(":npc-shared"))
}