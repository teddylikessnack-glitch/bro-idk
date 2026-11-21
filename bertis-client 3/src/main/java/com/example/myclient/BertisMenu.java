package com.example.myclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

// Lunar-style menu: translucent panel, tabs, big cards.
// The cards are drawn by hand; invisible vanilla buttons sit on top of them to handle clicks.
public class BertisMenu extends Screen {
    private static final String[] TABS = {"Mods", "Performance", "Customize"};
    private static int tab = 0;

    private record Card(int x, int y, int w, int h, String title, String desc,
                        Supplier<String> value, BooleanSupplier on) {}

    private final List<Card> cards = new ArrayList<>();
    private int panelX, panelY, panelW, panelH;
    private final int[] tabX = new int[3];
    private int tabY, tabW, tabH;

    public BertisMenu() {
        super(Component.literal("Bertis Client"));
    }

    private void addClickArea(int x, int y, int w, int h, Runnable action) {
        this.addRenderableWidget(Button.builder(Component.empty(), b -> action.run())
                .bounds(x, y, w, h).build());
    }

    @Override
    protected void init() {
        cards.clear();
        panelW = Math.min(this.width - 16, 420);
        panelH = Math.min(this.height - 16, 250);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        // Tabs
        tabH = 18;
        tabY = panelY + 40;
        tabW = (panelW - 24 - 2 * 6) / 3;
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            tabX[i] = panelX + 12 + i * (tabW + 6);
            addClickArea(tabX[i], tabY, tabW, tabH, () -> {
                tab = idx;
                this.rebuildWidgets();
            });
        }

        // Card list for the current tab
        List<Card> list = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        if (tab == 0) {
            for (int i = 0; i < Settings.HUD_NAMES.length; i++) {
                final int idx = i;
                list.add(new Card(0, 0, 0, 0, Settings.HUD_NAMES[i], Settings.HUD_DESC[i],
                        () -> Settings.HUD_ON[idx] ? "ON" : "OFF", () -> Settings.HUD_ON[idx]));
                actions.add(() -> Settings.HUD_ON[idx] = !Settings.HUD_ON[idx]);
            }
        } else if (tab == 1) {
            for (int i = 0; i < Settings.PERF_NAMES.length; i++) {
                final int idx = i;
                list.add(new Card(0, 0, 0, 0, Settings.PERF_NAMES[i], Settings.PERF_DESC[i],
                        () -> Settings.PERF_ON[idx] ? "ON" : "OFF", () -> Settings.PERF_ON[idx]));
                actions.add(() -> {
                    Settings.PERF_ON[idx] = !Settings.PERF_ON[idx];
                    Performance.apply(idx);
                });
            }
        } else {
            list.add(new Card(0, 0, 0, 0, "HUD Position", "Where the HUD sits",
                    () -> Settings.POSITION_NAMES[Settings.position], () -> true));
            actions.add(() -> Settings.position = (Settings.position + 1) % Settings.POSITION_NAMES.length);

            list.add(new Card(0, 0, 0, 0, "Accent Color", "Color of the HUD stripes",
                    () -> Settings.ACCENT_NAMES[Settings.accent], () -> true));
            actions.add(() -> Settings.accent = (Settings.accent + 1) % Settings.ACCENTS.length);

            list.add(new Card(0, 0, 0, 0, "Background", "HUD box transparency",
                    () -> Settings.BG_NAMES[Settings.background], () -> true));
            actions.add(() -> Settings.background = (Settings.background + 1) % Settings.BG_ALPHA.length);

            list.add(new Card(0, 0, 0, 0, "Text Shadow", "Shadow behind HUD text",
                    () -> Settings.textShadow ? "ON" : "OFF", () -> Settings.textShadow));
            actions.add(() -> Settings.textShadow = !Settings.textShadow);
        }

        // Layout
        int cols = panelW >= 400 ? 3 : (panelW >= 270 ? 2 : 1);
        int gap = 8;
        int cw = (panelW - 24 - (cols - 1) * gap) / cols;
        int top = panelY + 64;
        int rows = (list.size() + cols - 1) / cols;
        int avail = panelH - 64 - 22;
        int ch = Math.max(34, Math.min(52, avail / rows - gap));

        for (int i = 0; i < list.size(); i++) {
            Card c = list.get(i);
            int x = panelX + 12 + (i % cols) * (cw + gap);
            int y = top + (i / cols) * (ch + gap);
            cards.add(new Card(x, y, cw, ch, c.title(), c.desc(), c.value(), c.on()));
            final Runnable action = actions.get(i);
            addClickArea(x, y, cw, ch, () -> {
                action.run();
                Settings.save();
            });
        }
    }

    private static boolean inside(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        // Not calling super on purpose: no vanilla blur, so the game shows through.
        int accent = Settings.accentColor();

        g.fill(0, 0, this.width, this.height, 0x60000000);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xB0101018);
        g.fill(panelX, panelY, panelX + panelW, panelY + 2, accent);

        // Header
        Branding.drawDuck(g, panelX + 10, panelY + 5, 2);
        g.text(this.font, "Bertis Client", panelX + 50, panelY + 11, accent, true);
        g.text(this.font, "by CayL12", panelX + 50, panelY + 23, 0xFFAAAAAA, true);

        // Tabs
        for (int i = 0; i < 3; i++) {
            boolean active = i == tab;
            boolean hover = inside(tabX[i], tabY, tabW, tabH, mouseX, mouseY);
            int bg = active ? 0xB0303040 : (hover ? 0x80282833 : 0x60181820);
            g.fill(tabX[i], tabY, tabX[i] + tabW, tabY + tabH, bg);
            if (active) g.fill(tabX[i], tabY + tabH - 2, tabX[i] + tabW, tabY + tabH, accent);
            int tw = this.font.width(TABS[i]);
            g.text(this.font, TABS[i], tabX[i] + (tabW - tw) / 2, tabY + 5,
                    active ? 0xFFFFFFFF : 0xFFBBBBBB, true);
        }

        // Cards
        for (Card c : cards) {
            boolean hover = inside(c.x(), c.y(), c.w(), c.h(), mouseX, mouseY);
            boolean on = c.on().getAsBoolean();
            g.fill(c.x(), c.y(), c.x() + c.w(), c.y() + c.h(), hover ? 0xA0383848 : 0x90202030);
            g.fill(c.x(), c.y(), c.x() + c.w(), c.y() + 2, on ? accent : 0xFF555566);
            g.text(this.font, c.title(), c.x() + 8, c.y() + 8, 0xFFFFFFFF, true);
            g.text(this.font, c.desc(), c.x() + 8, c.y() + 20, 0xFF9999AA, false);

            String v = c.value().get();
            int pw = this.font.width(v) + 10;
            int px = c.x() + c.w() - pw - 6;
            int py = c.y() + c.h() - 16;
            g.fill(px, py, px + pw, py + 11, on ? accent : 0xFF444455);
            g.text(this.font, v, px + 5, py + 2, on ? 0xFF000000 : 0xFFDDDDDD, false);
        }

        // Footer
        String hint = "Right Ctrl: show/hide HUD   |   Esc: close";
        g.text(this.font, hint, this.width / 2 - this.font.width(hint) / 2,
                panelY + panelH - 14, 0xFF888899, false);
    }

    @Override
    public void onClose() {
        Settings.save();
        super.onClose();
    }
}
