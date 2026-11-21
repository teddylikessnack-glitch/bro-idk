package com.example.myclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Set;

public final class UiStyle {
    // Buttons that get the "cool yellow" look.
    private static final Set<String> PRIMARY = Set.of(
            "menu.singleplayer", "menu.multiplayer",
            "selectWorld.select", "selectWorld.create",
            "selectServer.select", "selectServer.add");

    private static final Set<String> MAIN_MENU = Set.of(
            "menu.singleplayer", "menu.multiplayer", "menu.options", "menu.quit");

    private UiStyle() {}

    public static String key(AbstractWidget w) {
        return w.getMessage().getContents() instanceof TranslatableContents tc ? tc.getKey() : "";
    }

    public static boolean isMainMenuButton(String key) {
        return MAIN_MENU.contains(key);
    }

    public static void paint(GuiGraphicsExtractor g, Minecraft mc, AbstractWidget w, int mx, int my) {
        int x = w.getX(), y = w.getY(), bw = w.getWidth(), bh = w.getHeight();
        String label = w.getMessage().getString();
        boolean hover = w.active && mx >= x && mx < x + bw && my >= y && my < y + bh;
        boolean primary = PRIMARY.contains(key(w));
        int ty = y + (bh - mc.font.lineHeight) / 2 + 1;
        int tx = x + (bw - mc.font.width(label)) / 2;

        if (!w.active) {
            g.fill(x, y, x + bw, y + bh, 0x80101018);
            g.text(mc.font, label, tx, ty, 0xFF777777, false);
        } else if (primary) {
            g.fillGradient(x, y, x + bw, y + bh, hover ? 0xFFFFF2A8 : 0xFFFFE680,
                    hover ? 0xFFFFC62E : 0xFFFFB400);
            g.fill(x, y, x + bw, y + 1, 0xFFFFF8D0);              // top highlight
            g.fill(x, y + bh - 1, x + bw, y + bh, 0xFFB07800);    // bottom edge
            g.fill(x, y, x + 1, y + bh, 0xFFE59E00);              // sides
            g.fill(x + bw - 1, y, x + bw, y + bh, 0xFFE59E00);
            g.fill(x + 1, y + 1, x + bw - 1, y + bh / 2, 0x28FFFFFF); // shine
            g.text(mc.font, label, tx, ty, 0xFF3A2400, false);
        } else {
            int accent = Settings.accentColor();
            g.fill(x, y, x + bw, y + bh, hover ? 0xD0303048 : 0xB0141420);
            g.fill(x, y, x + 3, y + bh, hover ? accent : 0xFF555566);
            if (hover) g.fill(x, y + bh - 1, x + bw, y + bh, accent);
            g.text(mc.font, label, tx, ty, 0xFFFFFFFF, true);
        }
    }

    // Restyles every real button on a screen (used for the world list and server list screens).
    public static void paintAllButtons(Minecraft mc, Screen screen, GuiGraphicsExtractor g, int mx, int my) {
        for (GuiEventListener l : screen.children()) {
            if (l instanceof Button b && b.visible && !b.getMessage().getString().isEmpty()) {
                paint(g, mc, b, mx, my);
            }
        }
    }
}
