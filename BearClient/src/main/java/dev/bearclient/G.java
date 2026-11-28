package dev.bearclient;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Thin wrapper around GuiGraphicsExtractor so version-specific names are in one place. */
final class G {
    private G() {}

    static void fill(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int argb) {
        g.fill(x1, y1, x2, y2, argb);
    }

    static void gradient(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int from, int to) {
        g.fillGradient(x1, y1, x2, y2, from, to);
    }

    static void text(GuiGraphicsExtractor g, Font f, String s, int x, int y, int argb) {
        g.text(f, s, x, y, argb, true);
    }

    static void text(GuiGraphicsExtractor g, Font f, Component c, int x, int y, int argb) {
        g.text(f, c, x, y, argb, true);
    }

    static void centered(GuiGraphicsExtractor g, Font f, String s, int cx, int y, int argb) {
        g.text(f, s, cx - f.width(s) / 2, y, argb, true);
    }

    static void centered(GuiGraphicsExtractor g, Font f, Component c, int cx, int y, int argb) {
        g.text(f, c, cx - f.width(c) / 2, y, argb, true);
    }

    static void item(GuiGraphicsExtractor g, ItemStack s, int x, int y) { g.item(s, x, y); }

    static void itemDecorations(GuiGraphicsExtractor g, Font f, ItemStack s, int x, int y) {
        g.itemDecorations(f, s, x, y);
    }

    static void pushScaled(GuiGraphicsExtractor g, float tx, float ty, float scale) {
        g.pose().pushMatrix();
        g.pose().translate(tx, ty);
        g.pose().scale(scale, scale);
    }

    static void pop(GuiGraphicsExtractor g) { g.pose().popMatrix(); }

    /** "ResourceKey[minecraft:worldgen/biome / minecraft:plains]" -> "plains" (works regardless of renames). */
    static String shortKey(Object key) {
        String s = String.valueOf(key);
        int i = s.lastIndexOf(" / ");
        if (i >= 0) s = s.substring(i + 3);
        s = s.replace("]", "");
        int c = s.indexOf(':');
        return c >= 0 ? s.substring(c + 1) : s;
    }
}
