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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Locale;

public class MyClient implements ClientModInitializer {
    public static final String MOD_ID = "myclient";

    // 2 = full HUD, 1 = minimal (FPS only), 0 = off
    private static int hudMode = 2;
    private static boolean coordsEnabled = true;

    private static KeyMapping toggleHud;
    private static KeyMapping toggleCoords;

    // CPS tracking
    private static final ArrayDeque<Long> clicks = new ArrayDeque<>();
    private static boolean wasAttackDown = false;

    // Speed tracking
    private static double lastX, lastZ, speed;
    private static boolean hasLast = false;

    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

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
            while (toggleHud.consumeClick()) hudMode = (hudMode + 2) % 3;
            while (toggleCoords.consumeClick()) coordsEnabled = !coordsEnabled;

            if (client.player == null) {
                hasLast = false;
                return;
            }

            // CPS: count each new press of the attack button
            boolean down = client.options.keyAttack.isDown();
            if (down && !wasAttackDown) clicks.addLast(System.currentTimeMillis());
            wasAttackDown = down;

            // Speed in blocks/second (20 ticks per second)
            double x = client.player.getX();
            double z = client.player.getZ();
            if (hasLast) speed = Math.hypot(x - lastX, z - lastZ) * 20.0;
            lastX = x;
            lastZ = z;
            hasLast = true;
        });

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath(MOD_ID, "info_hud"),
                MyClient::extractHud);
    }

    private static void draw(GuiGraphicsExtractor g, Minecraft mc, String s, int x, int y, int color) {
        // Colors are ARGB: the alpha byte (0xFF) must be set or text is invisible.
        g.text(mc.font, s, x, y, color, true);
    }

    private static void extractHud(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (hudMode == 0 || mc.player == null) return;

        int x = 6;
        int y = 6;
        int lh = 11;

        draw(g, mc, "FPS: " + mc.getFps(), x, y, 0xFFFFFFFF);
        y += lh;
        if (hudMode == 1) return;

        long now = System.currentTimeMillis();
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.removeFirst();
        draw(g, mc, "CPS: " + clicks.size(), x, y, 0xFFFFAA00);
        y += lh;

        if (coordsEnabled) {
            String pos = String.format(Locale.ROOT, "XYZ: %.1f / %.1f / %.1f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());
            draw(g, mc, pos, x, y, 0xFFFFFF55);
            y += lh;
        }

        String facing = mc.player.getDirection().getName();
        facing = facing.substring(0, 1).toUpperCase(Locale.ROOT) + facing.substring(1);
        draw(g, mc, "Facing: " + facing, x, y, 0xFF55FFFF);
        y += lh;

        draw(g, mc, String.format(Locale.ROOT, "Speed: %.1f b/s", speed), x, y, 0xFF55FF55);
        y += lh;

        draw(g, mc, "Time: " + LocalTime.now().format(CLOCK), x, y, 0xFFFFFFFF);
        y += lh;

        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
        long maxMb = rt.maxMemory() / 1048576L;
        draw(g, mc, "Mem: " + usedMb + "/" + maxMb + " MB", x, y, 0xFFAAAAAA);

        // Right side: armor and held item durability
        int rx = mc.getWindow().getGuiScaledWidth() - 6;
        int ry = 6;
        for (EquipmentSlot slot : ARMOR) {
            ry = drawItem(g, mc, mc.player.getItemBySlot(slot), rx, ry, lh);
        }
        drawItem(g, mc, mc.player.getMainHandItem(), rx, ry + 4, lh);
    }

    private static int drawItem(GuiGraphicsExtractor g, Minecraft mc, ItemStack stack,
                                int rx, int y, int lh) {
        if (stack.isEmpty()) return y;

        String text = stack.getHoverName().getString();
        int color = 0xFFFFFFFF;
        if (stack.isDamageableItem()) {
            int max = stack.getMaxDamage();
            int left = max - stack.getDamageValue();
            text += " " + left + "/" + max;
            double frac = (double) left / max;
            color = frac > 0.5 ? 0xFF55FF55 : (frac > 0.2 ? 0xFFFFFF55 : 0xFFFF5555);
        } else if (stack.getCount() > 1) {
            text += " x" + stack.getCount();
        }

        draw(g, mc, text, rx - mc.font.width(text), y, color);
        return y + lh;
    }
}
