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

Create NPCs on your server that allow your players to access other subservers through the NPCs. Perform various actions such as sending messages, titles, or sounds, and use your favourite supported NPC provider such as Citizens or FancyNPCs.

## Features

- [x] **Custom Join NPC**: Create NPCs to let your players join sub-servers seamlessly!  
- [x] **Custom Inventories**: Design the paginated inventory exactly the way you need it!  
- [x] **Holograms**: Add beautiful holograms above your NPCs!  
- [x] **Placeholders**: Use placeholders anywhere—in holograms, items, and more!  
- [x] **Customize with Actions**: Trigger messages, action bars, titles, sounds, and more when a player interacts with an NPC!  
- [x] **Support for Other NPC Plugins**: Integrate NPCs from other plugins like Citizens, FancyNPCs, and more!  
- [ ] **Use it as a Standalone**: No third-party plugins needed—just drag and drop!  

## Quick start

Install Citizens, FancyNPCs, or ZNPCsPlus, stand where the NPC should appear, and run:

```text
/scnpcs create <id> <group-or-persistent-server> [provider]
```

For example, `/scnpcs create lobby Lobby` creates the provider NPC, links it to the
SimpleCloud target, writes its configuration, adds a default hologram, and makes
right-click join the target. If multiple creation providers are installed, add
`citizens`, `fancynpcs`, or `znpcsplus` to the command.

Target names do not need `group:` or `ps:` prefixes. A name that exists as both a
group and persistent server is rejected so that it can never resolve silently to
the wrong target.

Use `/scnpcs edit <id>` to discover the target, hologram, action, and pushback
editors. Existing provider NPCs can be attached with
`/scnpcs link <id> <provider> <reference> <target>`.

## Hologram placeholders

Run `/scnpcs placeholders` in-game for the current list. The placeholders shared
by group and persistent-server targets are:

```text
<target_name>
<target_type>
<target_online_players>
<target_max_players>
<target_min_memory>
<target_max_memory>
<target_property:key>
```

Persistent servers also expose `<target_id>`, `<target_pretty_name>`, and
`<target_motd>`. Join-state values in NPC files are always written in lower case.

## Contributing
Contributions to SimpleCloud are welcome and highly appreciated. However, before you jump right into it, we would like you to read our [Contribution Guide][docs-contribute].

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
[maven-central]: https://central.sonatype.com/artifact/app.simplecloud.controller/controller-api
[dev]: https://repo.simplecloud.app/#/snapshots/app/simplecloud/controller/controller-api


[artifacts]: https://repo.simplecloud.app/#/snapshots/app/simplecloud/controller/controller-api
[dev-artifacts]: https://repo.simplecloud.app/#/snapshots/app/simplecloud/controller/controller-api

[badge-maven-central]: https://img.shields.io/maven-central/v/app.simplecloud.controller/controller-api?labelColor=18181b&style=flat-square&color=65a30d&label=Release
[badge-dev]: https://repo.simplecloud.app/api/badge/latest/snapshots/app/simplecloud/controller/controller-api?name=Dev&style=flat-square&color=0ea5e9

<!-- ⛔ DON'T TOUCH -->
[license]: https://opensource.org/licenses/Apache-2.0
[snapshots]: https://repo.simplecloud.app/#/snapshots

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
