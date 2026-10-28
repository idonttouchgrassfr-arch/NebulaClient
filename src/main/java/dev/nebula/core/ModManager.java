package dev.nebula.core;

import java.util.*;

public final class ModManager {
    private static final List<Mod> MODS = new ArrayList<>();
    private static final Map<String, Mod> BY_NAME = new HashMap<>();

    private ModManager() { }

    public static <M extends Mod> M register(M m) {
        MODS.add(m);
        BY_NAME.put(m.name.toLowerCase(Locale.ROOT), m);
        return m;
    }

    public static List<Mod> all() { return MODS; }

    public static List<Mod> in(Category c) {
        List<Mod> out = new ArrayList<>();
        for (Mod m : MODS) if (m.cat == c) out.add(m);
        out.sort(Comparator.comparing(m -> m.name));
        return out;
    }

    public static Mod get(String name) { return BY_NAME.get(name.toLowerCase(Locale.ROOT)); }

    public static boolean isOn(String name) {
        Mod m = get(name);
        return m != null && m.isEnabled();
    }

    public static void panic() {
        for (Mod m : MODS) if (m.cat != Category.HUD && m.isEnabled()) m.setEnabled(false);
    }
}
