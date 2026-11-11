# My Client (Fabric, Minecraft 26.2)

Client-side HUD mod: FPS, coordinates, facing direction.

Keys (rebindable in Controls > My Client):
- Right Shift: toggle HUD
- Right Ctrl: toggle coordinates

## Build
1. Install JDK 25 (26.x requires Java 25).
2. Clone the Fabric example mod, branch 26.2:
   https://github.com/FabricMC/fabric-example-mod/tree/26.2
   Copy its `gradlew`, `gradlew.bat` and `gradle/` folder into this project
   (or run `gradle wrapper` if you have Gradle installed).
3. Run `./gradlew build` (`gradlew.bat build` on Windows).
4. The mod is `build/libs/myclient-1.0.0.jar`. Put it in `.minecraft/mods`
   with Fabric Loader and Fabric API for 26.2.

Test in dev with `./gradlew runClient`.

Versions in gradle.properties match the official 26.2 example mod. Check
https://fabricmc.net/develop for newer loader/API versions.

## No Java/Gradle installed? Build on GitHub
Upload this folder to a new GitHub repo. Go to the Actions tab, open the
latest "build" run, and download the `myclient-jar` artifact. Unzip it to get
the jar. (The workflow installs Java 25 and Gradle for you, so no gradlew needed.)
