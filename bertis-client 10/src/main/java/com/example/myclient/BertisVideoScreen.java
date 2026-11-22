package com.example.myclient;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ParticleStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

// A clean replacement for the vanilla Video Settings screen.
public class BertisVideoScreen extends Screen {
    private record Setting(String name, String desc, Supplier<String> value,
                           Runnable inc, Runnable dec, boolean toggle) {}

    private record Card(int x, int y, int w, int h, Setting s) {}

    private static final int[] FPS_STEPS = {30, 60, 90, 120, 144, 165, 200, 240, 260};

    private final Screen parent;
    private final List<Card> cards = new ArrayList<>();
    private int panelX, panelY, panelW, panelH, doneX, doneY;

    public BertisVideoScreen(Screen parent) {
        super(Component.literal("Video Settings"));
        this.parent = parent;
    }

    // ---------------------------------------------------------------- setting builders
    private static Setting ints(String name, String desc, OptionInstance<Integer> o, int min, int max, int step,
                                Function<Integer, String> fmt) {
        return new Setting(name, desc, () -> fmt.apply(o.get()),
                () -> o.set(Math.min(max, o.get() + step)),
                () -> o.set(Math.max(min, o.get() - step)), false);
    }

    private static Setting dbls(String name, String desc, OptionInstance<Double> o, double min, double max,
                                double step, Function<Double, String> fmt) {
        return new Setting(name, desc, () -> fmt.apply(o.get()),
                () -> o.set(Math.min(max, Math.round((o.get() + step) * 1000.0) / 1000.0)),
                () -> o.set(Math.max(min, Math.round((o.get() - step) * 1000.0) / 1000.0)), false);
    }

    private static Setting bool(String name, String desc, OptionInstance<Boolean> o) {
        Runnable flip = () -> o.set(!o.get());
        return new Setting(name, desc, () -> o.get() ? "ON" : "OFF", flip, flip, true);
    }

    private static String pretty(String s) {
        return s.isEmpty() ? s : s.charAt(0) + s.substring(1).toLowerCase();
    }

    private static <E extends Enum<E>> Setting enums(String name, String desc, OptionInstance<E> o, E[] vals) {
        return new Setting(name, desc, () -> pretty(o.get().name()),
                () -> o.set(vals[(o.get().ordinal() + 1) % vals.length]),
                () -> o.set(vals[(o.get().ordinal() + vals.length - 1) % vals.length]), false);
    }

    private static Setting fps(OptionInstance<Integer> o) {
        return new Setting("Max Framerate", "Frame limit", () -> o.get() >= 260 ? "Unlimited" : o.get() + " FPS",
                () -> {
                    for (int v : FPS_STEPS) if (v > o.get()) { o.set(v); return; }
                },
                () -> {
                    for (int i = FPS_STEPS.length - 1; i >= 0; i--) if (FPS_STEPS[i] < o.get()) { o.set(FPS_STEPS[i]); return; }
                }, false);
    }

    // ---------------------------------------------------------------- layout
    @Override
    protected void init() {
        cards.clear();
        Options o = Minecraft.getInstance().options;

        List<Setting> list = new ArrayList<>();
        list.add(ints("Render Distance", "Chunks you can see", o.renderDistance(), 2, 32, 1, v -> v + " chunks"));
        list.add(ints("Simulation", "Chunks that tick", o.simulationDistance(), 5, 32, 1, v -> v + " chunks"));
        list.add(fps(o.framerateLimit()));
        list.add(bool("VSync", "Sync to monitor", o.enableVsync()));
        list.add(ints("FOV", "Field of view", o.fov(), 30, 110, 5, v -> String.valueOf(v)));
        list.add(dbls("Brightness", "Gamma", o.gamma(), 0.0, 1.0, 0.1, v -> Math.round(v * 100) + "%"));
        list.add(ints("GUI Scale", "Menu size", o.guiScale(), 0, 4, 1, v -> v == 0 ? "Auto" : String.valueOf(v)));
        list.add(enums("Clouds", "Sky clouds", o.cloudStatus(), CloudStatus.values()));
        list.add(enums("Particles", "Effects", o.particles(), ParticleStatus.values()));
        list.add(bool("Entity Shadows", "Mob shadows", o.entityShadows()));
        list.add(dbls("Entity Distance", "Mob draw range", o.entityDistanceScaling(), 0.5, 5.0, 0.25,
                v -> Math.round(v * 100) + "%"));
        list.add(ints("Biome Blend", "Color smoothing", o.biomeBlendRadius(), 0, 7, 1, v -> String.valueOf(v)));
        list.add(bool("View Bobbing", "Camera sway", o.bobView()));

        panelW = Math.min(this.width - 16, 480);
        panelH = Math.min(this.height - 16, 280);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        doneX = panelX + panelW - 12 - 70;
        doneY = panelY + 10;
        this.addRenderableWidget(Button.builder(Component.empty(), b -> this.onClose())
                .bounds(doneX, doneY, 70, 20).build());

        int cols = panelW >= 440 ? 4 : (panelW >= 330 ? 3 : 2);
        int gap = 8;
        int cw = (panelW - 24 - (cols - 1) * gap) / cols;
        int top = panelY + 44;
        int rows = (list.size() + cols - 1) / cols;
        int avail = panelH - 44 - 14;
        int ch = Math.max(36, Math.min(52, avail / rows - gap));

        for (int i = 0; i < list.size(); i++) {
            Setting s = list.get(i);
            int x = panelX + 12 + (i % cols) * (cw + gap);
            int y = top + (i / cols) * (ch + gap);
            cards.add(new Card(x, y, cw, ch, s));
            if (s.toggle()) {
                this.addRenderableWidget(Button.builder(Component.empty(), b -> s.inc().run())
                        .bounds(x, y, cw, ch).build());
            } else {
                this.addRenderableWidget(Button.builder(Component.empty(), b -> s.dec().run())
                        .bounds(x, y, cw / 2, ch).build());
                this.addRenderableWidget(Button.builder(Component.empty(), b -> s.inc().run())
                        .bounds(x + cw / 2, y, cw - cw / 2, ch).build());
            }
        }
    }

