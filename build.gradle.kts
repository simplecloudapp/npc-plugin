plugins {
    alias(libs.plugins.kotlin) apply false
}

val baseVersion = "0.2.0"
val commitHash: String? = System.getenv("COMMIT_HASH")

allprojects {
    group = "app.simplecloud.npc"
    version = commitHash?.let { "$baseVersion-dev.$it" } ?: baseVersion
}

val adventureFloor = libs.versions.adventure.floor.get()
val serverProvidedAdventure = setOf(
    "adventure-api",
    "adventure-key",
    "adventure-text-minimessage",
    "adventure-text-serializer-commons",
    "adventure-text-serializer-gson",
    "adventure-text-serializer-json",
    "adventure-text-serializer-legacy",
    "adventure-text-serializer-plain",
    "adventure-text-logger-slf4j",
)

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        dependencies {
            "implementation"(rootProject.libs.kotlin.coroutines)
            "testImplementation"(rootProject.libs.kotlin.test)
        }

        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        }

        configurations.matching {
            it.name == "compileClasspath" || it.name.endsWith("CompileClasspath")
        }.configureEach {
            resolutionStrategy.eachDependency {
                if (requested.group == "net.kyori" && requested.name in serverProvidedAdventure) {
                    val target = findProperty("npc.adventureVersion")?.toString() ?: adventureFloor
                    useVersion(target)
                    because("the paper-api this module compiles against provides Adventure $target")
                }
            }
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
