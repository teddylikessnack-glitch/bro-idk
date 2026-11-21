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
    // Brightest yellow for the main actions.
    private static final Set<String> PRIMARY = Set.of(
            "menu.singleplayer", "menu.multiplayer",
            "selectWorld.select", "selectWorld.create",
            "selectServer.select", "selectServer.add",
            "menu.returnToGame");

    // Only buttons whose label is a vanilla translation key get restyled. Buttons from other
    // mods (Essential etc.) use their own labels, so they are left completely alone.
    private static final String[] VANILLA_PREFIXES = {
            "menu.", "gui.", "selectWorld.", "selectServer.", "options.", "createWorld.",
            "mco.", "addServer.", "controls.", "pack.", "lanServer.", "narrator.", "advancements."
    };

    private UiStyle() {}

    public static String key(AbstractWidget w) {
        return w.getMessage().getContents() instanceof TranslatableContents tc ? tc.getKey() : "";
    }

    public static boolean shouldStyle(AbstractWidget w) {
        if (!(w instanceof Button) || !w.visible) return false;
        if (w.getWidth() < 60 || w.getClass().getSimpleName().contains("Icon")) return false;
        String k = key(w);
        for (String p : VANILLA_PREFIXES) if (k.startsWith(p)) return true;
        return false;
    }

    public static void paint(GuiGraphicsExtractor g, Minecraft mc, AbstractWidget w, int mx, int my) {
        int x = w.getX(), y = w.getY(), bw = w.getWidth(), bh = w.getHeight();
        String label = w.getMessage().getString();
        boolean hover = w.active && mx >= x && mx < x + bw && my >= y && my < y + bh;
        boolean primary = PRIMARY.contains(key(w));
        int ty = y + (bh - mc.font.lineHeight) / 2 + 1;
        int tx = x + (bw - mc.font.width(label)) / 2;

        if (!w.active) {
            g.fill(x, y, x + bw, y + bh, 0xB0303020);
            g.fill(x, y + bh - 1, x + bw, y + bh, 0xFF55503A);
            g.text(mc.font, label, tx, ty, 0xFF8A8670, false);
            return;
        }

        int top, bottom;
        if (primary) {
            top = hover ? 0xFFFFF2A8 : 0xFFFFE680;
            bottom = hover ? 0xFFFFC62E : 0xFFFFB400;
        } else {
            top = hover ? 0xFFFFE680 : 0xFFFFD84A;
            bottom = hover ? 0xFFFFB400 : 0xFFE6A800;
        }
        g.fillGradient(x, y, x + bw, y + bh, top, bottom);
        g.fill(x, y, x + bw, y + 1, 0xFFFFF8D0);              // top highlight
        g.fill(x, y + bh - 1, x + bw, y + bh, 0xFFB07800);    // bottom edge
        g.fill(x, y, x + 1, y + bh, 0xFFE59E00);              // sides
        g.fill(x + bw - 1, y, x + bw, y + bh, 0xFFE59E00);
        g.fill(x + 1, y + 1, x + bw - 1, y + bh / 2, 0x28FFFFFF); // shine
        g.text(mc.font, label, tx, ty, 0xFF3A2400, false);
    }

    public static void paintAllButtons(Minecraft mc, Screen screen, GuiGraphicsExtractor g, int mx, int my) {
        for (GuiEventListener l : screen.children()) {
            if (l instanceof AbstractWidget w && shouldStyle(w)) paint(g, mc, w, mx, my);
        }
    }
}
