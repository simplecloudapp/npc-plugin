plugins {
    alias(libs.plugins.kotlin)
}

dependencies {
    implementation(project(":provider:base"))
    compileOnly(libs.paper.api)
    compileOnly(libs.znpcsplus.api)
}
