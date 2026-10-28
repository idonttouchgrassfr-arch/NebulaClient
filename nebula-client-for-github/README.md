# Nebula Client (Fabric 1.21)

Original utility client: animated ClickGUI, 9 HUD widgets, 44 modules, chat commands, saved config.

## Build
1. Install JDK 21 and Gradle (or copy `gradlew` + `gradle/wrapper` from the Fabric example mod).
2. `gradle build`  ->  `build/libs/nebula-client-1.0.0.jar`
3. Drop the jar plus Fabric API 0.102.0+1.21 into `.minecraft/mods` (Fabric Loader 1.21).
   If Gradle rejects a version, take current ones from https://fabricmc.net/develop/ (gradle.properties).

## Use
- **Right Shift**: ClickGUI. Left-click toggles, right-click expands settings, click "Bind" then press a key.
- Chat commands: `.help`, `.toggle <mod>`, `.bind <mod> <key|none>`, `.set <mod> <setting> <value>`,
  `.list`, `.info <mod>`, `.panic`, `.save`
- Config: `.minecraft/config/nebula.properties`

## Modules
Combat: KillAura, TriggerBot, Aimbot, AutoClicker, AutoTotem, Criticals, Velocity, AutoLog
Movement: Sprint, Flight, Speed, Step, NoFall, Spider, WaterFloat, HighJump, AirJump, Glide, AutoJump, Parkour, ElytraBoost, Sneak, AutoWalk
Render: Fullbright, ESP, Zoom, FovChanger, TimeChanger, NoWeather
Player: AutoRespawn, AutoTool, AutoEat, AntiAFK, ChestStealer, Scaffold, Nuker, FastPlace, NoBreakDelay
Misc: Spin, Derp, Twerk, Swinger, ChatSuffix, Spammer
HUD: Watermark, ArrayList, Coordinates, Info, Radar, Keystrokes, ArmorHud, TargetHud, Notifications

## If it fails to compile (not compiled by the author)
Only 3 mixins exist; everything else is plain Fabric API. Most likely trouble spots:
- `ClientPlayNetworkHandlerMixin` (`packet.getEntityId()` / method name `onEntityVelocityUpdate`)
- the two `@Accessor` field names (`itemUseCooldown`, `blockBreakingCooldown`)
- `ClientWorld#setTimeOfDay` in TimeChanger
Delete the affected module/mixin entry from `nebula.mixins.json` and the module file to get unblocked.

Use on singleplayer or servers that allow it. Most public servers ban these features.
