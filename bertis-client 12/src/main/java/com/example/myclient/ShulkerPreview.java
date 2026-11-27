package com.example.myclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.List;

// Hover a shulker box (or any item with stored contents) in an inventory to see what's inside.
public final class ShulkerPreview {
    private static Field hoveredField;
    private static boolean failed;

    private ShulkerPreview() {}

    private static Slot hovered(AbstractContainerScreen<?> screen) {
        try {
            if (hoveredField == null) {
                for (Class<?> c = screen.getClass(); c != null && hoveredField == null; c = c.getSuperclass()) {
                    try {
                        hoveredField = c.getDeclaredField("hoveredSlot");
                    } catch (NoSuchFieldException ignored) {
                    }
                }
                if (hoveredField == null) {
                    failed = true;
                    return null;
                }
                hoveredField.setAccessible(true);
            }
            return (Slot) hoveredField.get(screen);
        } catch (Exception e) {
            failed = true;
            return null;
        }
    }

    public static void draw(Minecraft mc, AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int mx, int my) {
        if (failed || !Settings.on(Settings.SHULKER)) return;
        Slot slot = hovered(screen);
        if (slot == null || !slot.hasItem()) return;

        var contents = slot.getItem().get(DataComponents.CONTAINER);
        if (contents == null) return;
        List<ItemStack> items = contents.nonEmptyItemCopyStream().toList();
        if (items.isEmpty()) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int cols = Math.min(9, items.size());
        int rows = (items.size() + 8) / 9;
        int w = cols * 18 + 10;
        int h = rows * 18 + 22;
        int x = mx - w - 10;
        if (x < 4) x = Math.min(sw - w - 4, mx + 14);
        int y = Math.max(4, Math.min(sh - h - 4, my - 10));
        int accent = Settings.accentColor();

        g.fill(x, y, x + w, y + h, 0xF0101018);
        g.fill(x, y, x + w, y + 2, accent);
        g.text(mc.font, "Contents (" + items.size() + ")", x + 5, y + 6, accent, true);
        for (int i = 0; i < items.size(); i++) {
            ItemStack st = items.get(i);
            int cx = x + 5 + (i % 9) * 18;
            int cy = y + 17 + (i / 9) * 18;
            g.fill(cx, cy, cx + 17, cy + 17, 0x40FFFFFF);
            g.item(st, cx + 1, cy + 1);
            g.itemDecorations(mc.font, st, cx + 1, cy + 1);
        }
    }
}
