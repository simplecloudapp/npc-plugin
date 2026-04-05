dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.simplecloud)

    implementation(rootProject.libs.simpleCloudPluginApi)
    implementation(rootProject.libs.interfacesApi)

    compileOnly(rootProject.libs.bundles.configurate)
}