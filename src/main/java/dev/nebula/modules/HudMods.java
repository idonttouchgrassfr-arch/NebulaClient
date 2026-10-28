package dev.nebula.modules;

import dev.nebula.core.*;

public final class HudMods {
    private HudMods() { }

    private static void hud(Mod m, boolean on) { ModManager.register(m).silentSet(on); }

    public static void init() {
        hud(new Mod("Watermark", "Client name badge", Category.HUD) { }, true);

        hud(new Mod("ArrayList", "List of enabled modules", Category.HUD) {
            final Setting.Bool background = add(new Setting.Bool("Background", true));
            final Setting.Mode color = add(new Setting.Mode("Color", "Gradient", "Rainbow", "Category"));
        }, true);

        hud(new Mod("Coordinates", "Position, facing and Nether coordinates", Category.HUD) { }, true);
        hud(new Mod("Info", "FPS, ping and speed", Category.HUD) { }, true);

        hud(new Mod("Radar", "2D radar of nearby entities", Category.HUD) {
            final Setting.Num size = add(new Setting.Num("Size", 70, 40, 120, 10));
            final Setting.Num range = add(new Setting.Num("Range", 40, 10, 128, 5));
            final Setting.Bool players = add(new Setting.Bool("Players", true));
            final Setting.Bool mobs = add(new Setting.Bool("Mobs", true));
            final Setting.Bool animals = add(new Setting.Bool("Animals", false));
            final Setting.Bool items = add(new Setting.Bool("Items", false));
        }, true);

        hud(new Mod("Keystrokes", "WASD and jump overlay", Category.HUD) { }, false);
        hud(new Mod("ArmorHud", "Armor durability next to the hotbar", Category.HUD) { }, true);
        hud(new Mod("TargetHud", "KillAura target health", Category.HUD) { }, true);
        hud(new Mod("Notifications", "Module toggle notifications", Category.HUD) { }, true);
    }
}
