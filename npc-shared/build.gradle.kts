dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.simplecloud)

    implementation(rootProject.libs.simpleCloudPluginApi)

    compileOnly(rootProject.libs.bundles.configurate)
}