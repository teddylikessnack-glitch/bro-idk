package com.example.myclient;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;

public final class Performance {
    private static int restoreTo = -1;
    private static int wait = 0;

    private static final int[] BALANCED = {Settings.SHADOWS, Settings.BLEND, Settings.ENTITY_RANGE};
    private static final int[] LOW_ADDS = {Settings.PARTICLES, Settings.CLOUDS, Settings.UNLIMITED_FPS,
            Settings.LOW_SIM, Settings.FLAT_LIGHT, Settings.NO_BLUR};
    private static final int[] POTATO_ADDS = {Settings.FOLIAGE, Settings.LOW_RENDER, Settings.VSYNC_OFF,
            Settings.REDUCED_FX};

    private Performance() {}

    private static int opt(int index, int which) {
        int[] o = Settings.PERF_OPT[index];
        return which < o.length ? o[which] : 0;
    }

    // Called whenever a performance card (or one of its options) changes.
    public static void apply(int index) {
        Minecraft mc = Minecraft.getInstance();
        boolean on = Settings.PERF_ON[index];
        int o = opt(index, 0);
        switch (index) {
            case Settings.FOLIAGE -> refreshWorld(mc);
            case Settings.PARTICLES ->
                    mc.options.particles().set(on ? (o == 0 ? ParticleStatus.MINIMAL : ParticleStatus.DECREASED)
                            : ParticleStatus.ALL);
            case Settings.CLOUDS ->
                    mc.options.cloudStatus().set(on ? (o == 0 ? CloudStatus.OFF : CloudStatus.FAST)
                            : CloudStatus.FANCY);
            case Settings.SHADOWS -> mc.options.entityShadows().set(!on);
            case Settings.ENTITY_RANGE ->
                    mc.options.entityDistanceScaling().set(on ? new double[]{0.5, 0.25, 0.75}[o] : 1.0);
            case Settings.BLEND -> mc.options.biomeBlendRadius().set(on ? o : 2);
            case Settings.UNLIMITED_FPS -> mc.options.framerateLimit().set(on ? new int[]{260, 240, 144}[o] : 120);
            case Settings.VSYNC_OFF -> mc.options.enableVsync().set(!on);
            case Settings.LOW_SIM -> mc.options.simulationDistance().set(on ? new int[]{6, 5, 8, 10}[o] : 12);
            case Settings.LOW_RENDER -> mc.options.renderDistance().set(on ? new int[]{8, 6, 10, 12}[o] : 12);
            case Settings.FLAT_LIGHT -> mc.options.ambientOcclusion().set(!on);
            case Settings.NO_BLUR -> mc.options.menuBackgroundBlurriness().set(on ? 0 : 5);
            case Settings.REDUCED_FX -> {
                mc.options.screenEffectScale().set(on && opt(index, 0) == 0 ? 0.0 : 1.0);
                mc.options.fovEffectScale().set(on && opt(index, 1) == 0 ? 0.0 : 1.0);
            }
            default -> { }
        }
        mc.options.save();
    }

    // 0 Off, 1 Balanced, 2 Low, 3 Potato
    public static void applyPreset(int preset) {
        boolean[] want = new boolean[Settings.PERF_ON.length];
        if (preset >= 1) for (int i : BALANCED) want[i] = true;
        if (preset >= 2) for (int i : LOW_ADDS) want[i] = true;
        if (preset >= 3) for (int i : POTATO_ADDS) want[i] = true;
        for (int i = 0; i < want.length; i++) {
            if (Settings.PERF_ON[i] != want[i]) {
                Settings.PERF_ON[i] = want[i];
                apply(i);
            }
        }
        Settings.preset = preset;
    }

    // Nudges render distance by 1 for a few ticks so the game re-meshes every chunk.
    private static void refreshWorld(Minecraft mc) {
        if (mc.level == null || restoreTo >= 0) return;
        int d = mc.options.renderDistance().get();
        restoreTo = d;
        wait = 3;
        mc.options.renderDistance().set(d > 2 ? d - 1 : d + 1);
    }

    // Call every client tick.
    public static void tick(Minecraft mc) {
        if (restoreTo < 0) return;
        if (--wait > 0) return;
        int d = restoreTo;
        restoreTo = -1;
        mc.options.renderDistance().set(d);
    }
}
