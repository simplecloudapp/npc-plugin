plugins {
    alias(libs.plugins.kotlin)
}

dependencies {
    api(project(":core"))
    compileOnly(libs.paper.api)
    compileOnly(project(":libs:packetevents", "shadowRuntimeElements"))
    testImplementation(libs.paper.api)
    testImplementation(project(":libs:packetevents", "shadowRuntimeElements"))
}
