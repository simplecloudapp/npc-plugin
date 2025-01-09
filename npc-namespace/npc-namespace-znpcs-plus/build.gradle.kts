dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.bundles.simpleCloudController)

    compileOnly("lol.pyr:znpcsplus-api:2.0.0-SNAPSHOT")

    implementation(project(":npc-shared"))
}