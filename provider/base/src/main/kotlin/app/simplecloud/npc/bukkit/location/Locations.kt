package app.simplecloud.npc.bukkit.location

import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.location.NpcLocation
import org.bukkit.Location
import org.bukkit.World
import java.util.logging.Level
import java.util.logging.Logger

fun NpcLocation.toBukkitLocation(world: World): Location = Location(world, x, y, z, yaw, pitch)

fun Location.toNpcLocation(): NpcLocation? = world?.name?.let { NpcLocation(it, x, y, z, yaw, pitch) }

fun requireLoadedWorld(
    providerName: String,
    config: NpcConfig,
    logger: Logger,
    worldLookup: (name: String) -> World?,
): World? {
    val worldName = config.entity.location.world
    return worldLookup(worldName) ?: run {
        logger.log(Level.WARNING, "Cannot create $providerName NPC '${config.id}': world '$worldName' isn't loaded.")
        null
    }
}
