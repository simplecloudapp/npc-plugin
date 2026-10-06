package app.simplecloud.npc.provider.mannequin

import app.simplecloud.npc.bukkit.NpcKeys
import app.simplecloud.npc.bukkit.equipment.EquipmentItems
import app.simplecloud.npc.bukkit.glow.GlowTeams
import app.simplecloud.npc.bukkit.interact.InteractableEntities
import app.simplecloud.npc.bukkit.location.requireLoadedWorld
import app.simplecloud.npc.bukkit.location.toBukkitLocation
import app.simplecloud.npc.bukkit.location.toNpcLocation
import app.simplecloud.npc.bukkit.look.LookAtPlayerDriver
import app.simplecloud.npc.bukkit.look.LookAtPlayerTicker
import app.simplecloud.npc.bukkit.packet.PacketViewerTracker
import app.simplecloud.npc.bukkit.provider.WorldReconciler
import app.simplecloud.npc.bukkit.scheduling.sync
import app.simplecloud.npc.bukkit.skin.FixedSkinRenderer
import app.simplecloud.npc.bukkit.skin.NpcProfiles
import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.config.NpcPose
import app.simplecloud.npc.core.location.NpcLocation
import com.destroystokyo.paper.SkinParts
import com.destroystokyo.paper.profile.PlayerProfile
import com.destroystokyo.paper.profile.ProfileProperty
import io.papermc.paper.datacomponent.item.ResolvableProfile
import io.papermc.paper.event.player.PlayerTrackEntityEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Entity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.event.world.EntitiesUnloadEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import kotlin.math.floor

