dependencies {
    compileOnly(rootProject.libs.bundles.configurate)

    testImplementation(rootProject.libs.bundles.configurate)
    testImplementation(rootProject.libs.paper.api)
    testImplementation(rootProject.libs.simplecloud)
    testImplementation(rootProject.libs.simplecloud.plugin)
}
