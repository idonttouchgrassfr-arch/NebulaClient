package dev.nebula.core;

import dev.nebula.NebulaClient;
import dev.nebula.ui.Hud;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public abstract class Mod {
    public static final MinecraftClient mc = MinecraftClient.getInstance();

    public final String name, desc;
    public final Category cat;
    public final List<Setting<?>> settings = new ArrayList<>();
    public int key = -1;
    public boolean expanded;     // GUI state
    public boolean wasDown;      // keybind edge detection
    public float anim;           // arraylist animation
    private boolean enabled;

    protected Mod(String name, String desc, Category cat) {
        this.name = name; this.desc = desc; this.cat = cat;
    }

    protected <S extends Setting<?>> S add(S s) { settings.add(s); return s; }

    @SuppressWarnings("unchecked")
    public <S extends Setting<?>> S setting(String n) {
        for (Setting<?> s : settings) if (s.name.equalsIgnoreCase(n)) return (S) s;
        throw new IllegalArgumentException("No setting " + n + " on " + name);
    }

    public boolean isEnabled() { return enabled; }
    public void toggle() { setEnabled(!enabled); }

    /** Sets state without calling hooks (used for HUD defaults and config load). */
    public void silentSet(boolean v) { enabled = v; }

    public void setEnabled(boolean v) {
        if (v == enabled) return;
        enabled = v;
        try {
            if (v) onEnable(); else onDisable();
        } catch (Throwable t) {
            NebulaClient.LOG.error("Module {} failed in enable/disable", name, t);
        }
        if (cat != Category.HUD) Hud.notify(name + (v ? " enabled" : " disabled"), v);
    }

    public final void tickSafe() {
        try {
            onTick();
        } catch (Throwable t) {
            NebulaClient.LOG.error("Module {} crashed, disabling", name, t);
            enabled = false;
            try { onDisable(); } catch (Throwable ignored) { }
            Hud.notify(name + " crashed (" + t.getClass().getSimpleName() + ")", false);
        }
    }

    public void onEnable() { }
    public void onDisable() { }
    public void onTick() { }
    public String info() { return null; }
}
