package app.simplecloud.npc.provider.fancynpcs

import app.simplecloud.npc.bukkit.interact.OnNpcInteract
import app.simplecloud.npc.bukkit.location.toBukkitLocation
import app.simplecloud.npc.bukkit.location.toNpcLocation
import app.simplecloud.npc.bukkit.provider.LinkedProviderRenderer
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.location.NpcLocation
import app.simplecloud.npc.core.render.ProviderNpcSnapshot
import de.oliver.fancynpcs.api.FancyNpcsPlugin
import de.oliver.fancynpcs.api.Npc
import de.oliver.fancynpcs.api.NpcData
import de.oliver.fancynpcs.api.events.NpcCreateEvent
import de.oliver.fancynpcs.api.events.NpcsLoadedEvent
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.util.UUID

class FancyNpcsRenderer(
    plugin: Plugin,
    private val onInteract: OnNpcInteract,
) : LinkedProviderRenderer<Npc>(plugin, "FancyNpcs") {

    private val fancyNpcs get() = FancyNpcsPlugin.get()

    @Volatile
    private var npcsLoaded = false

    override fun isReady(): Boolean = npcsLoaded || runCatching { fancyNpcs.npcManager.isLoaded }.getOrDefault(false)

    override fun createListener(): Listener = FancyNpcsInteractListener(::configIdForEventKey, onInteract)

    override fun createReadinessListener(): Listener = object : Listener {
        @EventHandler
        fun onNpcsLoaded(event: NpcsLoadedEvent) {
            npcsLoaded = true
        }
    }

    override fun eventKeyOf(tracked: Npc): String = tracked.data.id

    override fun findByReference(reference: String): Npc? =
        fancyNpcs.npcManager.getNpcById(reference) ?: fancyNpcs.npcManager.getNpc(reference)

    override fun create(config: NpcConfig, world: World): Npc? {
        val npcData = NpcData(config.id, ownerUuid(config.id), config.entity.location.toBukkitLocation(world)).apply {
            applyAppearance(config)
            setMirrorSkin(true)
        }

        val npc = fancyNpcs.npcAdapter.apply(npcData)
        npc.setSaveToFile(false)

        val createEvent = NpcCreateEvent(npc, Bukkit.getConsoleSender()).also(Bukkit.getPluginManager()::callEvent)
        if (createEvent.isCancelled) {
            logger.warning("FancyNpcs NPC creation for '${config.id}' was cancelled by another plugin.")
            return null
        }

        npc.create()
        fancyNpcs.npcManager.registerNpc(npc)
        npc.spawnForAll()

        return npc
    }

    override fun destroy(tracked: Npc) {
        runCatching {
            tracked.removeForAll()
            fancyNpcs.npcManager.removeNpc(tracked)
        }
    }

    override fun deleteLegacy(legacy: Npc) {
        legacy.removeForAll()
        fancyNpcs.npcManager.removeNpc(legacy)
        fancyNpcs.npcManager.saveNpcs(true)
    }

    override fun linkCandidates(): List<Pair<String, Npc>> =
        fancyNpcs.npcManager.allNpcs.flatMap { listOf(it.data.name to it, it.data.id to it) }

    override fun refreshOwned(config: NpcConfig, tracked: Npc) {
        tracked.data.applyAppearance(config)
        tracked.updateForAll()
    }

    override fun teleportOwned(config: NpcConfig, tracked: Npc, world: World) {
        tracked.data.setLocation(config.entity.location.toBukkitLocation(world))
        tracked.moveForAll()
    }

    override fun locationOf(tracked: Npc): NpcLocation? = tracked.data.location?.toNpcLocation()

    override fun snapshotOf(found: Npc, config: NpcConfig): ProviderNpcSnapshot {
        val data = found.data
        val skin = data.skinData?.takeIf { !data.isMirrorSkin && it.hasTexture() }?.let {
            NpcConfig.SkinConfiguration(it.textureValue, it.textureSignature, it.identifier?.takeIf(String::isNotBlank))
        }
        val customName = data.displayName?.takeIf { it.isNotBlank() && it != EMPTY_NAME && it != config.id }

        return ProviderNpcSnapshot(locationOf(found), skin, customName)
    }

    private fun NpcData.applyAppearance(config: NpcConfig) {
        setDisplayName(config.entity.customName ?: EMPTY_NAME)
        setTurnToPlayer(config.entity.lookAtPlayer)
        setTurnToPlayerDistance(config.entity.lookAtPlayerDistance.toInt())
        setGlowing(config.entity.glowing)
        FancyGlowColor.apply(this, config.entity.glowColor)
    }

    private fun ownerUuid(id: String): UUID = UUID.nameUUIDFromBytes("simplecloud-npc:$id".toByteArray())

    private companion object {
        private const val EMPTY_NAME = "<empty>"
    }
}
