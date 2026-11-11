package com.example.myclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public class MyClient implements ClientModInitializer {
    public static final String MOD_ID = "myclient";

    private static boolean hudEnabled = true;
    private static boolean coordsEnabled = true;

    private static KeyMapping toggleHud;
    private static KeyMapping toggleCoords;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        toggleHud = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.myclient.toggle_hud", InputConstants.Type.KEYSYM,
                InputConstants.KEY_RSHIFT, category));

        toggleCoords = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.myclient.toggle_coords", InputConstants.Type.KEYSYM,
                InputConstants.KEY_RCONTROL, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleHud.consumeClick()) hudEnabled = !hudEnabled;
            while (toggleCoords.consumeClick()) coordsEnabled = !coordsEnabled;
        });

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath(MOD_ID, "info_hud"),
                MyClient::extractHud);
    }

    private static void extractHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!hudEnabled || mc.player == null || mc.options.hideGui) return;

        int x = 6;
        int y = 6;
        int lineHeight = 11;

        // Colors are ARGB: the alpha byte (0xFF) must be set or text is invisible.
        graphics.text(mc.font, "FPS: " + mc.getFps(), x, y, 0xFFFFFFFF, true);
        y += lineHeight;

        if (coordsEnabled) {
            String pos = String.format(Locale.ROOT, "XYZ: %.1f / %.1f / %.1f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());
            graphics.text(mc.font, pos, x, y, 0xFFFFFF55, true);
            y += lineHeight;
        }

        String facing = mc.player.getDirection().getName();
        facing = facing.substring(0, 1).toUpperCase(Locale.ROOT) + facing.substring(1);
        graphics.text(mc.font, "Facing: " + facing, x, y, 0xFF55FFFF, true);
    }
}
