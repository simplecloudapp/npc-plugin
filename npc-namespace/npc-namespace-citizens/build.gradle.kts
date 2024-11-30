dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.bundles.simpleCloudController)

    compileOnly("net.citizensnpcs:citizens-main:2.0.35-SNAPSHOT")

    implementation(project(":npc-shared"))
}