    private static boolean inside(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String fit(String s, int maxW) {
        while (s.length() > 1 && this.font.width(s) > maxW) s = s.substring(0, s.length() - 1);
        return s;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) TitleOverlay.drawScene(mc, g);
        int accent = Settings.accentColor();

        g.fill(0, 0, this.width, this.height, 0x60000000);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xB0101018);
        g.fill(panelX, panelY, panelX + panelW, panelY + 2, accent);

        Branding.drawDuck(g, panelX + 10, panelY + 5, 2);
        g.text(this.font, "Video Settings", panelX + 50, panelY + 11, accent, true);
        g.text(this.font, "Click the left or right side of a card", panelX + 50, panelY + 23, 0xFFAAAAAA, false);
        UiStyle.paintRect(g, mc, doneX, doneY, 70, 20, "Done", inside(doneX, doneY, 70, 20, mx, my), true, true);

        for (Card c : cards) {
            Setting s = c.s();
            boolean hover = inside(c.x(), c.y(), c.w(), c.h(), mx, my);
            boolean leftHalf = mx < c.x() + c.w() / 2;
            String v = s.value().get();
            boolean on = !s.toggle() || v.equals("ON");

            g.fill(c.x(), c.y(), c.x() + c.w(), c.y() + c.h(), hover ? 0xA0383848 : 0x90202030);
            g.fill(c.x(), c.y(), c.x() + c.w(), c.y() + 2, on ? accent : 0xFF555566);
            if (hover && !s.toggle()) {
                int hx = leftHalf ? c.x() : c.x() + c.w() / 2;
                g.fill(hx, c.y() + 2, hx + c.w() / 2, c.y() + c.h(), 0x25FFD84A);
            }
            g.text(this.font, fit(s.name(), c.w() - 12), c.x() + 6, c.y() + 7, 0xFFFFFFFF, true);
            g.text(this.font, fit(s.desc(), c.w() - 12), c.x() + 6, c.y() + 18, 0xFF9999AA, false);

            int cy = c.y() + c.h() - 14;
            if (s.toggle()) {
                int pw = this.font.width(v) + 10;
                int px = c.x() + c.w() - pw - 6;
                g.fill(px, cy, px + pw, cy + 11, on ? accent : 0xFF444455);
                g.text(this.font, v, px + 5, cy + 2, on ? 0xFF000000 : 0xFFDDDDDD, false);
            } else {
                g.text(this.font, "<", c.x() + 6, cy + 2, leftHalf && hover ? accent : 0xFFAAAAAA, false);
                g.text(this.font, ">", c.x() + c.w() - 6 - this.font.width(">"), cy + 2,
                        !leftHalf && hover ? accent : 0xFFAAAAAA, false);
                String shown = fit(v, c.w() - 36);
                g.text(this.font, shown, c.x() + (c.w() - this.font.width(shown)) / 2, cy + 2, accent, true);
            }
        }
    }

    @Override
    public void onClose() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.save();
        mc.setScreenAndShow(parent);
    }
}
