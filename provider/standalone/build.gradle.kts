plugins {
    alias(libs.plugins.kotlin)
}

dependencies {
    implementation(project(":provider:base"))
    compileOnly(libs.paper.api)
    compileOnly(project(":libs:packetevents", "shadowRuntimeElements"))
    testImplementation(project(":libs:packetevents", "shadowRuntimeElements"))
}
