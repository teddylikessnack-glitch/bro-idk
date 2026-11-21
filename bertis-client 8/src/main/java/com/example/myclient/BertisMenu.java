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
// Cards are drawn by hand; invisible vanilla buttons sit on top of them to handle clicks.
public class BertisMenu extends Screen {
    private static final String[] TABS = {"Mods", "Performance", "Customize"};
    private static int tab = 0;
    private static int detail = -1; // module whose options are open (Mods tab), or -1

    private record Item(String title, String desc, Supplier<String> value, BooleanSupplier on,
                        Runnable action, Runnable gear) {}

    private record Card(int x, int y, int w, int h, String title, String desc,
                        Supplier<String> value, BooleanSupplier on, boolean gear) {}

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

    private static Item toggle(String title, String desc, boolean[] arr, int idx, Runnable extra, Runnable gear) {
        return new Item(title, desc, () -> arr[idx] ? "ON" : "OFF", () -> arr[idx], () -> {
            arr[idx] = !arr[idx];
            if (extra != null) extra.run();
        }, gear);
    }

    private static Item cycle(String title, String desc, Supplier<String> value, Runnable action) {
        return new Item(title, desc, value, () -> true, action, null);
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
                detail = -1;
                this.rebuildWidgets();
            });
        }

        // Items for the current view
        List<Item> items = new ArrayList<>();
        if (tab == 0 && detail >= 0) {
            final int m = detail;
            items.add(toggle("Enabled", "Show on the HUD", Settings.HUD_ON, m, null, null));
            for (int j = 0; j < Settings.OPT_NAMES[m].length; j++) {
                final int o = j;
                items.add(cycle(Settings.OPT_NAMES[m][o], "Click to change",
                        () -> Settings.OPT_CHOICES[m][o][Settings.OPT[m][o]],
                        () -> Settings.OPT[m][o] = (Settings.OPT[m][o] + 1) % Settings.OPT_CHOICES[m][o].length));
            }
            items.add(cycle("Back", "Return to mods", () -> "<", () -> {
                detail = -1;
                this.rebuildWidgets();
            }));
        } else if (tab == 0) {
            for (int i = 0; i < Settings.HUD_NAMES.length; i++) {
                final int idx = i;
                Runnable gear = Settings.OPT_NAMES[i].length > 0 ? () -> {
                    detail = idx;
                    this.rebuildWidgets();
                } : null;
                items.add(toggle(Settings.HUD_NAMES[i], Settings.HUD_DESC[i], Settings.HUD_ON, i, null, gear));
            }
        } else if (tab == 1) {
            for (int i = 0; i < Settings.PERF_NAMES.length; i++) {
                final int idx = i;
                items.add(toggle(Settings.PERF_NAMES[i], Settings.PERF_DESC[i], Settings.PERF_ON, i,
                        () -> Performance.apply(idx), null));
            }
        } else {
            items.add(cycle("HUD Position", "Where the HUD sits",
                    () -> Settings.POSITION_NAMES[Settings.position],
                    () -> Settings.position = (Settings.position + 1) % Settings.POSITION_NAMES.length));
            items.add(cycle("Accent Color", "Color of the stripes",
                    () -> Settings.ACCENT_NAMES[Settings.accent],
                    () -> Settings.accent = (Settings.accent + 1) % Settings.ACCENTS.length));
            items.add(cycle("Background", "HUD box darkness",
                    () -> Settings.BG_NAMES[Settings.background],
                    () -> Settings.background = (Settings.background + 1) % Settings.BG_ALPHA.length));
            items.add(new Item("Text Shadow", "Shadow behind text",
                    () -> Settings.textShadow ? "ON" : "OFF", () -> Settings.textShadow,
                    () -> Settings.textShadow = !Settings.textShadow, null));
            items.add(new Item("Discord RPC", "Show on Discord",
                    () -> Settings.discordRpc ? "ON" : "OFF", () -> Settings.discordRpc,
                    () -> Settings.discordRpc = !Settings.discordRpc, null));
            items.add(new Item("Custom Menus", "Duck scene + yellow",
                    () -> Settings.customMenus ? "ON" : "OFF", () -> Settings.customMenus,
                    () -> Settings.customMenus = !Settings.customMenus, null));
        }

        // Layout
        int cols = panelW >= 400 ? 3 : (panelW >= 270 ? 2 : 1);
        int gap = 8;
        int cw = (panelW - 24 - (cols - 1) * gap) / cols;
        int top = panelY + 64;
        int rows = (items.size() + cols - 1) / cols;
        int avail = panelH - 64 - 22;
        int ch = Math.max(34, Math.min(52, avail / rows - gap));

        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            int x = panelX + 12 + (i % cols) * (cw + gap);
            int y = top + (i / cols) * (ch + gap);
            cards.add(new Card(x, y, cw, ch, it.title(), it.desc(), it.value(), it.on(), it.gear() != null));

            Runnable click = () -> {
                it.action().run();
                Settings.save();
            };
            if (it.gear() != null) {
                int gw = 30, gh = 18;
                addClickArea(x, y, cw - gw, gh, click);
                addClickArea(x + cw - gw, y, gw, gh, it.gear());
                addClickArea(x, y + gh, cw, ch - gh, click);
            } else {
                addClickArea(x, y, cw, ch, click);
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
        if (tab == 0 && detail >= 0) {
            String editing = "Editing: " + Settings.HUD_NAMES[detail];
            g.text(this.font, editing, panelX + panelW - 12 - this.font.width(editing), panelY + 11, 0xFFFFFFFF, true);
        }

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

            int titleW = c.w() - 16 - (c.gear() ? 30 : 0);
            g.text(this.font, fit(c.title(), titleW), c.x() + 8, c.y() + 8, 0xFFFFFFFF, true);
            g.text(this.font, fit(c.desc(), c.w() - 16), c.x() + 8, c.y() + 20, 0xFF9999AA, false);

            if (c.gear()) {
                int gx = c.x() + c.w() - 28;
                boolean gHover = inside(c.x() + c.w() - 30, c.y(), 30, 18, mouseX, mouseY);
                g.fill(gx, c.y() + 5, gx + 24, c.y() + 16, gHover ? 0xFF55556A : 0xFF3A3A4C);
                g.text(this.font, "Edit", gx + (24 - this.font.width("Edit")) / 2, c.y() + 7, 0xFFFFFFFF, false);
            }

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