class MannequinRenderer(
    private val plugin: Plugin,
) : FixedSkinRenderer, WorldReconciler, Listener {

    override var knownIdsProvider: (() -> Set<String>)? = null

    private val entityIdByNpcId = ConcurrentHashMap<String, UUID>()
    private val ownedEntityIds: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    private val networkIdByEntityId = ConcurrentHashMap<UUID, Int>()
    private val configsById = ConcurrentHashMap<String, NpcConfig>()
    private val lookTicker = LookAtPlayerTicker()
    private val lookDriver = LookAtPlayerDriver(plugin, lookTicker)
    private val viewers = PacketViewerTracker(plugin)
    private val ranges = ConcurrentHashMap<String, RangeEntry>()

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        viewers.start()
        lookDriver.start { lookTargets() }
    }

    override fun onDisable() {
        lookDriver.stop()
        viewers.stop()
        ranges.clear()
        HandlerList.unregisterAll(this)

        networkIdByEntityId.values.forEach(InteractableEntities::unregister)
        networkIdByEntityId.clear()
        entityIdByNpcId.clear()
        ownedEntityIds.clear()
        configsById.clear()
    }

    override fun spawn(config: NpcConfig): NpcConfig = plugin.sync {
        val world = requireLoadedWorld("Mannequin", config, logger, Bukkit::getWorld) ?: return@sync config
        val location = config.entity.location.toBukkitLocation(world)

        val existing = trackedEntity(config.id) ?: findTaggedMannequin(world, config)
        val mannequin = existing ?: world.spawn(location, Mannequin::class.java)

        val moved = existing != null &&
            (existing.world != world || existing.location.distanceSquared(location) > TELEPORT_EPSILON)
        if (moved) existing.teleport(location)

        configure(mannequin, config)
        track(config.id, mannequin)
        configsById[config.id] = config
        updateRange(config)

        config
    }

    override fun despawn(config: NpcConfig) {
        plugin.sync {
            ranges.remove(config.id)?.let(viewers::remove)
            untrack(config.id)?.let { entityId ->
                GlowTeams.clear(entityId)
                Bukkit.getEntity(entityId)?.remove()
            }
        }
    }

    override fun refresh(config: NpcConfig) {
        spawn(config)
    }

    override fun teleport(config: NpcConfig) {
        plugin.sync {
            val entity = trackedEntity(config.id) ?: run {
                spawn(config)
                return@sync
            }

            val world = Bukkit.getWorld(config.entity.location.world) ?: return@sync
            entity.teleport(config.entity.location.toBukkitLocation(world))
            updateRange(config)
        }
    }

    override fun locationOf(config: NpcConfig): NpcLocation? = trackedEntity(config.id)?.location?.toNpcLocation()

    override fun reconcileLoadedWorlds() {
        plugin.sync {
            Bukkit.getWorlds().forEach { world ->
                world.entities.filterIsInstance<Mannequin>().forEach(::reconcileEntity)
            }
        }
    }

    @EventHandler
    fun onEntitiesLoad(event: EntitiesLoadEvent) {
        event.entities.filterIsInstance<Mannequin>().forEach(::reconcileEntity)
    }

    @EventHandler
    fun onEntitiesUnload(event: EntitiesUnloadEvent) {
        event.entities.filterIsInstance<Mannequin>().forEach { mannequin ->
            networkIdByEntityId.remove(mannequin.uniqueId)?.let(::forgetNetworkId)
        }
    }

    @EventHandler
    fun onPlayerTrack(event: PlayerTrackEntityEvent) {
        if (event.entity.uniqueId !in ownedEntityIds) return
        lookTicker.forgetPair(event.entity.entityId, event.player.uniqueId)
    }

    private fun reconcileEntity(mannequin: Mannequin) {
        val npcId = taggedNpcId(mannequin) ?: return

        val knownIds = knownIdsProvider?.invoke()
        if (knownIds != null && npcId.lowercase() !in knownIds) {
            GlowTeams.clear(mannequin.uniqueId)
            mannequin.remove()
            logger.info("Removed a leftover mannequin of NPC '$npcId': the NPC is gone or no longer a mannequin.")
            return
        }

        val tracked = entityIdByNpcId[npcId]
        if (tracked != null && tracked != mannequin.uniqueId) {
            GlowTeams.clear(mannequin.uniqueId)
            mannequin.remove()
            logger.info("Removed duplicate mannequin of NPC '$npcId'.")
            return
        }

        track(npcId, mannequin)
    }

    private fun configure(mannequin: Mannequin, config: NpcConfig) {
        mannequin.apply {
            setProfile(ResolvableProfile.resolvableProfile(buildProfile(config)))
            setSkinParts(SkinParts.allParts())
            isImmovable = true
            isInvulnerable = true
            isCollidable = false
            isSilent = true
            isPersistent = true
            isVisibleByDefault = false
            isGlowing = config.entity.glowing
        }
        GlowTeams.set(mannequin, config.entity.glowColor)
        hideDescription(mannequin)

        val customName = config.entity.customName
        mannequin.customName(customName?.let(MiniMessage.miniMessage()::deserialize))
        mannequin.isCustomNameVisible = customName != null

        mannequin.persistentDataContainer.set(NpcKeys.MANNEQUIN, PersistentDataType.STRING, config.id)
        applyBody(mannequin, config)
    }

    private fun applyBody(mannequin: Mannequin, config: NpcConfig) {
        val configured = config.entity.equipment
        mannequin.equipment.apply {
            setItemInMainHand(configured.mainHand?.let(EquipmentItems::toItemStack))
            setItemInOffHand(configured.offHand?.let(EquipmentItems::toItemStack))
            helmet = configured.helmet?.let(EquipmentItems::toItemStack)
            chestplate = configured.chestplate?.let(EquipmentItems::toItemStack)
            leggings = configured.leggings?.let(EquipmentItems::toItemStack)
            boots = configured.boots?.let(EquipmentItems::toItemStack)
        }

        val pose = when (config.entity.pose) {
            NpcPose.STANDING -> Pose.STANDING
            NpcPose.SNEAKING -> Pose.SNEAKING
            NpcPose.SWIMMING -> Pose.SWIMMING
            NpcPose.SLEEPING -> Pose.SLEEPING
        }
        runCatching { mannequin.setPose(pose, pose != Pose.STANDING) }
            .onFailure { logger.log(Level.FINE, "Could not set the mannequin pose", it) }

        mannequin.getAttribute(Attribute.SCALE)?.baseValue = config.entity.effectiveScale()
    }

    private fun updateRange(config: NpcConfig) {
        val location = config.entity.location
        val entry = ranges.getOrPut(config.id) {
            RangeEntry(config.id, location.world, location.x, location.y, location.z, config.entity.viewDistance)
                .also(viewers::add)
        }
        entry.range = config.entity.viewDistance
        entry.moveTo(location.world, location.x, location.y, location.z)
    }

    private inner class RangeEntry(
        private val npcId: String,
        world: String,
        x: Double,
        y: Double,
        z: Double,
        range: Double,
    ) : PacketViewerTracker.Entry("Mannequin $npcId", world, x, y, z, range) {
        override fun show(player: Player) {
            trackedEntity(npcId)?.let { player.showEntity(plugin, it) }
        }

        override fun hide(player: Player) {
            trackedEntity(npcId)?.let { player.hideEntity(plugin, it) }
        }

        override fun forget(uuid: UUID) {
            Bukkit.getPlayer(uuid)?.let(::hide)
        }
    }

    private fun buildProfile(config: NpcConfig): PlayerProfile =
        Bukkit.createProfile(NpcProfiles.uuidFor(config), NpcProfiles.nameFor(config.id)).apply {
            val skin = config.entity.skin
            if (skin.texture != null && skin.signature != null) {
                setProperty(ProfileProperty("textures", skin.texture!!, skin.signature))
            }
        }

    private fun hideDescription(mannequin: Mannequin) {
        runCatching { mannequin.setDescription(null) }
            .onFailure { logger.log(Level.FINE, "Could not hide the mannequin description", it) }
    }

    private fun lookTargets(): List<LookAtPlayerDriver.Target> = configsById.mapNotNull { (id, config) ->
        if (!config.entity.lookAtPlayer) return@mapNotNull null
        val entity = trackedEntity(id) ?: return@mapNotNull null
        val location = entity.location

        LookAtPlayerDriver.Target(
            npcId = id,
            entityId = entity.entityId,
            x = location.x,
            y = location.y,
            z = location.z,
            viewers = entity.trackedBy.filter { it.world == entity.world },
            maxDistance = config.entity.lookAtPlayerDistance,
        )
    }

    private fun findTaggedMannequin(world: World, config: NpcConfig): Mannequin? {
        val location = config.entity.location
        val chunk = world.getChunkAt(floor(location.x).toInt() shr 4, floor(location.z).toInt() shr 4)

        return chunk.entities.filterIsInstance<Mannequin>().firstOrNull { taggedNpcId(it) == config.id }
    }

    private fun taggedNpcId(entity: Entity): String? =
        entity.persistentDataContainer.get(NpcKeys.MANNEQUIN, PersistentDataType.STRING)

    private fun trackedEntity(npcId: String): Mannequin? =
        entityIdByNpcId[npcId]?.let(Bukkit::getEntity) as? Mannequin

    private fun track(npcId: String, mannequin: Mannequin) {
        entityIdByNpcId.put(npcId, mannequin.uniqueId)?.let { previous ->
            if (previous != mannequin.uniqueId) ownedEntityIds.remove(previous)
        }
        ownedEntityIds += mannequin.uniqueId

        networkIdByEntityId.put(mannequin.uniqueId, mannequin.entityId)?.let { previous ->
            if (previous != mannequin.entityId) forgetNetworkId(previous)
        }
        InteractableEntities.register(mannequin.entityId, npcId, realEntity = true)
    }

    private fun untrack(npcId: String): UUID? {
        configsById.remove(npcId)
        return entityIdByNpcId.remove(npcId)?.also { entityId ->
            ownedEntityIds.remove(entityId)
            networkIdByEntityId.remove(entityId)?.let(::forgetNetworkId)
        }
    }

    private fun forgetNetworkId(networkId: Int) {
        InteractableEntities.unregister(networkId)
        lookTicker.forgetEntity(networkId)
    }

    private companion object {
        private const val TELEPORT_EPSILON = 0.0001
        private val logger = NpcLog.logger
    }
}
