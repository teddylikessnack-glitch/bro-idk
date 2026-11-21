package com.example.myclient;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class Branding {
    public static final String[] DUCK = {
            "................",
            ".......YYYY.....",
            "......YYYYYY....",
            "......YYKYYYOOO.",
            "......YYYYYYOOO.",
            "......YYYYYYOO..",
            ".......YYYYY....",
            ".......YYYY.....",
            "..Y...YYYYYY....",
            "..YYYYYYYYYYY...",
            ".YYYYYYyyYYYYY..",
            ".YYYYYyyyyYYYY..",
            "..YYYYyyyyYYY...",
            "...YYYYYYYYY....",
            ".....OO..OO.....",
            "................"
    };

    private Branding() {}

    private static int color(char c) {
        return switch (c) {
            case 'Y' -> 0xFFFFD700;
            case 'y' -> 0xFFE6AF00;
            case 'O' -> 0xFFFF8C00;
            case 'K' -> 0xFF000000;
            case 'W' -> 0xFFFFFFFF;
            default -> 0;
        };
    }

    public static void drawDuck(GuiGraphicsExtractor g, int x, int y, int scale) {
        for (int r = 0; r < DUCK.length; r++) {
            String row = DUCK[r];
            for (int c = 0; c < row.length(); c++) {
                int col = color(row.charAt(c));
                if (col != 0) g.fill(x + c * scale, y + r * scale, x + (c + 1) * scale, y + (r + 1) * scale, col);
            }
        }
    }

    // Draws "duck + Bertis Client" at the bottom of the main menu and of inventory screens.
    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen) {
                ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickProgress) ->
                        drawBrand(client, graphics, true));
            } else if (screen instanceof AbstractContainerScreen<?>) {
                ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickProgress) ->
                        drawBrand(client, graphics, false));
            }
        });
    }

    private static void drawBrand(Minecraft mc, GuiGraphicsExtractor g, boolean title) {
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        if (!title && sh < 250) return; // not enough room under the inventory

        int scale = title && sh >= 260 ? 2 : 1;
        int duckPx = 16 * scale;
        String name = "Bertis Client";
        int total = duckPx + 8 + mc.font.width(name);
        int x = (sw - total) / 2;
        int y = sh - duckPx - 8;

        g.fill(x - 8, y - 4, x + total + 8, y + duckPx + 4, 0x70000000);
        g.fill(x - 8, y - 4, x - 6, y + duckPx + 4, Settings.accentColor());
        drawDuck(g, x, y, scale);
        int tx = x + duckPx + 8;
        int mid = y + duckPx / 2;
        g.text(mc.font, name, tx, mid - mc.font.lineHeight - 1, Settings.accentColor(), true);
        g.text(mc.font, "by CayL12", tx, mid + 2, 0xFFAAAAAA, true);
    }
}
