plugins {
    alias(libs.plugins.kotlin)
    `java-test-fixtures`
}

dependencies {
    api(libs.configurate.yaml)
    api(libs.simplecloud.plugin)
    compileOnly(libs.simplecloud.api)
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.text.minimessage)

    testImplementation(libs.simplecloud.api)
    testImplementation(libs.bundles.adventure)
    testFixturesImplementation(libs.simplecloud.api)
    testFixturesImplementation(libs.kotlin.coroutines)
}
