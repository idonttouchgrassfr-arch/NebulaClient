package dev.nebula.core;

import dev.nebula.util.Util;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Chat commands, prefix "." (e.g. .toggle KillAura). */
public final class Commands {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private Commands() { }

    public static void reply(String s) {
        if (mc.inGameHud == null) return;
        mc.inGameHud.getChatHud().addMessage(Text.literal("[Nebula] ").formatted(Formatting.LIGHT_PURPLE)
                .append(Text.literal(s).formatted(Formatting.GRAY)));
    }

    /** @return true if the message was a Nebula command and must not be sent to the server. */
    public static boolean handle(String raw) {
        if (!raw.startsWith(".") || raw.startsWith("..") || raw.length() < 2) return false;
        mc.inGameHud.getChatHud().addToMessageHistory(raw);
        String[] a = raw.substring(1).trim().split("\\s+");
        switch (a[0].toLowerCase()) {
            case "help", "h" -> {
                reply(".toggle <mod>  |  .bind <mod> <key|none>  |  .set <mod> <setting> <value>");
                reply(".list [category]  |  .panic  |  .save  |  .info <mod>  |  .<mod> toggles a module");
            }
            case "toggle", "t" -> {
                Mod m = a.length > 1 ? ModManager.get(a[1]) : null;
                if (m == null) reply("Unknown module."); else { m.toggle(); reply(m.name + " is " + (m.isEnabled() ? "ON" : "OFF")); }
            }
            case "bind", "b" -> {
                Mod m = a.length > 2 ? ModManager.get(a[1]) : null;
                if (m == null) { reply("Usage: .bind <mod> <key|none>"); break; }
                if (a[2].equalsIgnoreCase("none")) { m.key = -1; reply("Unbound " + m.name); }
                else try {
                    m.key = InputUtil.fromTranslationKey("key.keyboard." + a[2].toLowerCase()).getCode();
                    reply(m.name + " bound to " + Util.keyName(m.key));
                } catch (IllegalArgumentException e) { reply("Unknown key '" + a[2] + "'"); }
                Config.save();
            }
            case "set" -> {
                Mod m = a.length > 3 ? ModManager.get(a[1]) : null;
                if (m == null) { reply("Usage: .set <mod> <setting> <value>"); break; }
                String val = String.join(" ", Arrays.copyOfRange(a, 3, a.length));
                boolean ok = false;
                for (Setting<?> s : m.settings) {
                    if (s.name.equalsIgnoreCase(a[2])) { s.deserialize(val); reply(m.name + "." + s.name + " = " + s.display()); ok = true; }
                }
                if (!ok) reply("No such setting.");
                Config.save();
            }
            case "info" -> {
                Mod m = a.length > 1 ? ModManager.get(a[1]) : null;
                if (m == null) { reply("Unknown module."); break; }
                reply(m.name + " - " + m.desc);
                for (Setting<?> s : m.settings) reply("  " + s.name + " = " + s.display());
            }
            case "list", "l" -> {
                for (Category c : Category.values()) {
                    if (a.length > 1 && !c.label.equalsIgnoreCase(a[1])) continue;
                    reply(c.label + ": " + ModManager.in(c).stream().map(m -> m.name).collect(Collectors.joining(", ")));
                }
            }
            case "panic" -> { ModManager.panic(); reply("All modules disabled."); }
            case "save" -> { Config.save(); reply("Config saved."); }
            default -> {
                Mod m = ModManager.get(a[0]);
                if (m != null) { m.toggle(); reply(m.name + " is " + (m.isEnabled() ? "ON" : "OFF")); }
                else reply("Unknown command. Try .help");
            }
        }
        return true;
    }
}
