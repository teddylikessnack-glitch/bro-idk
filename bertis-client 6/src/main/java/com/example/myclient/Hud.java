package com.example.myclient;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Hud {
    public static final class Tile {
        public final int id;
        public final int kind; // 0 square, 1 lines (coordinates), 2 gear, 3 keystrokes, 4 potions
        public int x, y, w, h;
        public String value = "", label = "";
        public int accent = 0xFFFFFFFF, valueScale = 1;
        public final List<String> lines = new ArrayList<>();
        public final List<ItemStack> gear = new ArrayList<>();
        public final List<String> right = new ArrayList<>();      // potion timers
        public final List<Integer> colors = new ArrayList<>();    // potion colors
        public final List<Integer> secs = new ArrayList<>();      // potion seconds left
        public boolean showMouse = true;

        Tile(int id, int kind) {
            this.id = id;
            this.kind = kind;
        }
    }

    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final ArrayDeque<Long> clicks = new ArrayDeque<>();
    private static boolean wasAttackDown = false;
    private static double lastX, lastZ, speed;
    private static boolean hasLast = false;

    public static boolean visible = true;
    private static long noticeUntil = 0;
    private static String noticeText = "";

    private Hud() {}

    // ------------------------------------------------------------------ state
    public static void tick(Minecraft client) {
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
    }

    public static void toggleVisible() {
        visible = !visible;
        noticeText = visible ? "HUD shown" : "HUD hidden - press Right Ctrl to show it again";
        noticeUntil = System.currentTimeMillis() + 2500;
    }

    // ------------------------------------------------------------------ tiles
    private static Tile square(Minecraft mc, int id, String value, String label, int accent) {
        Tile t = new Tile(id, 0);
        t.value = value;
        t.label = label;
        t.accent = accent;
        int w1 = mc.font.width(value);
        t.valueScale = (w1 * 2 + 12 <= 60) ? 2 : 1;
        t.w = Math.max(Math.max(40, w1 * t.valueScale + 12), mc.font.width(label) + 10);
        t.h = 36;
        return t;
    }

    public static List<Tile> build(Minecraft mc, boolean edit) {
        List<Tile> out = new ArrayList<>();
        if (mc.player == null) return out;
        int accent = Settings.accentColor();

        if (Settings.on(Settings.FPS)) out.add(square(mc, Settings.FPS, String.valueOf(mc.getFps()), "FPS", accent));

        if (Settings.on(Settings.CPS)) {
            long now = System.currentTimeMillis();
            while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.removeFirst();
            out.add(square(mc, Settings.CPS, String.valueOf(clicks.size()), "CPS", accent));
        }

        if (Settings.on(Settings.COORDS)) {
            Tile t = new Tile(Settings.COORDS, 1);
            t.accent = accent;
            String f = "%." + Settings.opt(Settings.COORDS, 1) + "f";
            double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
            switch (Settings.opt(Settings.COORDS, 0)) {
                case 1 -> t.lines.add(String.format(Locale.ROOT, "Y  " + f, py));
                case 2 -> {
                    t.lines.add(String.format(Locale.ROOT, "X  " + f, px));
                    t.lines.add(String.format(Locale.ROOT, "Z  " + f, pz));
                }
                default -> {
                    t.lines.add(String.format(Locale.ROOT, "X  " + f, px));
                    t.lines.add(String.format(Locale.ROOT, "Y  " + f, py));
                    t.lines.add(String.format(Locale.ROOT, "Z  " + f, pz));
                }
            }
            if (Settings.opt(Settings.COORDS, 2) == 0 && mc.level != null) t.label = biomeName(mc);
            int maxW = mc.font.width(t.label);
            for (String line : t.lines) maxW = Math.max(maxW, mc.font.width(line));
            t.w = Math.max(44, maxW + 14);
            t.h = t.lines.size() * 11 + (t.label.isEmpty() ? 0 : 12) + 8;
            out.add(t);
        }

        if (Settings.on(Settings.DIRECTION)) {
            String name = mc.player.getDirection().getName();
            String v = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
            out.add(square(mc, Settings.DIRECTION, v, "FACING", accent));
        }

        if (Settings.on(Settings.SPEED)) {
            boolean kmh = Settings.opt(Settings.SPEED, 0) == 1;
            out.add(square(mc, Settings.SPEED, String.format(Locale.ROOT, "%.1f", kmh ? speed * 3.6 : speed),
                    kmh ? "km/h" : "b/s", accent));
        }

        if (Settings.on(Settings.CLOCK)) {
            LocalTime now = LocalTime.now();
            boolean twelve = Settings.opt(Settings.CLOCK, 0) == 1;
            int hour = twelve ? (now.getHour() % 12 == 0 ? 12 : now.getHour() % 12) : now.getHour();
            String v = String.format(Locale.ROOT, "%02d:%02d", hour, now.getMinute());
            out.add(square(mc, Settings.CLOCK, v, twelve ? (now.getHour() < 12 ? "AM" : "PM") : "TIME", accent));
        }

        if (Settings.on(Settings.MEMORY)) {
            Runtime rt = Runtime.getRuntime();
            long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
            long maxMb = Math.max(1, rt.maxMemory() / 1048576L);
            boolean percent = Settings.opt(Settings.MEMORY, 0) == 1;
            out.add(percent ? square(mc, Settings.MEMORY, (usedMb * 100 / maxMb) + "%", "RAM", accent)
                    : square(mc, Settings.MEMORY, String.valueOf(usedMb), "/ " + maxMb + " MB", accent));
        }

        if (Settings.on(Settings.PING)) {
            String pv = "--";
            int pc = accent;
            var conn = mc.getConnection();
            if (conn != null) {
                var info = conn.getPlayerInfo(mc.player.getUUID());
                if (info != null) {
                    int ms = info.getLatency();
                    pv = String.valueOf(ms);
                    pc = ms < 80 ? 0xFF55FF55 : (ms < 160 ? 0xFFFFFF55 : 0xFFFF5555);
                }
            }
            out.add(square(mc, Settings.PING, pv, "PING", pc));
        }

        if (Settings.on(Settings.POTIONS)) {
            Tile t = new Tile(Settings.POTIONS, 4);
            int maxW = 0;
            for (var inst : mc.player.getActiveEffects()) {
                var effect = inst.getEffect().value();
                String name = effect.getDisplayName().getString();
                if (inst.getAmplifier() > 0) name += " " + roman(inst.getAmplifier() + 1);
                int dur = inst.getDuration();
                String time = dur < 0 ? "inf" : String.format(Locale.ROOT, "%d:%02d", dur / 1200, (dur / 20) % 60);
                t.lines.add(name);
                t.right.add(time);
                t.colors.add(effect.getColor());
                t.secs.add(dur < 0 ? 9999 : dur / 20);
                maxW = Math.max(maxW, mc.font.width(name) + mc.font.width(time));
            }
            if (!t.lines.isEmpty()) {
                t.w = Math.max(96, maxW + 26);
                t.h = t.lines.size() * 12 + 6;
                out.add(t);
            } else if (edit) {
                t.w = 96;
                t.h = 20;
                out.add(t);
            }
        }

        if (Settings.on(Settings.ARMOR)) {
            Tile t = new Tile(Settings.ARMOR, 2);
            for (EquipmentSlot slot : ARMOR) {
                ItemStack st = mc.player.getItemBySlot(slot);
                if (!st.isEmpty()) t.gear.add(st);
            }
            if (Settings.opt(Settings.ARMOR, 0) == 0) {
                ItemStack held = mc.player.getMainHandItem();
                if (!held.isEmpty()) t.gear.add(held);
            }
            if (!t.gear.isEmpty()) {
                int labelW = Math.max(mc.font.width("100%"), mc.font.width("x64"));
                t.w = 6 + 16 + 5 + 36 + 5 + labelW + 6;
                t.h = t.gear.size() * 18 + (t.gear.size() - 1) * 2;
                out.add(t);
            } else if (edit) {
                t.w = 80;
                t.h = 20;
                out.add(t);
            }
        }

        if (Settings.on(Settings.KEYSTROKES)) {
            Tile t = new Tile(Settings.KEYSTROKES, 3);
            t.showMouse = Settings.opt(Settings.KEYSTROKES, 0) == 0;
            t.w = 64;
            t.h = t.showMouse ? 62 : 42;
            out.add(t);
        }
        return out;
    }

    private static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 1 && n <= 10 ? r[n] : String.valueOf(n);
    }

    public static String biomeName(Minecraft mc) {
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

    // ------------------------------------------------------------------ layout
    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // Tiles you never dragged flow into rows from the corner chosen in Customize.
    public static void place(List<Tile> tiles, int sw, int sh) {
        boolean right = Settings.position % 2 == 1;
        boolean bottom = Settings.position >= 2;
        int maxW = Math.max(130, sw * 40 / 100);
        int fx = 6, fy = 6, rowH = 0;
        for (Tile t : tiles) {
            if (Settings.PLACED[t.id]) {
                t.x = clamp((int) (Settings.POS_X[t.id] * sw), 0, Math.max(0, sw - t.w));
                t.y = clamp((int) (Settings.POS_Y[t.id] * sh), 0, Math.max(0, sh - t.h));
                continue;
            }
            if (fx > 6 && fx + t.w > maxW) {
                fx = 6;
                fy += rowH + 4;
                rowH = 0;
            }
            t.x = right ? sw - fx - t.w : fx;
            t.y = bottom ? sh - fy - t.h : fy;
            fx += t.w + 4;
            rowH = Math.max(rowH, t.h);
        }
    }

    // Called when a drag starts: pin every tile where it is so the others don't jump around.
    public static void freezeLayout(List<Tile> tiles, int sw, int sh) {
        for (Tile t : tiles) {
            Settings.PLACED[t.id] = true;
            Settings.POS_X[t.id] = t.x / (float) sw;
            Settings.POS_Y[t.id] = t.y / (float) sh;
        }
    }

    // ------------------------------------------------------------------ drawing
    public static void drawTile(GuiGraphicsExtractor g, Minecraft mc, Tile t) {
        switch (t.kind) {
            case 0 -> drawSquare(g, mc, t);
            case 1 -> drawLines(g, mc, t);
            case 2 -> drawGear(g, mc, t);
            case 4 -> drawPotions(g, mc, t);
            default -> drawKeys(g, mc, t);
        }
    }

    private static void drawSquare(GuiGraphicsExtractor g, Minecraft mc, Tile t) {
        g.fill(t.x, t.y, t.x + t.w, t.y + t.h, Settings.bgColor());
        g.fill(t.x, t.y, t.x + t.w, t.y + 2, t.accent);
        int vw = mc.font.width(t.value) * t.valueScale;
        int vx = t.x + (t.w - vw) / 2;
        int vy = t.y + (t.valueScale == 2 ? 7 : 11);
        if (t.valueScale == 2) {
            var pose = g.pose();
            pose.pushMatrix();
            pose.translate((float) vx, (float) vy);
            pose.scale(2.0f, 2.0f);
            g.text(mc.font, t.value, 0, 0, 0xFFFFFFFF, Settings.textShadow);
            pose.popMatrix();
        } else {
            g.text(mc.font, t.value, vx, vy, 0xFFFFFFFF, Settings.textShadow);
        }
        g.text(mc.font, t.label, t.x + (t.w - mc.font.width(t.label)) / 2, t.y + t.h - 12,
                0xFFAAAAAA, Settings.textShadow);
    }

    private static void drawLines(GuiGraphicsExtractor g, Minecraft mc, Tile t) {
        g.fill(t.x, t.y, t.x + t.w, t.y + t.h, Settings.bgColor());
        g.fill(t.x, t.y, t.x + t.w, t.y + 2, t.accent);
        int y = t.y + 6;
        for (String line : t.lines) {
            g.text(mc.font, line, t.x + 7, y, 0xFFFFFFFF, Settings.textShadow);
            y += 11;
        }
        if (!t.label.isEmpty()) {
            g.fill(t.x + 6, y, t.x + t.w - 6, y + 1, 0x60FFFFFF);
            g.text(mc.font, t.label, t.x + 7, y + 3, t.accent, Settings.textShadow);
        }
    }

    private static void drawGear(GuiGraphicsExtractor g, Minecraft mc, Tile t) {
        if (t.gear.isEmpty()) {
            g.fill(t.x, t.y, t.x + t.w, t.y + t.h, Settings.bgColor());
            g.fill(t.x, t.y, t.x + 2, t.y + t.h, t.accent == 0xFFFFFFFF ? Settings.accentColor() : t.accent);
            g.text(mc.font, "Armor & Item", t.x + 8, t.y + 6, 0xFFAAAAAA, false);
            return;
        }
        int rowH = 18, gap = 2, barW = 36;
        int y = t.y;
        for (ItemStack st : t.gear) {
            int stripe = Settings.accentColor();
            int bx = t.x + 6 + 16 + 5;
            g.fill(t.x, y, t.x + t.w, y + rowH, Settings.bgColor());

            if (st.isDamageableItem()) {
                int max = Math.max(1, st.getMaxDamage());
                double frac = Math.max(0.0, Math.min(1.0, (double) (max - st.getDamageValue()) / max));
                stripe = frac > 0.5 ? 0xFF55FF55 : (frac > 0.2 ? 0xFFFFFF55 : 0xFFFF5555);
                g.fill(bx, y + 7, bx + barW, y + 11, 0xFF222222);
                g.fill(bx, y + 7, bx + (int) Math.round(barW * frac), y + 11, stripe);
                g.text(mc.font, Math.round(frac * 100) + "%", bx + barW + 5,
                        y + (rowH - mc.font.lineHeight) / 2 + 1, 0xFFFFFFFF, Settings.textShadow);
            } else if (st.getCount() > 1) {
                g.text(mc.font, "x" + st.getCount(), bx, y + (rowH - mc.font.lineHeight) / 2 + 1,
                        0xFFFFFFFF, Settings.textShadow);
            }

            g.fill(t.x, y, t.x + 2, y + rowH, stripe);
            g.item(st, t.x + 6, y + 1);
            y += rowH + gap;
        }
    }

    private static void drawKey(GuiGraphicsExtractor g, Minecraft mc, String label, boolean down,
                                int x, int y, int w, int h) {
        int idle = Math.max(Settings.BG_ALPHA[Settings.background], 0x50) << 24;
        g.fill(x, y, x + w, y + h, down ? 0xB0FFFFFF : idle);
        int tx = x + (w - mc.font.width(label)) / 2;
        int ty = y + (h - mc.font.lineHeight) / 2 + 1;
        g.text(mc.font, label, tx, ty, down ? 0xFF000000 : 0xFFFFFFFF, false);
    }

    private static void drawKeys(GuiGraphicsExtractor g, Minecraft mc, Tile t) {
        int s = 20;
        int kx = t.x, ky = t.y;
        drawKey(g, mc, "W", mc.options.keyUp.isDown(), kx + s + 2, ky, s, s);
        drawKey(g, mc, "A", mc.options.keyLeft.isDown(), kx, ky + s + 2, s, s);
        drawKey(g, mc, "S", mc.options.keyDown.isDown(), kx + s + 2, ky + s + 2, s, s);
        drawKey(g, mc, "D", mc.options.keyRight.isDown(), kx + 2 * (s + 2), ky + s + 2, s, s);
        if (t.showMouse) {
            drawKey(g, mc, "LMB", mc.options.keyAttack.isDown(), kx, ky + 2 * (s + 2), 31, 18);
            drawKey(g, mc, "RMB", mc.options.keyUse.isDown(), kx + 33, ky + 2 * (s + 2), 31, 18);
        }
    }

    private static void drawPotions(GuiGraphicsExtractor g, Minecraft mc, Tile t) {
        g.fill(t.x, t.y, t.x + t.w, t.y + t.h, Settings.bgColor());
        if (t.lines.isEmpty()) {
            g.fill(t.x, t.y, t.x + 2, t.y + t.h, Settings.accentColor());
            g.text(mc.font, "Potion Effects", t.x + 8, t.y + 6, 0xFFAAAAAA, false);
            return;
        }
        boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
        int y = t.y + 3;
        for (int i = 0; i < t.lines.size(); i++) {
            int sec = t.secs.get(i);
            int timeColor = sec < 10 ? (blink ? 0xFFFF5555 : 0xFFAA2222) : (sec < 30 ? 0xFFFFFF55 : 0xFFFFFFFF);
            g.fill(t.x, y, t.x + 3, y + 11, 0xFF000000 | t.colors.get(i));
            g.text(mc.font, t.lines.get(i), t.x + 8, y + 2, 0xFFFFFFFF, Settings.textShadow);
            String time = t.right.get(i);
            g.text(mc.font, time, t.x + t.w - 6 - mc.font.width(time), y + 2, timeColor, Settings.textShadow);
            y += 12;
        }
    }

    // ------------------------------------------------------------------ item counters beside the hotbar
    private static ItemStack stackTotem, stackPearl, stackCrystal, stackAnchor;

    private static void tally(int[] c, ItemStack st) {
        if (st.isEmpty()) return;
        if (st.is(Items.TOTEM_OF_UNDYING)) c[0] += st.getCount();
        else if (st.is(Items.ENDER_PEARL)) c[1] += st.getCount();
        else if (st.is(Items.END_CRYSTAL)) c[2] += st.getCount();
        else if (st.is(Items.RESPAWN_ANCHOR)) c[3] += st.getCount();
    }

    private static void drawCounter(GuiGraphicsExtractor g, Minecraft mc, int x, int y, int w, ItemStack icon, int count) {
        int idle = Math.max(Settings.BG_ALPHA[Settings.background], 0x50) << 24;
        g.fill(x, y, x + w, y + 20, idle);
        g.fill(x, y + 18, x + w, y + 20, count > 0 ? Settings.accentColor() : 0xFFAA3333);
        g.item(icon, x + 4, y + 1);
        g.text(mc.font, String.valueOf(count), x + 4 + 16 + 4, y + 6,
                count > 0 ? 0xFFFFFFFF : 0xFFFF6B6B, Settings.textShadow);
    }

    private static void drawCounters(GuiGraphicsExtractor g, Minecraft mc, int sw, int sh) {
        if (stackTotem == null) {
            stackTotem = new ItemStack(Items.TOTEM_OF_UNDYING);
            stackPearl = new ItemStack(Items.ENDER_PEARL);
            stackCrystal = new ItemStack(Items.END_CRYSTAL);
            stackAnchor = new ItemStack(Items.RESPAWN_ANCHOR);
        }
        int[] c = new int[4];
        var inv = mc.player.getInventory();
        int n = Math.min(inv.getContainerSize(), 36);
        for (int i = 0; i < n; i++) tally(c, inv.getItem(i));
        tally(c, mc.player.getOffhandItem());

        ItemStack[] icons = {stackTotem, stackPearl, stackCrystal, stackAnchor};
        boolean zeros = Settings.opt(Settings.COUNTERS, 4) == 0;
        int y = sh - 22;
        int lx = sw / 2 - 91 - 38;   // left of the hotbar (past the off-hand slot)
        int rx = sw / 2 + 91 + 38;   // right of the hotbar
        for (int k = 0; k < 4; k++) {
            if (Settings.opt(Settings.COUNTERS, k) != 0) continue;
            if (c[k] == 0 && !zeros) continue;
            int w = 4 + 16 + 4 + mc.font.width(String.valueOf(c[k])) + 6;
            if (k < 2) {
                drawCounter(g, mc, lx - w, y, w, icons[k], c[k]);
                lx -= w + 3;
            } else {
                drawCounter(g, mc, rx, y, w, icons[k], c[k]);
                rx += w + 3;
            }
        }
    }

    // ------------------------------------------------------------------ AppleSkin-style saturation
    private static void drawSaturation(GuiGraphicsExtractor g, Minecraft mc, int sw, int sh) {
        if (mc.player.isCreative() || mc.player.isSpectator()) return;
        var food = mc.player.getFoodData();
        int foodLevel = food.getFoodLevel();
        float sat = food.getSaturationLevel();
        int right = sw / 2 + 91;
        int y = sh - 43;                         // a slim meter just above the hunger icons

        boolean preview = false;
        int gainFood = 0;
        float gainSat = 0f;
        if (Settings.opt(Settings.SATURATION, 0) == 0) {
            var fp = mc.player.getMainHandItem().get(DataComponents.FOOD);
            if (fp != null) {
                preview = true;
                gainFood = fp.nutrition();
                gainSat = fp.saturation();
            }
        }
        if (sat <= 0f && !preview) return;

        float newFood = Math.min(20, foodLevel + gainFood);
        float newSat = Math.min(newFood, sat + gainSat);

        // one segment per hunger icon, each worth 2 saturation points
        for (int k = 0; k < 10; k++) {
            int x = right - 9 - k * 8;
            g.fill(x, y, x + 7, y + 3, 0x80000000);
            double have = Math.max(0.0, Math.min(1.0, sat / 2.0 - k));
            double after = Math.max(0.0, Math.min(1.0, newSat / 2.0 - k));
            if (preview && after > have) g.fill(x, y, x + (int) Math.round(7 * after), y + 3, 0xA0FFF2A8);
            if (have > 0) g.fill(x, y, x + (int) Math.round(7 * have), y + 3, 0xFFFFD84A);
        }

        if (preview) {
            String text = "+" + gainFood + " food   +" + String.format(Locale.ROOT, "%.1f", gainSat) + " sat";
            g.text(mc.font, text, right - mc.font.width(text), y - 12, 0xFFFFE680, true);
        }
    }

    // ------------------------------------------------------------------ entry point
    public static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        try {
            int sw = mc.getWindow().getGuiScaledWidth();
            int sh = mc.getWindow().getGuiScaledHeight();
            long now = System.currentTimeMillis();

            if (now < noticeUntil) {
                int w = mc.font.width(noticeText);
                int x = (sw - w) / 2;
                int y = sh - 64;
                g.fill(x - 6, y - 3, x + w + 6, y + 11, 0xA0000000);
                g.fill(x - 6, y - 3, x - 4, y + 11, Settings.accentColor());
                g.text(mc.font, noticeText, x, y, 0xFFFFFFFF, true);
            }
            if (!visible) return;

            List<Tile> tiles = build(mc, false);
            place(tiles, sw, sh);
            for (Tile t : tiles) drawTile(g, mc, t);
            if (Settings.on(Settings.SATURATION)) drawSaturation(g, mc, sw, sh);
            if (Settings.on(Settings.COUNTERS)) drawCounters(g, mc, sw, sh);
        } catch (Exception ignored) {
            // never let the HUD take the game down or stop drawing
        }
    }
}
