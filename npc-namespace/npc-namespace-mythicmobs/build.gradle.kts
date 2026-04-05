dependencies {
    compileOnly(rootProject.libs.paperApi)
    compileOnly(rootProject.libs.simplecloud)

    compileOnly("io.lumine:Mythic-Dist:5.11.2")

    implementation(project(":npc-shared"))
}