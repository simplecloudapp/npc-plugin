dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.simplecloud)

    implementation(project(":npc-shared"))
}