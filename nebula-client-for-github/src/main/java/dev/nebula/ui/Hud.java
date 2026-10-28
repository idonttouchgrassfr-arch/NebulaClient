package dev.nebula.ui;

import dev.nebula.NebulaClient;
import dev.nebula.core.Category;
import dev.nebula.core.Mod;
import dev.nebula.core.ModManager;
import dev.nebula.core.Setting;
import dev.nebula.modules.CombatMods;
import dev.nebula.util.Util;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Hud {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final int WHITE = 0xFFFFFFFF, GREY = 0xFFA0A0B8;

    private record Note(String text, boolean good, long time) { }
    private static final List<Note> NOTES = new ArrayList<>();
    private static double bps, lastX, lastZ;
    private static boolean hasLast;

    private Hud() { }

    // ------------------------------------------------------------ api
    public static void notify(String text, boolean good) {
        if (!ModManager.isOn("Notifications")) return;
        NOTES.add(new Note(text, good, System.currentTimeMillis()));
        while (NOTES.size() > 6) NOTES.remove(0);
    }

    public static void tick() {
        var p = mc.player;
        if (p == null) { hasLast = false; return; }
        if (hasLast) {
            double cur = Math.hypot(p.getX() - lastX, p.getZ() - lastZ) * 20.0;
            bps += (cur - bps) * 0.2;
        }
        lastX = p.getX(); lastZ = p.getZ(); hasLast = true;
    }

    private static boolean on(String n) { return ModManager.isOn(n); }
    private static TextRenderer tr() { return mc.textRenderer; }

    public static void gradientText(DrawContext ctx, String s, int x, int y, double offset) {
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            ctx.drawText(tr(), ch, cx, y, Util.accent(offset + i * 0.09), true);
            cx += tr().getWidth(ch);
        }
    }

    private static void hLine(DrawContext ctx, int x, int y, int w, int h, double base) {
        for (int i = 0; i < w; i += 2) ctx.fill(x + i, y, Math.min(x + i + 2, x + w), y + h, Util.accent(base + i * 0.012));
    }

    // ------------------------------------------------------------ main
    public static void render(DrawContext ctx) {
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        if (on("Watermark")) watermark(ctx);
        if (on("Radar")) radar(ctx);
        if (on("ArrayList")) arrayList(ctx);
        if (on("Coordinates")) coords(ctx);
        if (on("Info")) info(ctx);
        if (on("Keystrokes")) keystrokes(ctx);
        if (on("ArmorHud")) armor(ctx);
        if (on("TargetHud")) targetHud(ctx);
        notes(ctx);
    }

    // ------------------------------------------------------------ pieces
    private static void watermark(DrawContext ctx) {
        String name = NebulaClient.NAME;
        String rest = " " + NebulaClient.VERSION;
        int w = tr().getWidth(name) + tr().getWidth(rest) + 12;
        ctx.fill(4, 4, 4 + w, 18, 0xB00C0C18);
        hLine(ctx, 4, 4, w, 1, 0);
        gradientText(ctx, name, 10, 8, 0);
        ctx.drawText(tr(), rest, 10 + tr().getWidth(name), 8, GREY, true);
    }

    private static void arrayList(DrawContext ctx) {
        Mod al = ModManager.get("ArrayList");
        boolean bg = ((Setting.Bool) al.setting("Background")).get();
        Setting.Mode colorMode = al.setting("Color");

        List<Mod> list = new ArrayList<>();
        for (Mod m : ModManager.all()) {
            if (m.isEnabled() && m.cat != Category.HUD) { m.anim += (1f - m.anim) * 0.2f; list.add(m); }
            else m.anim = 0f;
        }
        list.sort((a, b) -> Integer.compare(width(b), width(a)));

        int sw = ctx.getScaledWindowWidth();
        int y = 2;
        for (int i = 0; i < list.size(); i++) {
            Mod m = list.get(i);
            int w = width(m);
            int slide = (int) ((1f - m.anim) * (w + 10));
            int x = sw - w - 6 + slide;
            int col = switch (colorMode.get()) {
                case "Rainbow" -> Util.rainbow(i * 0.04);
                case "Category" -> m.cat.color;
                default -> Util.accent(i * 0.12);
            };
            if (bg) ctx.fill(x - 2, y, sw + slide, y + 11, 0x960A0A14);
            ctx.fill(sw - 2 + slide, y, sw + slide, y + 11, col);
            ctx.drawText(tr(), m.name, x, y + 2, col, true);
            String info = m.info();
            if (info != null) ctx.drawText(tr(), info, x + tr().getWidth(m.name) + 4, y + 2, GREY, true);
            y += 11;
        }
    }

    private static int width(Mod m) {
        String info = m.info();
        return tr().getWidth(m.name) + (info == null ? 0 : tr().getWidth(info) + 4);
    }

    private static void coords(DrawContext ctx) {
        var p = mc.player;
        int sh = ctx.getScaledWindowHeight();
        String facing = p.getHorizontalFacing().asString();
        facing = Character.toUpperCase(facing.charAt(0)) + facing.substring(1);
        String l1 = String.format(Locale.ROOT, "XYZ  %.1f  %.1f  %.1f", p.getX(), p.getY(), p.getZ());
        String l2 = facing;
        if (mc.world.getRegistryKey() == World.OVERWORLD)
            l2 += String.format(Locale.ROOT, "   Nether  %.1f  %.1f", p.getX() / 8.0, p.getZ() / 8.0);
        else if (mc.world.getRegistryKey() == World.NETHER)
            l2 += String.format(Locale.ROOT, "   Overworld  %.1f  %.1f", p.getX() * 8.0, p.getZ() * 8.0);
        int w = Math.max(tr().getWidth(l1), tr().getWidth(l2)) + 10;
        ctx.fill(4, sh - 26, 4 + w, sh - 4, 0xB00C0C18);
        ctx.fill(4, sh - 26, 6, sh - 4, Util.accent(0.3));
        ctx.drawText(tr(), l1, 10, sh - 23, WHITE, true);
        ctx.drawText(tr(), l2, 10, sh - 13, GREY, true);
    }

    private static void info(DrawContext ctx) {
        int sh = ctx.getScaledWindowHeight();
        int ping = 0;
        if (mc.getNetworkHandler() != null) {
            var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = e.getLatency();
        }
        String[] lines = {
                "FPS  " + mc.getCurrentFps(),
                "Ping  " + ping + "ms",
                String.format(Locale.ROOT, "BPS  %.2f", bps)
        };
        int y = sh - 30 - lines.length * 11;
        int w = 0;
        for (String s : lines) w = Math.max(w, tr().getWidth(s));
        ctx.fill(4, y - 2, 4 + w + 10, y + lines.length * 11, 0xB00C0C18);
        ctx.fill(4, y - 2, 6, y + lines.length * 11, Util.accent(0.6));
        for (String s : lines) { ctx.drawText(tr(), s, 10, y, WHITE, true); y += 11; }
    }

    private static void keystrokes(DrawContext ctx) {
        int sh = ctx.getScaledWindowHeight();
        int bx = 8, by = sh / 2 - 20, s = 20;
        key(ctx, "W", bx + s + 2, by, s, mc.options.forwardKey.isPressed());
        key(ctx, "A", bx, by + s + 2, s, mc.options.leftKey.isPressed());
        key(ctx, "S", bx + s + 2, by + s + 2, s, mc.options.backKey.isPressed());
        key(ctx, "D", bx + 2 * s + 4, by + s + 2, s, mc.options.rightKey.isPressed());
        int wide = s * 3 + 4;
        int jy = by + 2 * s + 4;
        boolean j = mc.options.jumpKey.isPressed();
        ctx.fill(bx, jy, bx + wide, jy + 10, j ? Util.alpha(Util.accent(0.2), 0xC0) : 0x900C0C18);
        ctx.fill(bx + wide / 2 - 8, jy + 4, bx + wide / 2 + 8, jy + 6, WHITE);
    }

    private static void key(DrawContext ctx, String k, int x, int y, int s, boolean down) {
        ctx.fill(x, y, x + s, y + s, down ? Util.alpha(Util.accent(x * 0.01), 0xC0) : 0x900C0C18);
        ctx.drawText(tr(), k, x + (s - tr().getWidth(k)) / 2, y + (s - 8) / 2, WHITE, true);
    }

    private static void armor(DrawContext ctx) {
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int x = sw / 2 + 98, y = sh - 20;
        for (int i = 3; i >= 0; i--) {
            ItemStack st = mc.player.getInventory().armor.get(i);
            if (st.isEmpty()) continue;
            ctx.drawItem(st, x, y);
            ctx.drawItemInSlot(tr(), st, x, y);
            x += 18;
        }
    }

    private static void targetHud(DrawContext ctx) {
        LivingEntity t = CombatMods.target;
        if (t == null || !t.isAlive() || !on("KillAura")) return;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int x = sw / 2 + 24, y = sh / 2 + 24, w = 120, h = 34;
        ctx.fill(x, y, x + w, y + h, 0xCC0C0C18);
        hLine(ctx, x, y, w, 1, 0.2);
        String name = t.getName().getString();
        ctx.drawText(tr(), name, x + 6, y + 6, WHITE, true);
        float hp = t.getHealth(), max = Math.max(1f, t.getMaxHealth());
        float ratio = Math.max(0f, Math.min(1f, hp / max));
        int barW = w - 12;
        ctx.fill(x + 6, y + 20, x + 6 + barW, y + 27, 0xFF1A1A28);
        int fill = (int) (barW * ratio);
        int col = Util.lerp(0xFFFF4060, 0xFF40FF90, ratio);
        ctx.fill(x + 6, y + 20, x + 6 + fill, y + 27, col);
        String hs = String.format(Locale.ROOT, "%.1f", hp);
        ctx.drawText(tr(), hs, x + w - 6 - tr().getWidth(hs), y + 6, GREY, true);
    }

    private static void radar(DrawContext ctx) {
        Mod r = ModManager.get("Radar");
        Setting.Num size = r.setting("Size"), range = r.setting("Range");
        Setting.Bool pl = r.setting("Players"), mo = r.setting("Mobs"), an = r.setting("Animals"), it = r.setting("Items");
        int s = size.i(), x = 4, y = 24, cx = x + s / 2, cy = y + s / 2;
        double rng = range.get();

        ctx.fill(x - 1, y - 1, x + s + 1, y + s + 1, Util.alpha(Util.accent(0.4), 0xFF));
        ctx.fill(x, y, x + s, y + s, 0xD0080812);
        ctx.fill(cx, y, cx + 1, y + s, 0x30FFFFFF);
        ctx.fill(x, cy, x + s, cy + 1, 0x30FFFFFF);

        double rad = Math.toRadians(mc.player.getYaw());
        double sin = Math.sin(rad), cos = Math.cos(rad);
        double scale = (s / 2.0 - 2) / rng;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) continue;
            int col;
            if (e instanceof PlayerEntity && pl.get()) col = 0xFFFF5C7A;
            else if (e instanceof Monster && mo.get()) col = 0xFFFFA03C;
            else if (e instanceof AnimalEntity && an.get()) col = 0xFF5CFF8C;
            else if (e instanceof ItemEntity && it.get()) col = 0xFF5CC8FF;
            else continue;
            double dx = e.getX() - mc.player.getX(), dz = e.getZ() - mc.player.getZ();
            // right = (-cos, -sin), forward = (-sin, cos)
            double sx = dx * -cos + dz * -sin;
            double up = dx * -sin + dz * cos;
            int px = cx + (int) Math.round(sx * scale);
            int py = cy - (int) Math.round(up * scale);
            if (px < x + 1 || px > x + s - 3 || py < y + 1 || py > y + s - 3) continue;
            ctx.fill(px, py, px + 2, py + 2, col);
        }
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, WHITE);
    }

    private static void notes(DrawContext ctx) {
        long now = System.currentTimeMillis();
        NOTES.removeIf(n -> now - n.time() > 3000);
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int y = sh - 50;
        for (int i = NOTES.size() - 1; i >= 0; i--) {
            Note n = NOTES.get(i);
            long age = now - n.time();
            float in = Math.min(1f, age / 250f);
            float p = (1f - (float) Math.pow(1f - in, 3));
            if (age > 2700) p -= (age - 2700) / 300f;
            p = Math.max(0f, p);
            int w = tr().getWidth(n.text()) + 18;
            int x = sw - (int) ((w + 6) * p);
            ctx.fill(x, y, x + w, y + 17, 0xD00C0C18);
            ctx.fill(x, y, x + 2, y + 17, n.good() ? 0xFF5CFF90 : 0xFFFF5C7A);
            ctx.drawText(tr(), n.text(), x + 8, y + 5, WHITE, true);
            y -= 21;
        }
    }
}
