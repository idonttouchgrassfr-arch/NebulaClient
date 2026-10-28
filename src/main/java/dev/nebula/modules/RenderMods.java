package dev.nebula.modules;

import dev.nebula.core.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;

import static dev.nebula.core.Mod.mc;

public final class RenderMods {
    private RenderMods() { }

    public static void init() {
        ModManager.register(new Mod("Fullbright", "See in the dark", Category.RENDER) {
            @Override public void onTick() {
                var p = mc.player;
                if (!p.hasStatusEffect(StatusEffects.NIGHT_VISION))
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, StatusEffectInstance.INFINITE, 0, false, false, false));
            }
            @Override public void onDisable() {
                var p = mc.player;
                if (p == null) return;
                StatusEffectInstance e = p.getStatusEffect(StatusEffects.NIGHT_VISION);
                if (e != null && e.isInfinite()) p.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        });

        ModManager.register(new Mod("ESP", "Highlights entities through walls", Category.RENDER) {
            final Setting.Bool players = add(new Setting.Bool("Players", true));
            final Setting.Bool mobs = add(new Setting.Bool("Mobs", true));
            final Setting.Bool animals = add(new Setting.Bool("Animals", false));
            final Setting.Bool items = add(new Setting.Bool("Items", false));
            final Set<Integer> glowing = new HashSet<>();

            @Override public void onTick() {
                for (Entity e : mc.world.getEntities()) {
                    if (e == mc.player) continue;
                    boolean want = (e instanceof PlayerEntity && players.get())
                            || (e instanceof Monster && mobs.get())
                            || (e instanceof AnimalEntity && animals.get())
                            || (e instanceof ItemEntity && items.get());
                    if (want) { e.setGlowing(true); glowing.add(e.getId()); }
                    else if (glowing.remove(e.getId())) e.setGlowing(false);
                }
            }
            @Override public void onDisable() {
                if (mc.world != null) {
                    for (Entity e : mc.world.getEntities()) if (glowing.contains(e.getId())) e.setGlowing(false);
                }
                glowing.clear();
            }
        });

        ModManager.register(new Mod("Zoom", "Zoom in", Category.RENDER) {
            final Setting.Num level = add(new Setting.Num("Level", 4, 1.5, 10, 0.5));
            int saved = 70;
            @Override public void onEnable() { saved = mc.options.getFov().getValue(); }
            @Override public void onTick() {
                int target = (int) Math.max(30, Math.round(saved / level.get()));
                if (mc.options.getFov().getValue() != target) mc.options.getFov().setValue(target);
            }
            @Override public void onDisable() { mc.options.getFov().setValue(saved); }
        });

        ModManager.register(new Mod("FovChanger", "Set a custom field of view", Category.RENDER) {
            final Setting.Num fov = add(new Setting.Num("FOV", 100, 30, 110, 1));
            int saved = 70;
            @Override public void onEnable() { saved = mc.options.getFov().getValue(); }
            @Override public void onTick() { if (mc.options.getFov().getValue() != fov.i()) mc.options.getFov().setValue(fov.i()); }
            @Override public void onDisable() { mc.options.getFov().setValue(saved); }
        });

        ModManager.register(new Mod("TimeChanger", "Client-side time of day", Category.RENDER) {
            final Setting.Mode mode = add(new Setting.Mode("Time", "Day", "Noon", "Sunset", "Night", "Midnight", "Custom"));
            final Setting.Num custom = add(new Setting.Num("Custom", 6000, 0, 24000, 250));
            @Override public void onTick() {
                long t = switch (mode.get()) {
                    case "Day" -> 1000L;
                    case "Noon" -> 6000L;
                    case "Sunset" -> 12500L;
                    case "Night" -> 14000L;
                    case "Midnight" -> 18000L;
                    default -> custom.i();
                };
                mc.world.setTimeOfDay(t);
            }
            @Override public String info() { return mode.get(); }
        });

        ModManager.register(new Mod("NoWeather", "Removes rain and thunder visuals", Category.RENDER) {
            @Override public void onTick() {
                mc.world.setRainGradient(0f);
                mc.world.setThunderGradient(0f);
            }
        });
    }
}
