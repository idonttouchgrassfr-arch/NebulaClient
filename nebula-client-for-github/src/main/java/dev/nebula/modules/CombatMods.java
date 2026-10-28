package dev.nebula.modules;

import dev.nebula.core.*;
import dev.nebula.util.Util;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.hit.EntityHitResult;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.MathHelper;

import java.util.Comparator;
import java.util.List;

import static dev.nebula.core.Mod.mc;

public final class CombatMods {
    /** Current KillAura target (read by the TargetHud). */
    public static volatile LivingEntity target;

    private CombatMods() { }

    public static void init() {
        ModManager.register(new Mod("KillAura", "Automatically attacks entities around you", Category.COMBAT) {
            final Setting.Num range = add(new Setting.Num("Range", 3.6, 1, 6, 0.1));
            final Setting.Num cps = add(new Setting.Num("CPS", 12, 1, 20, 1));
            final Setting.Mode sort = add(new Setting.Mode("Priority", "Distance", "Health"));
            final Setting.Bool players = add(new Setting.Bool("Players", true));
            final Setting.Bool mobs = add(new Setting.Bool("Mobs", true));
            final Setting.Bool animals = add(new Setting.Bool("Animals", false));
            final Setting.Bool rotate = add(new Setting.Bool("Rotate", true));
            final Setting.Bool cooldown = add(new Setting.Bool("Cooldown", true));
            long last;

            @Override public void onTick() {
                if (mc.currentScreen != null) { target = null; return; }
                List<LivingEntity> list = Util.targets(range.get(), players.get(), mobs.get(), animals.get());
                if (list.isEmpty()) { target = null; return; }
                if (sort.is("Health")) list.sort(Comparator.<LivingEntity>comparingDouble(Util::health));
                else list.sort(Comparator.<LivingEntity>comparingDouble(e -> mc.player.distanceTo(e)));
                LivingEntity t = list.get(0);
                target = t;
                if (rotate.get()) {
                    float[] r = Util.rotsTo(t.getPos().add(0, t.getHeight() * 0.6, 0));
                    mc.player.setYaw(r[0]);
                    mc.player.setPitch(r[1]);
                }
                long now = System.currentTimeMillis();
                boolean ready = cooldown.get() ? mc.player.getAttackCooldownProgress(0.5f) >= 0.95f
                                               : now - last >= (long) (1000.0 / cps.get());
                if (ready) { Util.attack(t); last = now; }
            }
            @Override public void onDisable() { target = null; }
            @Override public String info() { return sort.get(); }
        });

        ModManager.register(new Mod("TriggerBot", "Attacks whatever your crosshair is on", Category.COMBAT) {
            final Setting.Bool players = add(new Setting.Bool("Players", true));
            final Setting.Bool mobs = add(new Setting.Bool("Mobs", true));
            final Setting.Bool animals = add(new Setting.Bool("Animals", false));

            @Override public void onTick() {
                if (mc.currentScreen != null) return;
                if (!(mc.crosshairTarget instanceof EntityHitResult r) || !(r.getEntity() instanceof LivingEntity le) || !le.isAlive()) return;
                if (mc.player.getAttackCooldownProgress(0.5f) < 0.95f) return;
                boolean ok = le instanceof PlayerEntity ? players.get()
                        : le instanceof net.minecraft.entity.mob.Monster ? mobs.get()
                        : le instanceof net.minecraft.entity.passive.AnimalEntity && animals.get();
                if (ok) Util.attack(le);
            }
        });

        ModManager.register(new Mod("Aimbot", "Smoothly aims at nearby targets", Category.COMBAT) {
            final Setting.Num range = add(new Setting.Num("Range", 5, 1, 8, 0.5));
            final Setting.Num fov = add(new Setting.Num("FOV", 90, 10, 360, 10));
            final Setting.Num smooth = add(new Setting.Num("Smooth", 6, 1, 20, 1));
            final Setting.Bool players = add(new Setting.Bool("Players", true));
            final Setting.Bool mobs = add(new Setting.Bool("Mobs", true));
            final Setting.Bool onClick = add(new Setting.Bool("OnlyOnClick", false));

            @Override public void onTick() {
                if (mc.currentScreen != null) return;
                if (onClick.get() && !mc.options.attackKey.isPressed()) return;
                var p = mc.player;
                LivingEntity best = null;
                float bestD = Float.MAX_VALUE, bestDy = 0, bestDp = 0;
                for (LivingEntity e : Util.targets(range.get(), players.get(), mobs.get(), false)) {
                    float[] r = Util.rotsTo(e.getPos().add(0, e.getHeight() * 0.75, 0));
                    float dy = MathHelper.wrapDegrees(r[0] - p.getYaw());
                    float dp = r[1] - p.getPitch();
                    if (Math.abs(dy) > fov.get() / 2.0) continue;
                    float d = (float) Math.hypot(dy, dp);
                    if (d < bestD) { bestD = d; best = e; bestDy = dy; bestDp = dp; }
                }
                if (best == null) return;
                p.setYaw(p.getYaw() + bestDy / smooth.f());
                p.setPitch(MathHelper.clamp(p.getPitch() + bestDp / smooth.f(), -90f, 90f));
            }
        });

        ModManager.register(new Mod("AutoClicker", "Clicks for you while attack is held", Category.COMBAT) {
            final Setting.Num cps = add(new Setting.Num("CPS", 10, 1, 20, 1));
            long last;

            @Override public void onTick() {
                if (mc.currentScreen != null || !mc.options.attackKey.isPressed()) return;
                long now = System.currentTimeMillis();
                if (now - last >= (long) (1000.0 / cps.get())) {
                    KeyBinding.onKeyPressed(InputUtil.fromTranslationKey(mc.options.attackKey.getBoundKeyTranslationKey()));
                    last = now;
                }
            }
        });

        ModManager.register(new Mod("AutoTotem", "Keeps a Totem of Undying in your offhand", Category.COMBAT) {
            final Setting.Num health = add(new Setting.Num("Health", 20, 1, 20, 1));
            final Setting.Num delay = add(new Setting.Num("Delay", 2, 0, 10, 1));
            int timer;

            @Override public void onTick() {
                var p = mc.player;
                if (timer > 0) { timer--; return; }
                if (p.currentScreenHandler != p.playerScreenHandler) return;
                if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
                if (Util.health(p) > health.get() && !p.getOffHandStack().isEmpty()) return;
                int idx = Util.findInv(s -> s.isOf(Items.TOTEM_OF_UNDYING));
                if (idx == -1) return;
                mc.interactionManager.clickSlot(0, Util.slotId(idx), 40, SlotActionType.SWAP, p);
                timer = delay.i();
            }
        });

        ModManager.register(new Mod("Criticals", "Sends mini-jump packets before attacks (KillAura/TriggerBot)", Category.COMBAT) { });

        ModManager.register(new Mod("Velocity", "Ignores knockback", Category.COMBAT) { });

        ModManager.register(new Mod("AutoLog", "Leaves the server when your health is low", Category.COMBAT) {
            final Setting.Num health = add(new Setting.Num("Health", 6, 1, 19, 1));

            @Override public void onTick() {
                if (Util.health(mc.player) <= health.get()) {
                    setEnabled(false);
                    mc.world.disconnect();
                }
            }
        });
    }
}
