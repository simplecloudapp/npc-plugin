dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.simplecloud)

    compileOnly("de.oliver:FancyNpcs:2.9.0")

    implementation(project(":npc-shared"))
}