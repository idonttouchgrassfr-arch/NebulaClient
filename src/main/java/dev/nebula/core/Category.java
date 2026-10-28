package dev.nebula.core;

public enum Category {
    COMBAT("Combat", 0xFFFF5C7A),
    MOVEMENT("Movement", 0xFF5CFFB0),
    RENDER("Render", 0xFF5CC8FF),
    PLAYER("Player", 0xFFFFD25C),
    MISC("Misc", 0xFFB45CFF),
    HUD("HUD", 0xFFFF8C5C);

    public final String label;
    public final int color;

    Category(String label, int color) {
        this.label = label;
        this.color = color;
    }
}
