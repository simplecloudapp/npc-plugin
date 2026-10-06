plugins {
    alias(libs.plugins.kotlin)
}

dependencies {
    api(project(":core"))
    api(libs.cloud.core)
    api(libs.cloud.annotations)
    compileOnly(libs.simplecloud.api)
    compileOnly(libs.bundles.adventure)

    testImplementation(libs.simplecloud.api)
    testImplementation(libs.bundles.adventure)
    testImplementation(testFixtures(project(":core")))
}
