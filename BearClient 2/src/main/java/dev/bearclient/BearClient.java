package dev.bearclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class BearClient implements ClientModInitializer {
    public static final String MOD_ID = "bearclient";
    public static final String NAME = "Bear Client";
    public static final String VERSION = "1.1.1";
    public static final String MC_VERSION = "26.3";

    private static final float[] FRAMES = new float[120];
    private static int frameIdx;
    private static long lastNanos;

    private static KeyMapping menuKey, mapKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.bearclient.menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_RSHIFT, category));
        mapKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.bearclient.map", InputConstants.Type.KEYBOARD, InputConstants.KEY_M, category));

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "hud"), HudRenderer::extract);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.gui.screen() instanceof TitleScreen) mc.gui.setScreen(new BearTitleScreen());
            while (menuKey.consumeClick()) {
                if (mc.gui.screen() == null) mc.gui.setScreen(new ModuleScreen(null));
            }
            while (mapKey.consumeClick()) {
                if (mc.gui.screen() == null && mc.level != null) mc.gui.setScreen(new WorldMapScreen(null));
            }
            WorldMapData.tick(mc);
            ModuleManager.tick(mc);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> WorldMapData.clear());
        ClientLifecycleEvents.CLIENT_STOPPING.register(ModuleManager::shutdown);
    }

    public static void recordFrame() {
        long now = System.nanoTime();
        if (lastNanos != 0) {
            FRAMES[frameIdx] = Math.min(100f, (now - lastNanos) / 1_000_000f);
            frameIdx = (frameIdx + 1) % FRAMES.length;
        }
        lastNanos = now;
    }

    public static float frameMs(int i) { return FRAMES[(frameIdx + i) % FRAMES.length]; }

    public static int frameCount() { return FRAMES.length; }

    public static float avgFrameMs() {
        float sum = 0;
        for (float f : FRAMES) sum += f;
        return sum / FRAMES.length;
    }

    public static int accent() {
        Module r = ModuleManager.rainbow;
        if (r != null && r.isEnabled()) {
            float hue = (System.currentTimeMillis() % 5000L) / 5000f;
            return 0xFF000000 | Mth.hsvToRgb(hue, 0.55f, 1f);
        }
        return 0xFFE8A04A;
    }
}
