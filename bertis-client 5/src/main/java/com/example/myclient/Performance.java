package com.example.myclient;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;

public final class Performance {
    private static int restoreTo = -1;
    private static int wait = 0;

    private Performance() {}

    // Called whenever a performance card is toggled.
    public static void apply(int index) {
        Minecraft mc = Minecraft.getInstance();
        boolean on = Settings.PERF_ON[index];
        switch (index) {
            case Settings.FOLIAGE -> refreshWorld(mc);
            case Settings.PARTICLES -> mc.options.particles().set(on ? ParticleStatus.MINIMAL : ParticleStatus.ALL);
            case Settings.CLOUDS -> mc.options.cloudStatus().set(on ? CloudStatus.OFF : CloudStatus.FANCY);
            case Settings.SHADOWS -> mc.options.entityShadows().set(!on);
            case Settings.ENTITY_RANGE -> mc.options.entityDistanceScaling().set(on ? 0.5 : 1.0);
            case Settings.BLEND -> mc.options.biomeBlendRadius().set(on ? 0 : 2);
            case Settings.UNLIMITED_FPS -> mc.options.framerateLimit().set(on ? 260 : 120);
            case Settings.VSYNC_OFF -> mc.options.enableVsync().set(!on);
            case Settings.LOW_SIM -> mc.options.simulationDistance().set(on ? 6 : 12);
            case Settings.LOW_RENDER -> mc.options.renderDistance().set(on ? 8 : 12);
            default -> { }
        }
        mc.options.save();
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
