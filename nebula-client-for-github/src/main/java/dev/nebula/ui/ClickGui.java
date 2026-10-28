package dev.nebula.ui;

import dev.nebula.NebulaClient;
import dev.nebula.core.*;
import dev.nebula.util.Util;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.IntConsumer;

public class ClickGui extends Screen {
    private static final int PW = 122, HH = 18, RH = 14;
    private static final int WHITE = 0xFFFFFFFF, GREY = 0xFFB4B4C8;

    private static final class PState { int x, y; boolean open = true; }
    private static final Map<Category, PState> STATE = new EnumMap<>(Category.class);
    private static boolean placed;
    static { for (Category c : Category.values()) STATE.put(c, new PState()); }

    private record Hit(int x1, int y1, int x2, int y2, IntConsumer act) {
        boolean in(double x, double y) { return x >= x1 && x < x2 && y >= y1 && y < y2; }
    }

    private final List<Hit> hits = new ArrayList<>();
    private PState dragPanel;
    private int dragOX, dragOY;
    private Setting.Num dragNum;
    private int dragX1, dragX2;
    private Mod listening;
    private String hover;
    private int curMx, curMy;
    private final long opened = System.currentTimeMillis();

    public ClickGui() { super(Text.literal("Nebula")); }

    // ---- persistence of panel positions ----
    public static void savePositions(Properties p) {
        if (!placed) return;
        for (var e : STATE.entrySet()) {
            p.setProperty("gui." + e.getKey().name() + ".x", String.valueOf(e.getValue().x));
            p.setProperty("gui." + e.getKey().name() + ".y", String.valueOf(e.getValue().y));
            p.setProperty("gui." + e.getKey().name() + ".open", String.valueOf(e.getValue().open));
        }
    }

    public static void loadPositions(Properties p) {
        boolean any = false;
        for (var e : STATE.entrySet()) {
            String x = p.getProperty("gui." + e.getKey().name() + ".x");
            String y = p.getProperty("gui." + e.getKey().name() + ".y");
            if (x == null || y == null) continue;
            try {
                e.getValue().x = Integer.parseInt(x.trim());
                e.getValue().y = Integer.parseInt(y.trim());
                any = true;
            } catch (NumberFormatException ignored) { }
            String o = p.getProperty("gui." + e.getKey().name() + ".open");
            if (o != null) e.getValue().open = Boolean.parseBoolean(o.trim());
        }
        if (any) placed = true;
    }

    @Override
    protected void init() {
        if (!placed) {
            int n = Category.values().length;
            int total = n * (PW + 8) - 8;
            int x = Math.max(6, (width - total) / 2);
            for (Category c : Category.values()) {
                PState s = STATE.get(c);
                s.x = x; s.y = 28;
                x += PW + 8;
            }
            placed = true;
        }
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext ctx, int mx, int my, float delta) { /* custom background */ }

    @Override
    public void removed() { Config.save(); }

    // ---------------------------------------------------------------- render
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        hits.clear();
        hover = null;
        curMx = mx; curMy = my;
        long t = System.currentTimeMillis();
        float fade = Math.min(1f, (t - opened) / 220f);

        ctx.fillGradient(0, 0, width, height, Util.alpha(0x160A2C, (int) (0xCC * fade)), Util.alpha(0x04040E, (int) (0xE0 * fade)));
        stars(ctx, t);

        if (dragNum != null && dragX2 > dragX1) {
            double r = Math.max(0, Math.min(1, (mx - dragX1) / (double) (dragX2 - dragX1)));
            dragNum.set(dragNum.min + (dragNum.max - dragNum.min) * r);
        }
        if (dragPanel != null) {
            dragPanel.x = Math.max(0, Math.min(width - PW, mx - dragOX));
            dragPanel.y = Math.max(0, Math.min(height - HH, my - dragOY));
        }

