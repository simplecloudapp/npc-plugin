package app.simplecloud.npc.common.editor.inventory

object CommonMaterials {

    private val COLORS = listOf(
        "WHITE", "LIGHT_GRAY", "GRAY", "BLACK", "BROWN", "RED", "ORANGE", "YELLOW",
        "LIME", "GREEN", "CYAN", "LIGHT_BLUE", "BLUE", "PURPLE", "MAGENTA", "PINK",
    )

    val DYES: List<String> = COLORS.map { "${it}_DYE" }
    val PANES: List<String> = COLORS.map { "${it}_STAINED_GLASS_PANE" }
    val HEADS: List<String> = listOf("PLAYER_HEAD")

    val REDSTONE: List<String> = listOf(
        "REDSTONE", "REDSTONE_TORCH", "REDSTONE_BLOCK", "COMPARATOR", "REPEATER", "LEVER",
        "TRIPWIRE_HOOK", "BARRIER", "STRUCTURE_VOID", "TNT", "GUNPOWDER", "OBSERVER", "PISTON",
    )

    val TOOLS: List<String> = listOf(
        "BOW", "CROSSBOW", "TRIDENT", "SHIELD", "ELYTRA", "ARROW", "SPECTRAL_ARROW",
        "DIAMOND_SWORD", "NETHERITE_SWORD", "IRON_SWORD", "GOLDEN_SWORD", "WOODEN_SWORD",
        "DIAMOND_PICKAXE", "IRON_PICKAXE", "DIAMOND_AXE", "IRON_AXE", "FISHING_ROD", "FLINT_AND_STEEL",
    )

    val FOOD: List<String> = listOf(
        "GOLDEN_APPLE", "ENCHANTED_GOLDEN_APPLE", "APPLE", "BREAD", "COOKED_BEEF", "CAKE",
        "POTION", "SPLASH_POTION",
    )

    val BLOCKS: List<String> = listOf(
        "COMPASS", "CLOCK", "NAME_TAG", "PAPER", "MAP", "BOOK", "WRITABLE_BOOK", "ENCHANTED_BOOK",
        "NETHER_STAR", "ENDER_PEARL", "ENDER_EYE", "EMERALD", "DIAMOND", "GOLD_INGOT", "IRON_INGOT",
        "NETHERITE_INGOT", "AMETHYST_SHARD", "EXPERIENCE_BOTTLE", "TOTEM_OF_UNDYING", "BEACON",
        "CHEST", "ENDER_CHEST", "BARREL", "ITEM_FRAME", "OAK_SIGN", "BELL", "LANTERN", "SOUL_LANTERN",
        "TORCH", "DRAGON_BREATH", "BLAZE_POWDER", "GHAST_TEAR", "SLIME_BALL", "MAGMA_CREAM",
        "FIREWORK_ROCKET", "FIREWORK_STAR", "SNOWBALL", "BUCKET", "WATER_BUCKET", "LAVA_BUCKET",
        "GRASS_BLOCK", "STONE", "COBBLESTONE", "OAK_PLANKS", "GLOWSTONE", "SEA_LANTERN", "OBSIDIAN",
        "CRYING_OBSIDIAN", "BEDROCK", "END_STONE", "NETHERRACK", "SPONGE", "BOOKSHELF",
        "CRAFTING_TABLE", "FURNACE", "ANVIL", "GRINDSTONE", "LECTERN", "GOLD_BLOCK", "IRON_BLOCK",
        "DIAMOND_BLOCK", "EMERALD_BLOCK", "NETHERITE_BLOCK",
    ) + listOf("WOOL", "CONCRETE", "TERRACOTTA").flatMap { suffix -> COLORS.map { "${it}_$suffix" } }

    val CATEGORIES: List<Pair<String, List<String>>> = listOf(
        "Blocks" to BLOCKS,
        "Tools" to TOOLS,
        "Food" to FOOD,
        "Dyes" to DYES,
        "Panes" to PANES,
        "Heads" to HEADS,
        "Redstone" to REDSTONE,
    )

    val COMMON: List<String> = (BLOCKS + TOOLS + FOOD + DYES + PANES + HEADS + REDSTONE).distinct()
}
