package app.simplecloud.npc.provider.znpcsplus

import app.simplecloud.npc.bukkit.interact.OnNpcInteract
import app.simplecloud.npc.bukkit.location.toBukkitLocation
import app.simplecloud.npc.bukkit.provider.LinkedProviderRenderer
import app.simplecloud.npc.core.config.GlowColors
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.location.NpcLocation
import lol.pyr.znpcsplus.api.NpcApiProvider
import lol.pyr.znpcsplus.api.npc.NpcEntry
import lol.pyr.znpcsplus.api.skin.SkinDescriptor
import lol.pyr.znpcsplus.util.LookType
import lol.pyr.znpcsplus.util.NamedColor
import org.bukkit.World
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.util.logging.Level
import lol.pyr.znpcsplus.util.NpcLocation as ZLocation

class ZNpcsPlusRenderer(
    plugin: Plugin,
    private val onInteract: OnNpcInteract,
) : LinkedProviderRenderer<NpcEntry>(plugin, "ZNPCsPlus") {

    private val api get() = NpcApiProvider.get()

    override fun createListener(): Listener = ZNpcsPlusInteractListener(::configIdForEventKey, onInteract)

    override fun eventKeyOf(tracked: NpcEntry): String = tracked.id

    override fun findByReference(reference: String): NpcEntry? = api.npcRegistry.getById(reference)

    override fun create(config: NpcConfig, world: World): NpcEntry? {
        val entryId = entryId(config.id)
        api.npcRegistry.getById(entryId)?.let { api.npcRegistry.delete(entryId) }

        val playerType = api.npcTypeRegistry.getByName("player") ?: run {
            logger.warning("Cannot create ZNPCsPlus NPC '${config.id}': the 'player' NPC type is unavailable.")
            return null
        }

        val entry = api.npcRegistry.create(
            entryId,
            world,
            playerType,
            ZLocation(config.entity.location.toBukkitLocation(world)),
        )

        return try {
            applyAppearance(entry, config)
            entry.apply {
                setProcessed(true)
                setSave(false)
                setAllowCommandModification(false)
            }
        } catch (exception: Exception) {
            api.npcRegistry.delete(entry.id)
            logger.log(
                Level.WARNING,
                "Failed to configure ZNPCsPlus NPC '${config.id}', rolled back its creation.",
                exception,
            )
            null
        }
    }

    override fun destroy(tracked: NpcEntry) {
        runCatching { api.npcRegistry.delete(tracked.id) }
    }

    override fun deleteLegacy(legacy: NpcEntry) {
        api.npcRegistry.delete(legacy.id)
        api.npcRegistry.save()
    }

    override fun linkCandidates(): List<Pair<String, NpcEntry>> =
        api.npcRegistry.allIds.mapNotNull { id -> api.npcRegistry.getById(id)?.let { id to it } }

    override fun refreshOwned(config: NpcConfig, tracked: NpcEntry) = applyAppearance(tracked, config)

    override fun teleportOwned(config: NpcConfig, tracked: NpcEntry, world: World) {
        tracked.npc.setWorld(world)
        tracked.npc.location = ZLocation(config.entity.location.toBukkitLocation(world))
    }

    override fun locationOf(tracked: NpcEntry): NpcLocation? {
        val world = tracked.npc.world ?: return null
        val location = tracked.npc.location

        return NpcLocation(world.name, location.x, location.y, location.z, location.yaw, location.pitch)
    }

    private fun applyAppearance(entry: NpcEntry, config: NpcConfig) {
        val npc = entry.npc
        val lookProperty = checkNotNull(api.propertyRegistry.getByName("look", LookType::class.java)) {
            "ZNPCsPlus look property is unavailable"
        }
        npc.setProperty(lookProperty, if (config.entity.lookAtPlayer) LookType.PER_PLAYER else LookType.FIXED)

        val lookDistanceProperty = checkNotNull(api.propertyRegistry.getByName("look_distance", Double::class.java)) {
            "ZNPCsPlus look-distance property is unavailable"
        }
        npc.setProperty(lookDistanceProperty, config.entity.lookAtPlayerDistance)

        val skinProperty = checkNotNull(api.propertyRegistry.getByName("skin", SkinDescriptor::class.java)) {
            "ZNPCsPlus skin property is unavailable"
        }
        npc.setProperty(skinProperty, api.skinDescriptorFactory.createMirrorDescriptor())

        val glowProperty = checkNotNull(api.propertyRegistry.getByName("glow", NamedColor::class.java)) {
            "ZNPCsPlus glow property is unavailable"
        }
        npc.setProperty(glowProperty, glowColorOf(config))
    }

    private fun glowColorOf(config: NpcConfig): NamedColor? {
        if (!config.entity.glowing) return null
        val name = config.entity.glowColor?.lowercase()?.takeIf { GlowColors.resolve(it) != null } ?: "white"

        return NamedColor.valueOf(name.uppercase())
    }

    private fun entryId(configId: String): String = "scnpc_${configId.lowercase()}"
}
