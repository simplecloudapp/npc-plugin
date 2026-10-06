pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")

        exclusive("https://repo.codemc.io/repository/maven-public/", "com.github.retrooper")
        exclusive("https://maven.pvphub.me/tofaa", "io.github.tofaa2")
        exclusive("https://buf.build/gen/maven", "build.buf.gen")
        exclusive("https://repo.simplecloud.app/snapshots", "app.simplecloud.api", "app.simplecloud.plugin")
        exclusive("https://repo.pyr.lol/snapshots", "lol.pyr")
        exclusive("https://maven.citizensnpcs.co/repo", "net.citizensnpcs")
        exclusive("https://repo.fancyinnovations.com/releases", "de.oliver")
        exclusive("https://mvn.lumine.io/repository/maven-public/", "io.lumine")
    }
}

fun RepositoryHandler.exclusive(url: String, vararg groups: String) = exclusiveContent {
    forRepository { maven(url) }
    filter { groups.forEach(::includeGroup) }
}

rootProject.name = "npc-plugin"

include(
    "api",
    "core",
    "libs:packetevents",
    "platform:common",
    "platform:paper",
    "provider:base",
    "provider:standalone",
    "provider:mannequin",
    "provider:citizens",
    "provider:fancynpcs",
    "provider:mythicmobs",
    "provider:znpcsplus",
)
