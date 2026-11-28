# Bear Client (Fabric, Minecraft 26.3)

A client-only Fabric mod: HUD with Performance / Debug / Customization / Overlay modules,
a minimap + full-screen world map, and a custom bear-in-the-forest title screen.
Client-only means it works on vanilla and Fabric servers without anything installed server-side.

## Requirements
- Minecraft **26.3** (Java Edition), Fabric Loader **0.19.5+**, Fabric API **0.166.2+26.3** (or newer for 26.3)
- JDK **25** to build

## Controls
| Key | Action |
|---|---|
| Right Shift | Bear Client module menu (rebindable in Controls) |
| M | World map: drag = pan, scroll = zoom, C = recenter |
| Menu: left click | toggle a module |
| Menu: right click | move a HUD module to another screen corner |

Settings are saved in `config/bearclient.properties`.

## Server rules
Some survival servers ban minimaps, radars or similar mods. Turn on **Server-Safe Mode**
(Customization column) to hide the Radar, Minimap and World Map. Check your server's rules.

## Build
```
gradle wrapper --gradle-version 9.6.1   # once, to create ./gradlew
./gradlew build                          # jar in build/libs/
./gradlew runClient                      # dev client
```
Or just push to GitHub: the included Actions workflow builds the jar on every push.

## If the build fails after a Minecraft update
All version-sensitive GUI calls are in `G.java`; module option names are in `Modules.java`.
To look up real names: extract the deobfuscated jar from
`~/.gradle/caches/fabric-loom/` and run `javap -p` on the class in question.
