package dev.nebula.util;

import dev.nebula.core.ModManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class Util {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final int ACCENT_A = 0x8A5CFF, ACCENT_B = 0x3DDCFF;

    private Util() { }

    // ---------- entities / combat ----------
    public static List<LivingEntity> targets(double range, boolean players, boolean mobs, boolean animals) {
        List<LivingEntity> out = new ArrayList<>();
        if (mc.world == null || mc.player == null) return out;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive()
                    || le instanceof ArmorStandEntity || le.isSpectator()) continue;
            if (mc.player.distanceTo(e) > range) continue;
            boolean ok = (e instanceof PlayerEntity && players)
                    || (e instanceof Monster && mobs)
                    || (e instanceof AnimalEntity && animals);
            if (ok) out.add(le);
        }
        return out;
    }

    public static double health(LivingEntity e) { return e.getHealth() + e.getAbsorptionAmount(); }

    public static float[] rotsTo(Vec3d t) {
        Vec3d eye = mc.player.getEyePos();
        double dx = t.x - eye.x, dy = t.y - eye.y, dz = t.z - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, h));
        return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
    }

    public static void attack(Entity e) {
        var p = mc.player;
        if (ModManager.isOn("Criticals") && p.isOnGround() && !p.isTouchingWater() && !p.isClimbing()) {
            double x = p.getX(), y = p.getY(), z = p.getZ();
            p.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.0625, z, false));
            p.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
        }
        mc.interactionManager.attackEntity(p, e);
        p.swingHand(Hand.MAIN_HAND);
    }

    // ---------- movement ----------
    public static boolean moving() {
        return mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
    }

    /** Sets horizontal velocity in the direction the player is pressing; keeps Y. Zero if not moving. */
    public static void setSpeed(double speed) {
        var p = mc.player;
        double f = p.input.movementForward, s = p.input.movementSideways;
        Vec3d v = p.getVelocity();
        if (f == 0 && s == 0) { p.setVelocity(0, v.y, 0); return; }
        double len = Math.sqrt(f * f + s * s);
        f /= len; s /= len;
        double rad = Math.toRadians(p.getYaw());
        double sin = Math.sin(rad), cos = Math.cos(rad);
        p.setVelocity((-sin * f + cos * s) * speed, v.y, (cos * f + sin * s) * speed);
    }

    // ---------- inventory ----------
    public static int findHotbar(Predicate<ItemStack> pred) {
        for (int i = 0; i < 9; i++) if (pred.test(mc.player.getInventory().getStack(i))) return i;
        return -1;
    }

    public static int findInv(Predicate<ItemStack> pred) {
        for (int i = 0; i < 36; i++) if (pred.test(mc.player.getInventory().getStack(i))) return i;
        return -1;
    }

    /** Inventory index (0-35) -> slot id in the player screen handler. */
    public static int slotId(int invIndex) { return invIndex < 9 ? invIndex + 36 : invIndex; }

    public static void select(int hotbar) { mc.player.getInventory().selectedSlot = hotbar; }

    // ---------- colours ----------
    public static int accent(double offset) {
        double t = (Math.sin(System.currentTimeMillis() / 700.0 + offset * 4.0) + 1.0) / 2.0;
        return lerp(0xFF000000 | ACCENT_A, 0xFF000000 | ACCENT_B, (float) t);
    }

    public static int rainbow(double offset) {
        float h = (float) (((System.currentTimeMillis() / 20.0) / 360.0 + offset) % 1.0);
        return 0xFF000000 | MathHelper.hsvToRgb(h, 0.6f, 1f);
    }

    public static int lerp(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    public static int alpha(int color, int a) { return (a << 24) | (color & 0xFFFFFF); }

    public static int darken(int c, float f) {
        return 0xFF000000 | ((int) ((c >> 16 & 255) * f) << 16) | ((int) ((c >> 8 & 255) * f) << 8) | (int) ((c & 255) * f);
    }

    public static String keyName(int k) {
        if (k == -1) return "None";
        try { return InputUtil.fromKeyCode(k, 0).getLocalizedText().getString(); }
        catch (Exception e) { return "Key " + k; }
    }
}
