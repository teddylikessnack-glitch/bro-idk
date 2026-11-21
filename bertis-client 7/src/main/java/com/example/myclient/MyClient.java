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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyClient implements ClientModInitializer {
    public static final String MOD_ID = "myclient";

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
    private static final DateTimeFormatter CLOCK_24 = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH);
    private static final DateTimeFormatter CLOCK_12 = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH);

    private record Entry(String text, int accent) {}

    @Override
    public void onInitializeClient() {
        Settings.load();
        Branding.init();
        DiscordRpc.init();

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
            Performance.tick(client);
            DiscordRpc.tick(client);

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

    // One Lunar-style module box: translucent panel, accent stripe, white text.
    private static void drawBox(GuiGraphicsExtractor g, Minecraft mc, String text,
                                boolean rightAligned, int screenW, int y, int accent) {
        int w = mc.font.width(text) + 10;
        int h = mc.font.lineHeight + 6;
        int x = rightAligned ? screenW - 6 - w : 6;
        g.fill(x, y, x + w, y + h, Settings.bgColor());
        g.fill(x, y, x + 2, y + h, accent);
        g.text(mc.font, text, x + 6, y + 3, 0xFFFFFFFF, Settings.textShadow);
    }

    private static void drawKey(GuiGraphicsExtractor g, Minecraft mc, String label, boolean down,
                                int x, int y, int w, int h) {
        int idle = Math.max(Settings.BG_ALPHA[Settings.background], 0x50) << 24;
        g.fill(x, y, x + w, y + h, down ? 0xB0FFFFFF : idle);
        int tx = x + (w - mc.font.width(label)) / 2;
        int ty = y + (h - mc.font.lineHeight) / 2 + 1;
        g.text(mc.font, label, tx, ty, down ? 0xFF000000 : 0xFFFFFFFF, false);
    }

    private static void extractHud(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!hudVisible || mc.player == null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        boolean right = Settings.position % 2 == 1;
        boolean bottom = Settings.position >= 2;
        int accent = Settings.accentColor();

        // ---- main stack ----
        List<Entry> main = new ArrayList<>();
        if (Settings.on(Settings.FPS)) main.add(new Entry(mc.getFps() + " FPS", accent));

        if (Settings.on(Settings.CPS)) {
            long now = System.currentTimeMillis();
            while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.removeFirst();
            main.add(new Entry(clicks.size() + " CPS", accent));
        }

        if (Settings.on(Settings.COORDS)) {
            String f = "%." + Settings.opt(Settings.COORDS, 1) + "f";
            double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
            String text = switch (Settings.opt(Settings.COORDS, 0)) {
                case 1 -> String.format(Locale.ROOT, "Y: " + f, py);
                case 2 -> String.format(Locale.ROOT, "XZ: " + f + " / " + f, px, pz);
                default -> String.format(Locale.ROOT, "XYZ: " + f + " / " + f + " / " + f, px, py, pz);
            };
            main.add(new Entry(text, accent));

            if (Settings.opt(Settings.COORDS, 2) == 0 && mc.level != null) {
                main.add(new Entry("Biome: " + biomeName(mc), accent));
            }
        }

        if (Settings.on(Settings.DIRECTION)) {
            String facing = mc.player.getDirection().getName();
            facing = facing.substring(0, 1).toUpperCase(Locale.ROOT) + facing.substring(1);
            main.add(new Entry("Facing: " + facing, accent));
        }

        if (Settings.on(Settings.SPEED)) {
            boolean kmh = Settings.opt(Settings.SPEED, 0) == 1;
            main.add(new Entry(String.format(Locale.ROOT, kmh ? "%.1f km/h" : "%.1f b/s",
                    kmh ? speed * 3.6 : speed), accent));
        }

        if (Settings.on(Settings.CLOCK)) {
            boolean twelve = Settings.opt(Settings.CLOCK, 0) == 1;
            main.add(new Entry(LocalTime.now().format(twelve ? CLOCK_12 : CLOCK_24), accent));
        }

        if (Settings.on(Settings.MEMORY)) {
            Runtime rt = Runtime.getRuntime();
            long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
            long maxMb = rt.maxMemory() / 1048576L;
            boolean percent = Settings.opt(Settings.MEMORY, 0) == 1;
            main.add(new Entry(percent ? "Mem: " + (usedMb * 100 / Math.max(1, maxMb)) + "%"
                    : "Mem: " + usedMb + "/" + maxMb + " MB", accent));
        }

        boolean showMouse = Settings.opt(Settings.KEYSTROKES, 0) == 0;
        int boxH = mc.font.lineHeight + 6 + 2;
        int ksH = Settings.on(Settings.KEYSTROKES) ? (showMouse ? 66 : 48) : 0;
        int mainH = main.size() * boxH + ksH;
        int y = bottom ? sh - 6 - mainH : 6;
        for (Entry e : main) {
            drawBox(g, mc, e.text(), right, sw, y, e.accent());
            y += boxH;
        }

        if (ksH > 0) {
            int kx = right ? sw - 6 - 64 : 6;
            int ky = y + 4;
            int s = 20;
            drawKey(g, mc, "W", mc.options.keyUp.isDown(), kx + s + 2, ky, s, s);
            drawKey(g, mc, "A", mc.options.keyLeft.isDown(), kx, ky + s + 2, s, s);
            drawKey(g, mc, "S", mc.options.keyDown.isDown(), kx + s + 2, ky + s + 2, s, s);
            drawKey(g, mc, "D", mc.options.keyRight.isDown(), kx + 2 * (s + 2), ky + s + 2, s, s);
            if (showMouse) {
                drawKey(g, mc, "LMB", mc.options.keyAttack.isDown(), kx, ky + 2 * (s + 2), 31, 18);
                drawKey(g, mc, "RMB", mc.options.keyUse.isDown(), kx + 33, ky + 2 * (s + 2), 31, 18);
            }
        }

        // ---- armor + held item panel on the opposite side ----
        if (Settings.on(Settings.ARMOR)) {
            List<ItemStack> gear = new ArrayList<>();
            for (EquipmentSlot slot : ARMOR) {
                ItemStack st = mc.player.getItemBySlot(slot);
                if (!st.isEmpty()) gear.add(st);
            }
            if (Settings.opt(Settings.ARMOR, 0) == 0) {
                ItemStack held = mc.player.getMainHandItem();
                if (!held.isEmpty()) gear.add(held);
            }
            drawGear(g, mc, gear, !right, bottom, sw, sh);
        }
    }

    // Clean gear panel: item icon, slim durability bar, percentage.
    private static void drawGear(GuiGraphicsExtractor g, Minecraft mc, List<ItemStack> gear,
                                 boolean rightSide, boolean bottom, int sw, int sh) {
        if (gear.isEmpty()) return;
        int rowH = 18, gap = 2, barW = 36;
        int labelW = Math.max(mc.font.width("100%"), mc.font.width("x64"));
        int w = 6 + 16 + 5 + barW + 5 + labelW + 6;
        int totalH = gear.size() * (rowH + gap) - gap;
        int x = rightSide ? sw - 6 - w : 6;
        int y = bottom ? sh - 6 - totalH : 6;

        for (ItemStack st : gear) {
            int stripe = Settings.accentColor();
            int bx = x + 6 + 16 + 5;
            g.fill(x, y, x + w, y + rowH, Settings.bgColor());

            if (st.isDamageableItem()) {
                int max = Math.max(1, st.getMaxDamage());
                double frac = Math.max(0.0, Math.min(1.0, (double) (max - st.getDamageValue()) / max));
                stripe = frac > 0.5 ? 0xFF55FF55 : (frac > 0.2 ? 0xFFFFFF55 : 0xFFFF5555);
                g.fill(bx, y + 7, bx + barW, y + 11, 0xFF222222);
                g.fill(bx, y + 7, bx + (int) Math.round(barW * frac), y + 11, stripe);
                String pct = Math.round(frac * 100) + "%";
                g.text(mc.font, pct, bx + barW + 5, y + (rowH - mc.font.lineHeight) / 2 + 1,
                        0xFFFFFFFF, Settings.textShadow);
            } else if (st.getCount() > 1) {
                g.text(mc.font, "x" + st.getCount(), bx, y + (rowH - mc.font.lineHeight) / 2 + 1,
                        0xFFFFFFFF, Settings.textShadow);
            }

            g.fill(x, y, x + 2, y + rowH, stripe);
            g.item(st, x + 6, y + 1);
            y += rowH + gap;
        }
    }

    private static String biomeName(Minecraft mc) {
        String id = mc.level.getBiome(mc.player.blockPosition()).getRegisteredName();
        int colon = id.indexOf(':');
        if (colon >= 0) id = id.substring(colon + 1);
        StringBuilder out = new StringBuilder();
        for (String word : id.split("_")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return out.toString();
    }
}
