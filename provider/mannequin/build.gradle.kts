plugins {
    alias(libs.plugins.kotlin)
}

ext["npc.adventureVersion"] = libs.versions.adventure.mannequin.get()

dependencies {
    compileOnly(libs.paper.mannequin.api)

    implementation(project(":provider:base"))
}
