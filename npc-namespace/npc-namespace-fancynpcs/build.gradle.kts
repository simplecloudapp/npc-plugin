dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.bundles.simpleCloudController)

    compileOnly("de.oliver:FancyNpcs:2.4.0")

    implementation(project(":npc-shared"))
}