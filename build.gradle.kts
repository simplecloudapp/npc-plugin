import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

val baseVersion = "0.0.1"
val commitHash = System.getenv("COMMIT_HASH")
val snapshotVersion = "${baseVersion}-dev.$commitHash"

allprojects {
    group = "app.simplecloud.plugin"
    version = if (commitHash != null) snapshotVersion else baseVersion

    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
        maven {
            url = uri("https://libraries.minecraft.net")
        }
        maven {
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven("https://maven.citizensnpcs.co/repo")
        maven("https://repo.minebench.de")
        maven("https://buf.build/gen/maven")
        maven("https://repo.fancyplugins.de/releases")
        maven("https://mvn.lumine.io/repository/maven-public/")
        maven {
            name = "ranullRepoExternal"
            url = uri("https://repo.ranull.com/maven/external")
        }
        maven {
            name = "noxcrew-maven"
            url = uri("https://maven.noxcrew.com/public")
        }
        maven("https://repo.simplecloud.app/snapshots")
        maven("https://buf.build/gen/maven")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")

    dependencies {
        testImplementation(rootProject.libs.kotlinTest)
        compileOnly(rootProject.libs.kotlinJvm)
    }

    kotlin {
        jvmToolchain(21)
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
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

    tasks.shadowJar {
        exclude("kotlin")
        exclude("kotlinx")
        mergeServiceFiles()
    }
}

tasks.processResources {
    expand(
        "version" to project.version,
        "name" to project.name
    )
}

tasks.test {
    useJUnitPlatform()
}