package dev.bearclient;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ModuleManager {
    public static final List<Module> ALL = new ArrayList<>();
    public static Module rainbow;
    public static Module safeMode;

    private static final Set<String> pending = new HashSet<>();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("bearclient.properties");
    private static boolean applied;
    private static int ticks;

    private ModuleManager() {}

    public static void init() {
        Modules.registerAll();
        load();
    }

    /** True when "Server-Safe Mode" is on: radar / minimap / world map are hidden. */
    public static boolean safe() { return safeMode != null && safeMode.isEnabled(); }

    public static List<Module> in(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : ALL) if (m.category == c) out.add(m);
        return out;
    }

    public static void tick(Minecraft mc) {
        if (!applied) { // enable saved modules once the options are available
            applied = true;
            for (Module m : ALL) if (pending.contains(m.name)) m.setEnabled(true);
        }
        ticks++;
        boolean refresh = ticks % 4 == 0 && mc.player != null && mc.level != null;
        for (Module m : ALL) {
            if (!m.isEnabled()) continue;
            try {
                m.onTick(mc);
                if (refresh) m.lines = m.buildLines(mc);
            } catch (RuntimeException e) {
                m.lines = List.of();
            }
        }
    }

    public static void shutdown(Minecraft mc) {
        save();
        for (Module m : ALL) if (m.isEnabled() && m.category == Category.PERFORMANCE) m.setEnabled(false);
        mc.options.save(); // persist the player's original settings, not our tweaks
    }

    private static void load() {
        if (!Files.exists(FILE)) {
            pending.addAll(List.of("Watermark", "FPS Counter", "Coordinates", "Armor HUD", "Keystrokes", "Minimap"));
            return;
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) { p.load(in); } catch (IOException e) { return; }
        for (Module m : ALL) {
            if ("true".equals(p.getProperty(m.key() + ".enabled"))) pending.add(m.name);
            String c = p.getProperty(m.key() + ".corner");
            if (c != null) try { m.corner = Corner.valueOf(c); } catch (IllegalArgumentException ignored) {}
        }
    }

    public static void save() {
        Properties p = new Properties();
        for (Module m : ALL) {
            p.setProperty(m.key() + ".enabled", String.valueOf(m.isEnabled()));
            p.setProperty(m.key() + ".corner", m.corner.name());
        }
        try (OutputStream out = Files.newOutputStream(FILE)) { p.store(out, "Bear Client"); } catch (IOException ignored) {}
    }
}
