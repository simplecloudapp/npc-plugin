package app.simplecloud.npc.provider.standalone

import app.simplecloud.npc.bukkit.compat.ServerCompat
import app.simplecloud.npc.bukkit.equipment.EquipmentItems
import app.simplecloud.npc.bukkit.glow.GlowTeamPackets
import app.simplecloud.npc.bukkit.interact.InteractableEntities
import app.simplecloud.npc.bukkit.location.requireLoadedWorld
import app.simplecloud.npc.bukkit.look.LookAtPlayerDriver
import app.simplecloud.npc.bukkit.look.LookAtPlayerTicker
import app.simplecloud.npc.bukkit.packet.PacketComponents
import app.simplecloud.npc.bukkit.packet.PacketPoses
import app.simplecloud.npc.bukkit.scheduling.sync
import app.simplecloud.npc.bukkit.skin.FixedSkinRenderer
import app.simplecloud.npc.bukkit.skin.NpcProfiles
import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.config.EquipmentSlot
import app.simplecloud.npc.core.config.GlowColors
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.location.NpcLocation
import app.simplecloud.npc.provider.standalone.interact.InteractionHitboxes
import com.github.retrooper.packetevents.protocol.attribute.Attributes
import com.github.retrooper.packetevents.protocol.player.TextureProperty
import com.github.retrooper.packetevents.protocol.player.UserProfile
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import me.tofaa.entitylib.meta.types.PlayerMeta
import me.tofaa.entitylib.wrapper.WrapperPlayer
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot as PacketEquipmentSlot
import com.github.retrooper.packetevents.protocol.world.Location as PacketLocation

