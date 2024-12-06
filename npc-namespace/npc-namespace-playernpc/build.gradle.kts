dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.bundles.simpleCloudController)

    compileOnly("dev.sergiferry:playernpc:2023.6")

    implementation(project(":npc-shared"))
}