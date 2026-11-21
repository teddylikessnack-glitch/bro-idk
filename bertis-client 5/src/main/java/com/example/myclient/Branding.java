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
    private static final String FEET_STEP = "....OO....OO....";

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
        drawDuckFrame(g, x, y, scale, 0);
    }

    // frame 1 = a little hop with the feet moved (used for the walking animation)
    public static void drawDuckFrame(GuiGraphicsExtractor g, int x, int y, int scale, int frame) {
        for (int r = 0; r < DUCK.length; r++) {
            String row = DUCK[r];
            if (r == 14 && frame == 1) row = FEET_STEP;
            int yy = y + r * scale - (frame == 1 ? scale : 0);
            for (int c = 0; c < row.length(); c++) {
                int col = color(row.charAt(c));
                if (col != 0) g.fill(x + c * scale, yy, x + (c + 1) * scale, yy + scale, col);
            }
        }
    }

    // Main menu: custom scene + styled buttons + logo. Inventories: logo bottom-right.
    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen) {
                ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickProgress) -> {
                    TitleOverlay.draw(client, s, graphics, mouseX, mouseY);
                    drawBrand(client, graphics, true);
                });
            } else if (screen instanceof AbstractContainerScreen<?>) {
                ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickProgress) ->
                        drawBrand(client, graphics, false));
            } else {
                String n = screen.getClass().getSimpleName();
                if (n.equals("SelectWorldScreen") || n.equals("JoinMultiplayerScreen")) {
                    ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickProgress) ->
                            UiStyle.paintAllButtons(client, s, graphics, mouseX, mouseY));
                }
            }
        });
    }

    // "duck + Bertis Client" in the bottom-right corner.
    private static void drawBrand(Minecraft mc, GuiGraphicsExtractor g, boolean title) {
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        if (sh < 150) return;

        int scale = title && sh >= 260 ? 2 : 1;
        int duckPx = 16 * scale;
        String name = "Bertis Client";
        String by = "by CayL12";
        int textW = Math.max(mc.font.width(name), mc.font.width(by));
        int total = duckPx + 8 + textW;
        int x = sw - total - 14;
        int y = sh - duckPx - (title ? 22 : 12);

        g.fill(x - 8, y - 4, x + total + 8, y + duckPx + 4, 0x70000000);
        g.fill(x + total + 6, y - 4, x + total + 8, y + duckPx + 4, Settings.accentColor());
        drawDuck(g, x, y, scale);
        int tx = x + duckPx + 8;
        int mid = y + duckPx / 2;
        g.text(mc.font, name, tx, mid - mc.font.lineHeight - 1, Settings.accentColor(), true);
        g.text(mc.font, by, tx, mid + 2, 0xFFAAAAAA, true);
    }
}