        // title
        ctx.getMatrices().push();
        ctx.getMatrices().translate(width / 2f, 6, 0);
        ctx.getMatrices().scale(1.6f, 1.6f, 1f);
        String title = "NEBULA";
        int tw = textRenderer.getWidth(title);
        Hud.gradientText(ctx, title, -tw / 2, 0, 0);
        ctx.getMatrices().pop();

        for (Category c : Category.values()) panel(ctx, c, mx, my, fade);

        String foot = "Left click: toggle   Right click: expand   Drag headers to move   RShift / Esc: close";
        ctx.drawCenteredTextWithShadow(textRenderer, foot, width / 2, height - 12, 0xFF8888A8);

        if (hover != null) {
            int w = textRenderer.getWidth(hover) + 8;
            int x = Math.min(mx + 10, width - w - 2), y = Math.min(my + 12, height - 16);
            ctx.fill(x, y, x + w, y + 14, 0xF0101020);
            ctx.fill(x, y, x + 2, y + 14, Util.accent(0));
            ctx.drawText(textRenderer, hover, x + 5, y + 3, WHITE, true);
        }
    }

    private void stars(DrawContext ctx, long t) {
        for (int i = 0; i < 80; i++) {
            float speed = 0.006f + (i % 5) * 0.004f;
            int sx = (int) ((i * 7919L) % Math.max(1, width));
            int sy = (int) (((i * 104729L) % Math.max(1, height) + t * speed) % Math.max(1, height));
            int a = 40 + (i * 37) % 100;
            int s = i % 7 == 0 ? 2 : 1;
            ctx.fill(sx, sy, sx + s, sy + s, (a << 24) | (i % 3 == 0 ? 0xB8A8FF : 0xFFFFFF));
        }
    }

    private static boolean inRect(int mx, int my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx < x2 && my >= y1 && my < y2;
    }

    private void panel(DrawContext ctx, Category c, int mx, int my, float fade) {
        PState st = STATE.get(c);
        int x = st.x, y = st.y;
        List<Mod> mods = ModManager.in(c);

        int h = HH;
        if (st.open) {
            for (Mod m : mods) {
                h += RH;
                if (m.expanded) h += m.settings.size() * RH + RH;
            }
        }

        ctx.fill(x - 1, y - 1, x + PW + 1, y + h + 1, Util.alpha(c.color, 0x55));
        ctx.fill(x, y, x + PW, y + h, 0xE60B0B16);

        ctx.fillGradient(x, y, x + PW, y + HH, c.color, Util.darken(c.color, 0.45f));
        ctx.drawText(textRenderer, c.label, x + 7, y + 5, WHITE, true);
        String tag = st.open ? "-" : "+";
        ctx.drawText(textRenderer, tag, x + PW - 12, y + 5, WHITE, true);
        hits.add(new Hit(x, y, x + PW, y + HH, b -> {
            if (b == 0) { dragPanel = st; dragOX = curMx - st.x; dragOY = curMy - st.y; }
            else if (b == 1) st.open = !st.open;
        }));
        if (!st.open) return;

        int cy = y + HH;
        for (Mod m : mods) {
            boolean hov = inRect(mx, my, x, cy, x + PW, cy + RH);
            if (m.isEnabled()) {
                ctx.fillGradient(x, cy, x + PW, cy + RH, Util.alpha(c.color, 0xC0), Util.alpha(Util.darken(c.color, 0.5f), 0xC0));
            } else {
                ctx.fill(x, cy, x + PW, cy + RH, hov ? 0xFF22223A : 0xFF14141F);
            }
            ctx.drawText(textRenderer, m.name, x + 7, cy + 3, m.isEnabled() ? WHITE : GREY, true);
            if (!m.settings.isEmpty()) ctx.drawText(textRenderer, m.expanded ? "v" : ">", x + PW - 11, cy + 3, 0xFF8080A0, true);
            final int ry = cy;
            hits.add(new Hit(x, cy, x + PW, cy + RH, b -> {
                if (b == 0) m.toggle(); else if (b == 1) m.expanded = !m.expanded;
            }));
            if (hov) hover = m.desc;
            cy += RH;

            if (m.expanded) {
                for (Setting<?> s : m.settings) { settingRow(ctx, c, s, x, cy, mx, my); cy += RH; }
                // bind row
                ctx.fill(x, cy, x + PW, cy + RH, 0xFF0E0E18);
                String bt = "Bind: " + (listening == m ? "press a key..." : Util.keyName(m.key));
                ctx.drawText(textRenderer, bt, x + 11, cy + 3, 0xFFC8C8E0, true);
                hits.add(new Hit(x, cy, x + PW, cy + RH, b -> {
                    if (b == 0) listening = m; else if (b == 1) { m.key = -1; listening = null; }
                }));
                cy += RH;
            }
        }
    }

    private void settingRow(DrawContext ctx, Category c, Setting<?> s, int x, int y, int mx, int my) {
        ctx.fill(x, y, x + PW, y + RH, 0xFF0E0E18);
        ctx.fill(x + 4, y, x + 6, y + RH, Util.alpha(c.color, 0x90));
        if (s instanceof Setting.Bool b) {
            ctx.drawText(textRenderer, b.name, x + 11, y + 3, 0xFFC8C8E0, true);
            int bx = x + PW - 16;
            ctx.fill(bx, y + 3, bx + 9, y + 12, 0xFF000000);
            ctx.fill(bx + 1, y + 4, bx + 8, y + 11, b.get() ? Util.accent(y * 0.01) : 0xFF2A2A3C);
            hits.add(new Hit(x, y, x + PW, y + RH, btn -> b.toggle()));
        } else if (s instanceof Setting.Mode md) {
            ctx.drawText(textRenderer, md.name + ": ", x + 11, y + 3, 0xFFC8C8E0, true);
            int off = textRenderer.getWidth(md.name + ": ");
            ctx.drawText(textRenderer, md.display(), x + 11 + off, y + 3, Util.accent(y * 0.01), true);
            hits.add(new Hit(x, y, x + PW, y + RH, btn -> { if (btn == 0) md.next(); else if (btn == 1) md.prev(); }));
        } else if (s instanceof Setting.Num n) {
            int fillW = (int) ((PW - 8) * n.ratio());
            ctx.fill(x + 7, y + 1, x + 7 + fillW, y + RH - 1, Util.alpha(c.color, 0x70));
            ctx.drawText(textRenderer, n.name, x + 11, y + 3, 0xFFC8C8E0, true);
            String v = n.display();
            ctx.drawText(textRenderer, v, x + PW - 6 - textRenderer.getWidth(v), y + 3, WHITE, true);
            hits.add(new Hit(x, y, x + PW, y + RH, btn -> {
                if (btn == 0) {
                    dragNum = n; dragX1 = x + 7; dragX2 = x + PW - 1;
                    double r = Math.max(0, Math.min(1, (curMx - dragX1) / (double) (dragX2 - dragX1)));
                    n.set(n.min + (n.max - n.min) * r);
                }
            }));
        } else if (s instanceof Setting.Str str) {
            String v = str.display();
            if (v.length() > 11) v = v.substring(0, 10) + "..";
            ctx.drawText(textRenderer, str.name + ": " + v, x + 11, y + 3, 0xFFC8C8E0, true);
            if (inRect(mx, my, x, y, x + PW, y + RH)) hover = "Edit with .set <mod> " + str.name + " <text>";
        }
    }

    // ---------------------------------------------------------------- input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        curMx = (int) mx; curMy = (int) my;
        if (listening != null) { listening = null; }
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.in(mx, my)) { h.act().accept(button); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragPanel != null || dragNum != null) Config.save();
        dragPanel = null;
        dragNum = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening != null) {
            boolean clear = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE;
            if (keyCode != NebulaClient.guiKey) listening.key = clear ? -1 : keyCode;
            listening = null;
            Config.save();
            return true;
        }
        if (keyCode == NebulaClient.guiKey) {
            NebulaClient.guiWasDown = true;
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
