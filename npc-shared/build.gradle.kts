dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.simpleCloudApi)

    implementation(rootProject.libs.simpleCloudPluginApi)
    implementation(rootProject.libs.interfacesApi)
    implementation(rootProject.libs.bundles.configurate)
}