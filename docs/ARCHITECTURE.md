# Architecture

## Modules

| Module | Package | Contents |
|---|---|---|
| `core` | `app.simplecloud.npc.core` | Config model, migrations, YAML repositories, SimpleCloud access, hologram text, renderer interfaces. No Bukkit. |
| `platform/common` | `app.simplecloud.npc.common` | Commands, managers, editors, inventories. No Bukkit. |
| `platform/paper` | `app.simplecloud.npc.paper` | The Paper plugin. Builds the jar. |
| `api` | `app.simplecloud.npc.api` | Public developer API: Bukkit events and the `SimpleCloudNpcs` service. |
| `provider/base` | `app.simplecloud.npc.bukkit` | Shared Bukkit code for providers: click packets, glow teams, skins, look-at-player. |
| `provider/standalone` | `app.simplecloud.npc.provider.standalone` | Packet NPCs (PacketEvents/EntityLib). |
| `provider/mannequin` | `app.simplecloud.npc.provider.mannequin` | Mannequin entities, 1.21.9 and newer. |
| `provider/citizens`, `fancynpcs`, `znpcsplus`, `mythicmobs` | `app.simplecloud.npc.provider.<name>` | Wrappers for the other NPC plugins. |
| `libs/packetevents` | | PacketEvents and EntityLib, relocated, with their own Adventure. No code. |

`core` and `platform/common` don't know Bukkit. They talk to the server through
interfaces in `platform/common/.../platform` and `core/.../platform`, which
`platform/paper` implements.

## Build

- The API floor is paper-api 1.20.6. `platform/paper` and `provider/base` compile
  against it, so using newer API fails the build. `provider/mannequin` is the
  only exception (Paper 1.21.11). It is loaded by reflection when
  `ServerCompat.hasMannequin` is true, and `ReflectiveSeamsTest` checks its class name.
- Our code uses the server's Adventure. The compile classpath is pinned to the
  floor version (4.17.0).
- PacketEvents and EntityLib can't use the server's Adventure: they break below
  4.25 and on Adventure 5. `libs/packetevents` ships them with Adventure 4.26.1,
  relocated to `app.simplecloud.npc.relocate.packets.kyori`. Components going into
  a packet are converted through `PacketComponents` (JSON).
- The plugin is one jar, `simplecloud-npc.jar`, built from `platform/paper`. All
  shaded libraries are relocated under `app.simplecloud.npc.relocate`, because the
  SimpleCloud API plugin brings its own Kotlin. The SimpleCloud API itself comes
  from that plugin (`depend: simplecloud-api`).
- `PluginJarTest` checks the finished jar: allowed contents, that every
  referenced class exists, and that it links against the Adventure of 1.20.6,
  1.21.8 and 26.x.
- The root `build.gradle.kts` holds the shared Kotlin setup and the version
  (`baseVersion`). With `COMMIT_HASH` set, the version becomes
  `<baseVersion>-dev.<hash>` and is published to Modrinth as a beta.

## Startup

`PaperPlugin.onEnable` runs these steps in order:

1. `bootstrap/PacketStack` starts PacketEvents and EntityLib, `bootstrap/Listeners`
   registers the listeners.
2. `provider/ProviderWiring.load` loads the providers from `ProviderCatalog` and
   skips plugins that aren't installed. `RoutingNpcRenderer` picks the provider
   per NPC. New NPCs always use our own provider (Mannequin if available,
   otherwise standalone). Other plugins' NPCs only come in through `link`.
3. `editor/EditorWiring.create` builds the NPC and inventory editors.
4. `NpcPluginContext` creates the managers, the interaction executor and the
   inventory views, and starts watching the config folders.
5. Effects, API hooks, the PlaceholderAPI hook and the update checker are added.
6. `bootstrap/ProviderReadyWait` waits until all provider plugins have loaded
   their NPCs. Then `NpcManager.reconcileOnBoot` spawns or re-attaches every NPC.

## Providers

A provider implements `NpcRenderer` from `core`. Wrappers for other plugins
extend `LinkedProviderRenderer`, which tracks whether an NPC is ours or linked
and runs every call on the main thread.

Holograms don't go through providers. Every NPC gets a packet-only TextDisplay
hologram from `PacketHologramRenderer` in `provider/base`. Each line is sent per
player, so `<playername>` and PlaceholderAPI text go straight into that player's
packets (`ViewerTexts`). Holograms show and hide with the NPC's `view-distance`
through `PacketViewerTracker`. `LegacyHologramCleanup` removes the persisted
TextDisplays older versions left in the worlds.

Clicks and glow colors work through packets:

- `NpcInteractPacketListener` reads click packets and maps them to NPCs through
  `InteractableEntities`. `ClickPackets` knows the format: up to 1.21.11 one
  interact packet, from 26.1 a separate attack packet. The packet is cancelled,
  so game mode, invulnerability or other plugins can't block the click.
- `GlowTeams` sends scoreboard teams per player, so glow colors also work when
  another plugin gives the player its own scoreboard.

## Configs

- `NpcConfig` (`core/config`) is the format of `npcs/<id>.yml`.
  `NpcConfigMigration` upgrades old files on load and keeps a backup. The old
  formats are in `migration/legacy`.
- Value ranges and `ActionFields` live next to the config classes, so commands
  and editor check the same limits.
- `InventoryConfiguration` (`core/inventory`) is the format of the menus.
- `YamlConfigurator` reads and writes the files. Missing keys keep the default
  of the data class.
- `WatchableYamlDirectoryRepository` watches a folder and reports changes made
  outside the plugin. It ignores its own writes.

## Editors

Both editors are in `platform/common/.../editor`.

- `editor/menu`: `EditorMenu` is a menu made of `Element`s (icon and click
  handler). `Pane` builds one, `Paginator` splits lists into pages.
- `editor/core`: shared parts. `ScreenStackEditor` keeps a stack of screens per
  player, back and Escape go one screen back. Also pickers, confirm dialogs and
  the shared action editor (`ActionFieldsPane`), which NPC clicks and menu items
  both use.
- `editor/npc`: the NPC editor. Each screen is a `*MenuBuilder` in
  `editor/npc/<feature>/`. Changes go through `NpcEditorContext.commit`, which
  saves, refreshes the NPC and redraws the menu.
- `editor/inventory`: the menu editor. Changes go through
  `InventoryEditorContext.change`, with undo/redo (`EditHistory`, 50 steps).
- `editor/inventory/canvas`: the canvas for laying out menus. On Paper,
  `PacketCanvasSurface` handles it at packet level: the player's inventory shows
  the toolbox and the cursor shows the carried item, but no editor item ever
  ends up in a real inventory.

Text input always happens in chat (`ChatInputPrompts`, `TextPrompts`).
