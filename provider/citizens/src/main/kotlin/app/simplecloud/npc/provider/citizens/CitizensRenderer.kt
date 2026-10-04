package app.simplecloud.npc.provider.citizens

import app.simplecloud.npc.bukkit.glow.GlowTeams
import app.simplecloud.npc.bukkit.interact.OnNpcInteract
import app.simplecloud.npc.bukkit.location.toBukkitLocation
import app.simplecloud.npc.bukkit.location.toNpcLocation
import app.simplecloud.npc.bukkit.provider.LinkedProviderRenderer
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.location.NpcLocation
import app.simplecloud.npc.core.render.ProviderNpcSnapshot
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.event.CitizensEnableEvent
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.trait.LookClose
import net.citizensnpcs.trait.MirrorTrait
import net.citizensnpcs.trait.SkinTrait
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause
import org.bukkit.plugin.Plugin

class CitizensRenderer(
    plugin: Plugin,
    private val onInteract: OnNpcInteract,
) : LinkedProviderRenderer<NPC>(plugin, "Citizens") {

    override val supportsFixedSkin: Boolean = true

    private val registry by lazy { CitizensAPI.createInMemoryNPCRegistry(REGISTRY_NAME) }

    @Volatile
    private var citizensEnabled = false

    override fun isReady(): Boolean = citizensEnabled

    override fun onEnabled() {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable { citizensEnabled = true }, READY_FALLBACK_TICKS)
    }

    override fun createListener(): Listener = CitizensInteractListener(::configIdForEventKey, onInteract)

    override fun createReadinessListener(): Listener = object : Listener {
        @EventHandler
        fun onCitizensEnabled(event: CitizensEnableEvent) {
            citizensEnabled = true
        }
    }

    override fun onDisabled() {
        runCatching { CitizensAPI.removeNamedNPCRegistry(REGISTRY_NAME) }
    }

    override fun eventKeyOf(tracked: NPC): String = tracked.uniqueId.toString()

    override fun findByReference(reference: String): NPC? =
        reference.toIntOrNull()?.let(CitizensAPI.getNPCRegistry()::getById)

    override fun create(config: NpcConfig, world: World): NPC {
        val npc = registry.createNPC(EntityType.PLAYER, config.entity.customName ?: config.id)
        applyAppearance(npc, config)
        npc.spawn(config.entity.location.toBukkitLocation(world))
        syncGlowColor(npc, config)

        return npc
    }

    override fun destroy(tracked: NPC) {
        tracked.entity?.let { GlowTeams.clear(it.uniqueId) }
        runCatching { tracked.destroy() }
    }

    override fun deleteLegacy(legacy: NPC) {
        legacy.destroy()
        CitizensAPI.getNPCRegistry().saveToStore()
    }

    override fun refreshOwned(config: NpcConfig, tracked: NPC) {
        val skinChanged = config.entity.skin.texture != null &&
            tracked.getOrAddTrait(SkinTrait::class.java).texture != config.entity.skin.texture
        applyAppearance(tracked, config)

        if (skinChanged && tracked.isSpawned) {
            val world = worldOf(config) ?: return
            tracked.despawn()
            tracked.spawn(config.entity.location.toBukkitLocation(world))
        }

        syncGlowColor(tracked, config)
    }

    override fun teleportOwned(config: NpcConfig, tracked: NPC, world: World) {
        tracked.teleport(config.entity.location.toBukkitLocation(world), TeleportCause.PLUGIN)
    }

    override fun locationOf(tracked: NPC): NpcLocation? = tracked.storedLocation?.toNpcLocation()

    override fun linkCandidates(): List<Pair<String, NPC>> = CitizensAPI.getNPCRegistry().map { it.id.toString() to it }

    override fun snapshotOf(found: NPC, config: NpcConfig): ProviderNpcSnapshot {
        val skin = found.getTraitNullable(SkinTrait::class.java)?.let(::skinOf)
        val nameplateVisible = found.data().get(NPC.Metadata.NAMEPLATE_VISIBLE, true)
        val customName = found.rawName.takeIf { nameplateVisible && it.isNotBlank() && it != config.id }

        return ProviderNpcSnapshot(locationOf(found), skin, customName)
    }

    private fun skinOf(trait: SkinTrait): NpcConfig.SkinConfiguration? {
        val texture = trait.texture
        val signature = trait.signature
        if (texture.isNullOrBlank() || signature.isNullOrBlank()) return null

        return NpcConfig.SkinConfiguration(texture, signature, trait.skinName?.takeIf(String::isNotBlank))
    }

    private fun syncGlowColor(npc: NPC, config: NpcConfig) {
        npc.entity?.let { GlowTeams.set(it, config.entity.glowColor) }
    }

    private fun applyAppearance(npc: NPC, config: NpcConfig) {
        npc.name = config.entity.customName ?: config.id
        npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, config.entity.customName != null)
        npc.data().setPersistent(NPC.Metadata.GLOWING, config.entity.glowing)
        applySkin(npc, config.entity.skin)
        npc.removeTrait(LookClose::class.java)
        npc.getOrAddTrait(LookClose::class.java).apply {
            lookClose(config.entity.lookAtPlayer)
            setPerPlayer(true)
            setRange(config.entity.lookAtPlayerDistance)
        }
    }

    private fun applySkin(npc: NPC, skin: NpcConfig.SkinConfiguration) {
        val texture = skin.texture
        val signature = skin.signature
        if (texture == null || signature == null) {
            npc.removeTrait(SkinTrait::class.java)
            npc.getOrAddTrait(MirrorTrait::class.java).isEnabled = true
            return
        }

        npc.removeTrait(MirrorTrait::class.java)
        npc.getOrAddTrait(SkinTrait::class.java).setSkinPersistent(skin.sourcePlayer ?: npc.name, signature, texture)
    }

    private companion object {
        private const val REGISTRY_NAME = "simplecloud-npc"
        private const val READY_FALLBACK_TICKS = 2L
    }
}
