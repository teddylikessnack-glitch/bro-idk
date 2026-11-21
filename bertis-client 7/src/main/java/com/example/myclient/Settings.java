package com.example.myclient;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

public final class Settings {
    // ---- HUD modules ----
    public static final String[] HUD_NAMES = {
            "FPS", "CPS", "Coordinates", "Direction", "Speed",
            "Clock", "Memory", "Armor & Item", "Keystrokes"
    };
    public static final String[] HUD_DESC = {
            "Frames per second", "Clicks per second", "Your position",
            "Compass direction", "Movement speed", "Real-world time",
            "Game RAM usage", "Gear durability", "WASD + mouse keys"
    };
    public static final int FPS = 0, CPS = 1, COORDS = 2, DIRECTION = 3, SPEED = 4,
            CLOCK = 5, MEMORY = 6, ARMOR = 7, KEYSTROKES = 8;
    public static final boolean[] HUD_ON = new boolean[HUD_NAMES.length];

    // ---- Per-module options (opened with the "Edit" chip on a card) ----
    public static final String[][] OPT_NAMES = {
            {}, {}, {"Mode", "Decimals", "Biome"}, {}, {"Unit"}, {"Format"}, {"Display"}, {"Held item"}, {"Mouse buttons"}
    };
    public static final String[][][] OPT_CHOICES = {
            {}, {},
            {{"XYZ", "Y only", "XZ only"}, {"0", "1", "2"}, {"Show", "Hide"}},
            {},
            {{"Blocks/sec", "km/h"}},
            {{"24 hour", "12 hour"}},
            {{"Used / Max", "Percent"}},
            {{"Show", "Hide"}},
            {{"Show", "Hide"}}
    };
    public static final int[][] OPT = new int[HUD_NAMES.length][];

    // ---- Performance mods ----
    public static final String[] PERF_NAMES = {
            "Hide Foliage", "Minimal Particles", "No Clouds", "No Entity Shadows", "Short Entity Range"
    };
    public static final String[] PERF_DESC = {
            "Hides grass, flowers", "Fewer particles", "Turns clouds off",
            "Skips shadows", "Mobs draw closer"
    };
    public static final int FOLIAGE = 0, PARTICLES = 1, CLOUDS = 2, SHADOWS = 3, ENTITY_RANGE = 4;
    public static final boolean[] PERF_ON = new boolean[PERF_NAMES.length];

    // ---- Customization ----
    public static final String[] POSITION_NAMES = {"Top Left", "Top Right", "Bottom Left", "Bottom Right"};
    public static final String[] ACCENT_NAMES = {"Lemon", "Sky", "Mint", "Pink", "Orange", "White"};
    public static final int[] ACCENTS = {
            0xFFFFD84A, 0xFF5BC0FF, 0xFF6BFF9E, 0xFFFF7EB6, 0xFFFF9A3C, 0xFFFFFFFF
    };
    public static final String[] BG_NAMES = {"Off", "Light", "Medium", "Dark"};
    public static final int[] BG_ALPHA = {0x00, 0x40, 0x80, 0xC0};

    public static int position = 0;
    public static int accent = 0;
    public static int background = 2;
    public static boolean textShadow = true;
    public static boolean discordRpc = true;

    static {
        Arrays.fill(HUD_ON, true);
        for (int i = 0; i < OPT.length; i++) OPT[i] = new int[OPT_NAMES[i].length];
    }

    private Settings() {}

    public static boolean on(int hudIndex) {
        return HUD_ON[hudIndex];
    }

    public static int opt(int module, int option) {
        return OPT[module][option];
    }

    public static int accentColor() {
        return ACCENTS[accent];
    }

    public static int bgColor() {
        return BG_ALPHA[background] << 24;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("bertis-client.properties");
    }

    private static int getInt(Properties p, String key, int def, int max) {
        try {
            int v = Integer.parseInt(p.getProperty(key, String.valueOf(def)));
            return Math.max(0, Math.min(max, v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static void load() {
        Path p = path();
        if (!Files.exists(p)) return;
        try (InputStream in = Files.newInputStream(p)) {
            Properties pr = new Properties();
            pr.load(in);
            for (int i = 0; i < HUD_ON.length; i++)
                HUD_ON[i] = Boolean.parseBoolean(pr.getProperty("hud." + i, "true"));
            for (int i = 0; i < PERF_ON.length; i++)
                PERF_ON[i] = Boolean.parseBoolean(pr.getProperty("perf." + i, "false"));
            for (int i = 0; i < OPT.length; i++)
                for (int j = 0; j < OPT[i].length; j++)
                    OPT[i][j] = getInt(pr, "opt." + i + "." + j, 0, OPT_CHOICES[i][j].length - 1);
            position = getInt(pr, "position", 0, POSITION_NAMES.length - 1);
            accent = getInt(pr, "accent", 0, ACCENTS.length - 1);
            background = getInt(pr, "background", 2, BG_ALPHA.length - 1);
            textShadow = Boolean.parseBoolean(pr.getProperty("textShadow", "true"));
            discordRpc = Boolean.parseBoolean(pr.getProperty("discordRpc", "true"));
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        Properties pr = new Properties();
        for (int i = 0; i < HUD_ON.length; i++) pr.setProperty("hud." + i, String.valueOf(HUD_ON[i]));
        for (int i = 0; i < PERF_ON.length; i++) pr.setProperty("perf." + i, String.valueOf(PERF_ON[i]));
        for (int i = 0; i < OPT.length; i++)
            for (int j = 0; j < OPT[i].length; j++)
                pr.setProperty("opt." + i + "." + j, String.valueOf(OPT[i][j]));
        pr.setProperty("position", String.valueOf(position));
        pr.setProperty("accent", String.valueOf(accent));
        pr.setProperty("background", String.valueOf(background));
        pr.setProperty("textShadow", String.valueOf(textShadow));
        pr.setProperty("discordRpc", String.valueOf(discordRpc));
        try (OutputStream out = Files.newOutputStream(path())) {
            pr.store(out, "Bertis Client settings");
        } catch (Exception ignored) {
        }
    }
}
