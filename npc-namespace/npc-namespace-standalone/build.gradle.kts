dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.bundles.simpleCloudController)

    implementation(project(":npc-shared"))
}