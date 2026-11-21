package com.example.myclient;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;

public final class Performance {
    private Performance() {}

    // Called whenever a performance card is toggled.
    public static void apply(int index) {
        Minecraft mc = Minecraft.getInstance();
        boolean on = Settings.PERF_ON[index];
        switch (index) {
            case Settings.FOLIAGE -> {
                if (mc.levelRenderer != null) mc.levelRenderer.invalidateCompiledGeometry();
            }
            case Settings.PARTICLES -> mc.options.particles().set(on ? ParticleStatus.MINIMAL : ParticleStatus.ALL);
            case Settings.CLOUDS -> mc.options.cloudStatus().set(on ? CloudStatus.OFF : CloudStatus.FANCY);
            case Settings.SHADOWS -> mc.options.entityShadows().set(!on);
            case Settings.ENTITY_RANGE -> mc.options.entityDistanceScaling().set(on ? 0.5 : 1.0);
            default -> { }
        }
    }
}
