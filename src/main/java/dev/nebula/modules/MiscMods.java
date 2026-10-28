package dev.nebula.modules;

import dev.nebula.core.*;
import net.minecraft.client.option.KeyBinding;

import java.util.Random;

import static dev.nebula.core.Mod.mc;

public final class MiscMods {
    private MiscMods() { }

    public static void init() {
        ModManager.register(new Mod("Spin", "Spins your character", Category.MISC) {
            final Setting.Num speed = add(new Setting.Num("Speed", 30, 5, 90, 5));
            @Override public void onTick() { mc.player.setYaw(mc.player.getYaw() + speed.f()); }
        });

        ModManager.register(new Mod("Derp", "Random head movements", Category.MISC) {
            final Random rng = new Random();
            @Override public void onTick() {
                mc.player.setYaw(rng.nextFloat() * 360f - 180f);
                mc.player.setPitch(rng.nextFloat() * 180f - 90f);
            }
        });

        ModManager.register(new Mod("Twerk", "Rapidly toggles sneak", Category.MISC) {
            final Setting.Num rate = add(new Setting.Num("Ticks", 2, 1, 10, 1));
            int t;
            @Override public void onTick() {
                t++;
                mc.options.sneakKey.setPressed((t / rate.i()) % 2 == 0);
            }
            @Override public void onDisable() { KeyBinding.updatePressedStates(); }
        });

        ModManager.register(new Mod("Swinger", "Swings your arm repeatedly", Category.MISC) {
            final Setting.Num rate = add(new Setting.Num("Ticks", 4, 1, 20, 1));
            int t;
            @Override public void onTick() {
                if (++t % rate.i() == 0) mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            }
        });

        ModManager.register(new Mod("ChatSuffix", "Appends a suffix to your chat messages", Category.MISC) { });

        ModManager.register(new Mod("Spammer", "Sends a chat message repeatedly", Category.MISC) {
            final Setting.Str message = add(new Setting.Str("Message", "Nebula Client on top"));
            final Setting.Num delay = add(new Setting.Num("Seconds", 5, 1, 60, 1));
            final Setting.Bool antiSpam = add(new Setting.Bool("RandomTag", true));
            final Random rng = new Random();
            long last;
            @Override public void onEnable() { last = System.currentTimeMillis(); }
            @Override public void onTick() {
                long now = System.currentTimeMillis();
                if (now - last < delay.i() * 1000L) return;
                last = now;
                String m = message.get();
                if (m.isBlank()) return;
                if (antiSpam.get()) m += " [" + (100 + rng.nextInt(900)) + "]";
                mc.player.networkHandler.sendChatMessage(m);
            }
        });
    }
}
