dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.interfacesApi)
    compileOnly(rootProject.libs.bundles.simpleCloudController)
    implementation(rootProject.libs.simpleCloudPluginApi)
    api(libs.bundles.configurate) {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
    }
}