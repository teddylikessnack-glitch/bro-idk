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

    private static final int ACCENT = 0xFFFFD84A;      // lemon yellow
    private static final int BG = 0x80000000;          // translucent black

    private static boolean hudVisible = true;
    private static KeyMapping openMenu;
    private static KeyMapping toggleHud;

    private static final ArrayDeque<Long> clicks = new ArrayDeque<>();
    private static boolean wasAttackDown = false;

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

        openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.myclient.open_menu", InputConstants.Type.KEYSYM,
                InputConstants.KEY_RSHIFT, category));

        toggleHud = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.myclient.toggle_hud", InputConstants.Type.KEYSYM,
                InputConstants.KEY_RCONTROL, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick()) client.setScreenAndShow(new BertisMenu());
            while (toggleHud.consumeClick()) hudVisible = !hudVisible;

            if (client.player == null) {
                hasLast = false;
                return;
            }

            boolean down = client.options.keyAttack.isDown();
            if (down && !wasAttackDown) clicks.addLast(System.currentTimeMillis());
            wasAttackDown = down;

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

    // One Lunar-style module box: dark panel, colored accent bar, white text.
    private static int box(GuiGraphicsExtractor g, Minecraft mc, String text, int x, int y, int accent) {
        int w = mc.font.width(text) + 10;
        int h = mc.font.lineHeight + 6;
        g.fill(x, y, x + w, y + h, BG);
        g.fill(x, y, x + 2, y + h, accent);
        g.text(mc.font, text, x + 6, y + 3, 0xFFFFFFFF, true);
        return y + h + 2;
    }

    private static int boxRight(GuiGraphicsExtractor g, Minecraft mc, String text, int rightX, int y, int accent) {
        int w = mc.font.width(text) + 10;
        return box(g, mc, text, rightX - w, y, accent);
    }

    private static void key(GuiGraphicsExtractor g, Minecraft mc, String label, boolean down,
                            int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, down ? 0xB0FFFFFF : BG);
        int tx = x + (w - mc.font.width(label)) / 2;
        int ty = y + (h - mc.font.lineHeight) / 2 + 1;
        g.text(mc.font, label, tx, ty, down ? 0xFF000000 : 0xFFFFFFFF, false);
    }

    private static void extractHud(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!hudVisible || mc.player == null) return;

        int x = 6;
        int y = 6;

        if (Settings.on(Settings.FPS)) y = box(g, mc, mc.getFps() + " FPS", x, y, ACCENT);

        if (Settings.on(Settings.CPS)) {
            long now = System.currentTimeMillis();
            while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.removeFirst();
            y = box(g, mc, clicks.size() + " CPS", x, y, ACCENT);
        }

        if (Settings.on(Settings.COORDS)) {
            String pos = String.format(Locale.ROOT, "XYZ: %.0f / %.0f / %.0f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());
            y = box(g, mc, pos, x, y, ACCENT);
        }

        if (Settings.on(Settings.DIRECTION)) {
            String facing = mc.player.getDirection().getName();
            facing = facing.substring(0, 1).toUpperCase(Locale.ROOT) + facing.substring(1);
            y = box(g, mc, "Facing: " + facing, x, y, ACCENT);
        }

        if (Settings.on(Settings.SPEED)) {
            y = box(g, mc, String.format(Locale.ROOT, "%.1f b/s", speed), x, y, ACCENT);
        }

        if (Settings.on(Settings.CLOCK)) {
            y = box(g, mc, LocalTime.now().format(CLOCK), x, y, ACCENT);
        }

        if (Settings.on(Settings.MEMORY)) {
            Runtime rt = Runtime.getRuntime();
            long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
            long maxMb = rt.maxMemory() / 1048576L;
            y = box(g, mc, "Mem: " + usedMb + "/" + maxMb + " MB", x, y, ACCENT);
        }

        if (Settings.on(Settings.KEYSTROKES)) {
            y += 4;
            int s = 20;
            key(g, mc, "W", mc.options.keyUp.isDown(), x + s + 2, y, s, s);
            key(g, mc, "A", mc.options.keyLeft.isDown(), x, y + s + 2, s, s);
            key(g, mc, "S", mc.options.keyDown.isDown(), x + s + 2, y + s + 2, s, s);
            key(g, mc, "D", mc.options.keyRight.isDown(), x + 2 * (s + 2), y + s + 2, s, s);
            key(g, mc, "LMB", mc.options.keyAttack.isDown(), x, y + 2 * (s + 2), 31, 18);
            key(g, mc, "RMB", mc.options.keyUse.isDown(), x + 33, y + 2 * (s + 2), 31, 18);
        }

        if (Settings.on(Settings.ARMOR)) {
            int rx = mc.getWindow().getGuiScaledWidth() - 6;
            int ry = 6;
            for (EquipmentSlot slot : ARMOR) {
                ry = drawItem(g, mc, mc.player.getItemBySlot(slot), rx, ry);
            }
            drawItem(g, mc, mc.player.getMainHandItem(), rx, ry + 4);
        }
    }

    private static int drawItem(GuiGraphicsExtractor g, Minecraft mc, ItemStack stack, int rx, int y) {
        if (stack.isEmpty()) return y;

        String text = stack.getHoverName().getString();
        int accent = ACCENT;
        if (stack.isDamageableItem()) {
            int max = stack.getMaxDamage();
            int left = max - stack.getDamageValue();
            text += " " + left + "/" + max;
            double frac = (double) left / max;
            accent = frac > 0.5 ? 0xFF55FF55 : (frac > 0.2 ? 0xFFFFFF55 : 0xFFFF5555);
        } else if (stack.getCount() > 1) {
            text += " x" + stack.getCount();
        }
        return boxRight(g, mc, text, rx, y, accent);
    }
}
