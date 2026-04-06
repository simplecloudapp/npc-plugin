import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

val baseVersion = "0.1.0"
val commitHash = System.getenv("COMMIT_HASH")
val snapshotVersion = "${baseVersion}-dev.$commitHash"

allprojects {
    group = "app.simplecloud.plugin"
    version = if (commitHash != null) snapshotVersion else baseVersion

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://libraries.minecraft.net")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.citizensnpcs.co/repo")
        maven("https://repo.minebench.de")
        maven("https://buf.build/gen/maven")
        maven("https://repo.fancyplugins.de/releases")
        maven("https://mvn.lumine.io/repository/maven-public/")
        maven("https://repo.ranull.com/maven/external")
        maven("https://maven.noxcrew.com/public")
        maven("https://repo.simplecloud.app/snapshots")
        maven("https://buf.build/gen/maven")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")

    dependencies {
        testImplementation(rootProject.libs.kotlin.test)
        implementation(rootProject.libs.kotlin.jvm)
        implementation(rootProject.libs.kotlin.coroutines)

        compileOnly(rootProject.libs.paper.api)

        compileOnly(rootProject.libs.simplecloud)
        compileOnly(rootProject.libs.simplecloud.plugin)
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            apiVersion.set(KotlinVersion.KOTLIN_2_0)
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.processResources {
        expand(
            "version" to project.version,
            "name" to project.name
        )
    }

    /* TODO: fix here
    tasks.shadowJar {
        exclude("kotlin")
        exclude("kotlinx")
        mergeServiceFiles()

        relocate("com.google.protobuf", "app.simplecloud.relocate.google.protobuf")
        relocate("com.google.common", "app.simplecloud.relocate.google.common")
        relocate("io.grpc", "app.simplecloud.relocate.io.grpc")

        relocate("org.incendo", "app.simplecloud.npc.plugin.relocate.incendo")
        relocate("org.spongepowered", "app.simplecloud.npc.plugin.relocate.spongepowered")
        relocate("app.simplecloud.plugin.api", "app.simplecloud.npc.plugin.relocate.plugin.api")
    }

     */
}