plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "npc-plugin"
include("npc-paper")
include("npc-shared")
include("npc-namespace")
include("npc-namespace:npc-namespace-citizens")
findProject(":npc-namespace:npc-namespace-citizens")?.name = "npc-namespace-citizens"
include("npc-namespace:npc-namespace-standalone")
findProject(":npc-namespace:npc-namespace-standalone")?.name = "npc-namespace-standalone"
include("npc-namespace:npc-namespace-playernpc")
findProject(":npc-namespace:npc-namespace-playernpc")?.name = "npc-namespace-playernpc"
include("npc-namespace:npc-namespace-fancynpcs")
findProject(":npc-namespace:npc-namespace-fancynpcs")?.name = "npc-namespace-fancynpcs"
include("npc-namespace:npc-namespace-mythicmobs")
findProject(":npc-namespace:npc-namespace-mythicmobs")?.name = "npc-namespace-mythicmobs"
