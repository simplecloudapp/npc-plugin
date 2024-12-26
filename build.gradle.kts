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
        maven("https://repo.fancyplugins.de/releases")
        maven {
            name = "ranullRepoExternal"
            url = uri("https://repo.ranull.com/maven/external")
        }
        maven {
            name = "labymod"
            url = uri("https://dist.labymod.net/api/v1/maven/release/")
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

    tasks.shadowJar {
        relocate("io.grpc", "app.simplecloud.relocate.grpc")
        relocate("app.simplecloud.controller", "app.simplecloud.relocate.controller")
        relocate("app.simplecloud.pubsub", "app.simplecloud.relocate.pubsub")
        relocate("app.simplecloud.droplet", "app.simplecloud.relocate.droplet")
        relocate("build.buf.gen", "app.simplecloud.relocate.buf")
        relocate("com.google.protobuf", "app.simplecloud.relocate.protobuf")
    }
}

tasks.processResources {
    expand("version" to project.version,
        "name" to project.name)
}

tasks.test {
    useJUnitPlatform()
}