class StandaloneNpcRenderer(
    private val plugin: Plugin,
) : FixedSkinRenderer {

    private val npcs = ConcurrentHashMap<String, TrackedNpc>()
    private val teams = GlowTeamPackets.CreationTracker()
    private val lookTicker = LookAtPlayerTicker()
    private val lookDriver = LookAtPlayerDriver(plugin, lookTicker)
    private val hitboxes = InteractionHitboxes(plugin)
    private var visibilityTask: BukkitTask? = null

    private val lifecycleListener = object : Listener {
        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            val uuid = event.player.uniqueId

            teams.forget(uuid)
            forget(uuid)
        }

        @EventHandler
        fun onRespawn(event: PlayerRespawnEvent) = forget(event.player.uniqueId)

        @EventHandler
        fun onWorldChange(event: PlayerChangedWorldEvent) = forget(event.player.uniqueId)
    }

    override fun onEnable() {
        hitboxes.onEnable()
        Bukkit.getPluginManager().registerEvents(lifecycleListener, plugin)

        visibilityTask = Bukkit.getScheduler().runTaskTimer(plugin, ::tickVisibility, 20L, VISIBILITY_TICK_PERIOD)
        lookDriver.start { lookTargets() }
    }

    override fun onDisable() {
        visibilityTask?.cancel()
        lookDriver.stop()

        hitboxes.onDisable()
        HandlerList.unregisterAll(lifecycleListener)

        npcs.values.toList().forEach(::removeEntity)
        npcs.clear()
        teams.removeAll()
    }

    private fun forget(uuid: UUID) {
        npcs.values.forEach { it.entity.removeViewerSilently(uuid) }
        lookTicker.forgetViewer(uuid)
    }

    fun cleanupStaleHitboxes() {
        hitboxes.cleanupStale()
    }

    override fun spawn(config: NpcConfig): NpcConfig = plugin.sync {
        despawnNow(config.id)
        requireLoadedWorld("Standalone", config, logger, Bukkit::getWorld) ?: return@sync config

        val tracked = buildTrackedNpc(config)

        npcs[config.id] = tracked
        InteractableEntities.register(tracked.entity.entityId, config.id, realEntity = false)
        tracked.entity.spawn(toPacketLocation(config.entity.location))

        Bukkit.getOnlinePlayers().forEach { tryShow(tracked, it) }

        hitboxes.spawn(config)?.let { hitboxId -> npcs[config.id] = tracked.copy(hitboxId = hitboxId) }

        config
    }

    override fun despawn(config: NpcConfig) {
        plugin.sync { despawnNow(config.id) }
    }

    private fun despawnNow(id: String) {
        val tracked = npcs.remove(id) ?: return
        val viewers = tracked.entity.viewers.mapNotNull(Bukkit::getPlayer)
        removeEntity(tracked)

        val teamName = GlowTeamPackets.hiddenNameTagTeamFor(tracked.glowColor)
        viewers.forEach { player ->
            GlowTeamPackets.send(player, GlowTeamPackets.removeEntry(teamName, tracked.entity.username))
        }
        lookTicker.forgetEntity(tracked.entity.entityId)

        tracked.hitboxId?.let(hitboxes::remove)
    }

    private fun removeEntity(tracked: TrackedNpc) {
        InteractableEntities.unregister(tracked.entity.entityId)
        val viewers = tracked.entity.viewers.mapNotNull(Bukkit::getPlayer)
        tracked.entity.remove()
        viewers.forEach { removeProfile(tracked, it) }
    }

    private fun removeProfile(tracked: TrackedNpc, player: Player) {
        GlowTeamPackets.send(player, WrapperPlayServerPlayerInfoRemove(tracked.entity.uuid))
    }

    override fun refresh(config: NpcConfig) {
        spawn(config)
    }

    override fun teleport(config: NpcConfig) {
        plugin.sync {
            val tracked = npcs[config.id]
                ?.takeIf { it.world.equals(config.entity.location.world, ignoreCase = true) }
                ?: run {
                    spawn(config)
                    return@sync
                }

            tracked.entity.teleport(toPacketLocation(config.entity.location))
            tracked.hitboxId?.let { hitboxes.teleport(config, it) }
        }
    }

    override fun locationOf(config: NpcConfig): NpcLocation? {
        val tracked = npcs[config.id] ?: return null
        val entity = tracked.entity

        return NpcLocation(tracked.world, entity.x, entity.y, entity.z, entity.yaw, entity.pitch)
    }

    private fun buildTrackedNpc(config: NpcConfig): TrackedNpc {
        val texture = config.entity.skin.texture
        val signature = config.entity.skin.signature
        val textures = if (texture != null && signature != null) {
            listOf(TextureProperty("textures", texture, signature))
        } else {
            emptyList()
        }

        val profile = UserProfile(NpcProfiles.uuidFor(config), NpcProfiles.nameFor(config.id), textures)
        @Suppress("DEPRECATION")
        val entity = WrapperPlayer(profile, Bukkit.getUnsafe().nextEntityId())

        entity.isInTablist = false
        entity.getEntityMeta(PlayerMeta::class.java).apply {
            isGlowing = config.entity.glowing
            config.entity.customName?.let {
                customName = PacketComponents.of(MiniMessage.miniMessage().deserialize(it))
                isCustomNameVisible = true
            }
            SkinLayers.showAll(this, ServerCompat.version)
            pose = PacketPoses.of(config.entity.pose)
        }
        applyEquipment(entity, config)
        val scale = config.entity.effectiveScale()
        if (scale != 1.0) entity.attributes.setAttribute(Attributes.SCALE, scale)

        return TrackedNpc(
            entity = entity,
            world = config.entity.location.world,
            viewDistance = config.entity.viewDistance,
            lookAtPlayer = config.entity.lookAtPlayer,
            lookAtPlayerDistance = config.entity.lookAtPlayerDistance,
            glowColor = config.entity.glowColor,
        )
    }

    private fun applyEquipment(entity: WrapperPlayer, config: NpcConfig) {
        config.entity.equipment.filled().forEach { (slot, item) ->
            val stack = EquipmentItems.toItemStack(item) ?: return@forEach
            entity.equipment.setItem(packetSlot(slot), SpigotConversionUtil.fromBukkitItemStack(stack))
        }
    }

    private fun packetSlot(slot: EquipmentSlot): PacketEquipmentSlot = when (slot) {
        EquipmentSlot.MAIN_HAND -> PacketEquipmentSlot.MAIN_HAND
        EquipmentSlot.OFF_HAND -> PacketEquipmentSlot.OFF_HAND
        EquipmentSlot.HELMET -> PacketEquipmentSlot.HELMET
        EquipmentSlot.CHESTPLATE -> PacketEquipmentSlot.CHEST_PLATE
        EquipmentSlot.LEGGINGS -> PacketEquipmentSlot.LEGGINGS
        EquipmentSlot.BOOTS -> PacketEquipmentSlot.BOOTS
    }

    private fun tryShow(tracked: TrackedNpc, player: Player) {
        if (!tracked.entity.hasViewer(player.uniqueId) && inRange(tracked, player)) showFor(tracked, player)
    }

    private fun tickVisibility() {
        val onlinePlayers = Bukkit.getOnlinePlayers().toList()

        npcs.forEach { (id, tracked) ->
            try {
                onlinePlayers.forEach { player ->
                    val withinRange = inRange(tracked, player)
                    val currentlyVisible = tracked.entity.hasViewer(player.uniqueId)

                    when {
                        withinRange && !currentlyVisible -> showFor(tracked, player)
                        !withinRange && currentlyVisible -> hideFor(tracked, player)
                    }
                }
            } catch (exception: Exception) {
                logger.log(Level.WARNING, "Failed to update visibility for NPC $id", exception)
            }
        }
    }

    private fun inRange(tracked: TrackedNpc, player: Player): Boolean {
        if (!player.world.name.equals(tracked.world, ignoreCase = true)) return false
        val entity = tracked.entity
        val location = player.location
        val dx = entity.x - location.x
        val dy = entity.y - location.y
        val dz = entity.z - location.z

        return dx * dx + dy * dy + dz * dz <= tracked.viewDistance * tracked.viewDistance
    }

    private fun showFor(tracked: TrackedNpc, player: Player) {
        val teamName = GlowTeamPackets.hiddenNameTagTeamFor(tracked.glowColor)
        teams.ensure(player, teamName, GlowColors.resolve(tracked.glowColor), hideNameTag = true)
        tracked.entity.addViewer(player.uniqueId)
        GlowTeamPackets.send(player, GlowTeamPackets.addEntry(teamName, tracked.entity.username))
    }

    private fun hideFor(tracked: TrackedNpc, player: Player) {
        tracked.entity.removeViewer(player.uniqueId)
        removeProfile(tracked, player)

        GlowTeamPackets.send(
            player,
            GlowTeamPackets.removeEntry(
                GlowTeamPackets.hiddenNameTagTeamFor(tracked.glowColor),
                tracked.entity.username,
            ),
        )

        lookTicker.forgetPair(tracked.entity.entityId, player.uniqueId)
    }

    private fun lookTargets(): List<LookAtPlayerDriver.Target> = npcs.mapNotNull { (id, tracked) ->
        if (!tracked.lookAtPlayer) return@mapNotNull null
        val entity = tracked.entity

        LookAtPlayerDriver.Target(
            npcId = id,
            entityId = entity.entityId,
            x = entity.x,
            y = entity.y,
            z = entity.z,
            viewers = entity.viewers.mapNotNull(Bukkit::getPlayer),
            maxDistance = tracked.lookAtPlayerDistance,
        )
    }

    private fun toPacketLocation(location: NpcLocation): PacketLocation =
        PacketLocation(location.x, location.y, location.z, location.yaw, location.pitch)

    private data class TrackedNpc(
        val entity: WrapperPlayer,
        val world: String,
        val viewDistance: Double,
        val lookAtPlayer: Boolean,
        val lookAtPlayerDistance: Double,
        val glowColor: String? = null,
        val hitboxId: UUID? = null,
    )

    private companion object {
        private const val VISIBILITY_TICK_PERIOD = 10L
        private val logger = NpcLog.logger
    }
}
