import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

allprojects {
    group = "app.simplecloud.plugin"
    version = "1.0-SNAPSHOT"

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
        maven("https://maven.citizensnpcs.co/repo/citizens-repo")
        maven("https://buf.build/gen/maven")
        maven {
            name = "ranullRepoExternal"
            url = uri("https://repo.ranull.com/maven/external")
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.johnrengelman.shadow")

    dependencies {
        testImplementation(rootProject.libs.kotlinTest)
        implementation(rootProject.libs.kotlinJvm)
    }

    kotlin {
        jvmToolchain(21)
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }

    tasks.named("shadowJar", ShadowJar::class) {
        mergeServiceFiles()
        archiveFileName.set("${project.name}.jar")
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.processResources {
        expand("version" to project.version,
            "name" to project.name)
    }
}

tasks.processResources {
    expand("version" to project.version,
        "name" to project.name)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}