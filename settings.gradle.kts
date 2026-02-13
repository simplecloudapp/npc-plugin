plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "npc-plugin"

include(
    "npc-paper",
    "npc-shared",
    "npc-namespace",
    "npc-namespace:npc-namespace-citizens",
    "npc-namespace:npc-namespace-standalone",
    "npc-namespace:npc-namespace-fancynpcs",
    "npc-namespace:npc-namespace-mythicmobs"
)

findProject(":npc-namespace:npc-namespace-citizens")?.name = "npc-namespace-citizens"
findProject(":npc-namespace:npc-namespace-standalone")?.name = "npc-namespace-standalone"
findProject(":npc-namespace:npc-namespace-fancynpcs")?.name = "npc-namespace-fancynpcs"
findProject(":npc-namespace:npc-namespace-mythicmobs")?.name = "npc-namespace-mythicmobs"

//include("npc-namespace:npc-namespace-playernpc")
//findProject(":npc-namespace:npc-namespace-playernpc")?.name = "npc-namespace-playernpc"