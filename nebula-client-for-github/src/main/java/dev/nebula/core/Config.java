package dev.nebula.core;

import dev.nebula.NebulaClient;
import dev.nebula.ui.ClickGui;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Config {
    private Config() { }

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("nebula.properties"); }
    private static String sk(Setting<?> s) { return s.name.replace(' ', '_'); }

    public static void save() {
        Properties p = new Properties();
        for (Mod m : ModManager.all()) {
            p.setProperty("mod." + m.name + ".key", String.valueOf(m.key));
            if (m.cat == Category.HUD) p.setProperty("mod." + m.name + ".enabled", String.valueOf(m.isEnabled()));
            for (Setting<?> s : m.settings) p.setProperty("mod." + m.name + ".s." + sk(s), s.serialize());
        }
        ClickGui.savePositions(p);
        try (Writer w = Files.newBufferedWriter(file())) {
            p.store(w, "Nebula Client config");
        } catch (IOException e) {
            NebulaClient.LOG.error("Could not save config", e);
        }
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(f)) {
            p.load(r);
        } catch (IOException e) {
            NebulaClient.LOG.error("Could not load config", e);
            return;
        }
        for (Mod m : ModManager.all()) {
            String k = p.getProperty("mod." + m.name + ".key");
            if (k != null) try { m.key = Integer.parseInt(k.trim()); } catch (NumberFormatException ignored) { }
            if (m.cat == Category.HUD) {
                String en = p.getProperty("mod." + m.name + ".enabled");
                if (en != null) m.silentSet(Boolean.parseBoolean(en.trim()));
            }
            for (Setting<?> s : m.settings) {
                String v = p.getProperty("mod." + m.name + ".s." + sk(s));
                if (v != null) s.deserialize(v);
            }
        }
        ClickGui.loadPositions(p);
    }
}
