dependencies {
    compileOnly(rootProject.libs.paperApi)
    implementation(rootProject.libs.bundles.simpleCloudController)
    implementation(rootProject.libs.bundles.cloudPaper)

    implementation(project(":npc-shared"))
    implementation(project(":npc-namespace:npc-namespace-citizens"))
    implementation(project(":npc-namespace:npc-namespace-standalone"))
}