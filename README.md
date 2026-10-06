# NPC Plugin

![Banner][banner]

<div align="center">

  [![Modrinth][badge-modrinth]][modrinth]
  [![License][badge-license]][license]
  <br>

  [![Discord][badge-discord]][social-discord]
  [![Follow @simplecloudapp][badge-x]][social-x]
  [![Follow @simplecloudapp][badge-bluesky]][social-bluesky]
  [![Follow @simplecloudapp][badge-youtube]][social-youtube]
  <br>

  [Report a Bug][issue-bug-report]
  ·
  [Request a Feature][issue-feature-request]
  <br>

🌟 Give us a star — your support means the world to us!
</div>
<br>

> All information about this project can be found in our detailed [documentation][docs-thisproject].

Create NPCs on your server that allow your players to access other subservers through the NPCs. Perform various actions such as sending messages, titles, or sounds. NPCs are rendered natively, so no third-party NPC plugin is needed, but Citizens, FancyNpcs, ZNPCsPlus and MythicMobs NPCs can still be used.

## Features

- [x] **Custom Join NPC**: Create NPCs to let your players join sub-servers seamlessly!  
- [x] **Use it as a Standalone**: No third-party plugins needed—just drag and drop!  
- [x] **In-Game Editor**: Change everything about an NPC without touching a file!  
- [x] **Custom Inventories**: Build server selectors on a drag-and-drop canvas with live server lists!  
- [x] **Holograms**: Add beautiful holograms above your NPCs, with rotating lines!  
- [x] **Placeholders**: Use placeholders anywhere—in holograms, items, and more!  
- [x] **Customize with Actions**: Trigger messages, action bars, titles, sounds, and more when a player interacts with an NPC!  
- [x] **Knows When Servers Are Offline**: Holograms and click actions switch on their own.  
- [x] **Lively NPCs**: Equipment, poses, sizes, click animations and speech bubbles.  
- [x] **Support for Other NPC Plugins**: Attach NPCs from Citizens, FancyNpcs, ZNPCsPlus or MythicMobs!  

## Supported servers

Paper (and its forks) from **1.20.6 up to the latest release**, on **Java 21** or newer.
The SimpleCloud API plugin is required; use its build for your server version.
From 1.21.9 on, NPCs are native Mannequin entities.

## Quick start

Stand where the NPC should appear, and run:

```text
/scnpcs create <id> <group-or-persistent-server>
```

For example, `/scnpcs create lobby Lobby` spawns an NPC wearing your skin, links it
to the SimpleCloud target, adds a default hologram, and makes right-click join the
target. The editor opens right away; later, `/scnpcs edit` while looking at the NPC
brings it back.

Existing NPCs from other plugins can be attached with
`/scnpcs link <id> <provider> <reference> <target>`. They keep belonging to their
plugin, SimpleCloud adds the click handling. A hologram can be turned on in the editor.

Menus are built with `/scnpcs inventory create <id>` and opened from an NPC or with
`/scnpcs inventory open <id>`. Run `/scnpcs help` for all commands.

## Upgrading

Drop the new jar over the old one. NPC files are migrated on the first start, and a
backup of every old file is kept next to it.

## Placeholders

Run `/scnpcs placeholders` in-game for the full list. Holograms and menus understand
the placeholders of the NPC's target:

```text
<target_name>
<target_online_players>
<target_max_players>
<target_property:key>
```

Holograms and action texts also understand `<playername>` and `<playeruuid>`, and with
PlaceholderAPI installed `%placeholders%`. Holograms render them for each player.

## Contributing
Contributions to SimpleCloud are welcome and highly appreciated. However, before you jump right into it, we would like you to read our [Contribution Guide][docs-contribute].
For this repository, [`docs/ARCHITECTURE.md`](https://github.com/simplecloudapp/npc-plugin/blob/main/docs/ARCHITECTURE.md) explains the modules.

## License
This repository is licensed under [Apache 2.0][license].


<!-- LINK GROUP -->

<!-- ✅ PLEASE EDIT -->
[banner]: https://raw.githubusercontent.com/simplecloudapp/branding/refs/heads/main/readme/banner/plugin/npcs.png
[issue-bug-report]: https://github.com/simplecloudapp/npc-plugin/issues/new?labels=bug&projects=template=01_BUG-REPORT.yml&title=%5BBUG%5D+%3Ctitle%3E
[issue-feature-request]: https://github.com/simplecloudapp/npc-plugin/discussions/new?category=ideas
[docs-thisproject]: https://docs.simplecloud.app/en/manual/plugin/npcs
[docs-contribute]: https://docs.simplecloud.app/contribute

[modrinth]: https://modrinth.com/plugin/npcs-plugin

<!-- ⛔ DON'T TOUCH -->
[license]: https://opensource.org/licenses/Apache-2.0

[social-x]: https://x.com/simplecloudapp
[social-bluesky]: https://bsky.app/profile/simplecloud.app
[social-youtube]: https://www.youtube.com/@thesimplecloud9075
[social-discord]: https://discord.simplecloud.app

[badge-modrinth]: https://img.shields.io/badge/modrinth-18181b.svg?style=flat-square&logo=modrinth
[badge-license]: https://img.shields.io/badge/apache%202.0-blue.svg?style=flat-square&label=license&labelColor=18181b&style=flat-square&color=e11d48
[badge-discord]: https://img.shields.io/badge/Community_Discord-d95652.svg?style=flat-square&logo=discord&color=27272a
[badge-x]: https://img.shields.io/badge/Follow_@simplecloudapp-d95652.svg?style=flat-square&logo=x&color=27272a
[badge-bluesky]: https://img.shields.io/badge/Follow_@simplecloud.app-d95652.svg?style=flat-square&logo=bluesky&color=27272a
[badge-youtube]: https://img.shields.io/badge/youtube-d95652.svg?style=flat-square&logo=youtube&color=27272a
