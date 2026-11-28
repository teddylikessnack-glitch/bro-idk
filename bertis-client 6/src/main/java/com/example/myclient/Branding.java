package com.example.myclient;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;

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

    // used to know which screen the Video Settings screen was opened from
    private static Screen currentScreen, previousScreen;

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

    private static boolean isOurs(Screen s) {
        return s.getClass().getName().startsWith("com.example.myclient");
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen != currentScreen) {
                previousScreen = currentScreen;
                currentScreen = screen;
            }

            // Replace the vanilla Video Settings screen with our clean one.
            if (Settings.customMenus && screen.getClass().getSimpleName().equals("VideoSettingsScreen")) {
                Screen parent = previousScreen;
                client.execute(() -> client.setScreenAndShow(new BertisVideoScreen(parent)));
                return;
            }

            if (screen instanceof TitleScreen) {
                ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickProgress) -> {
                    if (Settings.customMenus) TitleOverlay.draw(client, s, graphics, mouseX, mouseY);
                    drawBrand(client, graphics, true);
                    pollMenuKey(client, s);
                });
            } else if (screen instanceof AbstractContainerScreen<?> container) {
                ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickProgress) -> {
                    drawBrand(client, graphics, false);
                    ShulkerPreview.draw(client, container, graphics, mouseX, mouseY);
                });
            } else if (!isOurs(screen)) {
                // yellow buttons on every other screen (pause menu, world list, options...)
                ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickProgress) -> {
                    if (Settings.customMenus) UiStyle.paintAllButtons(client, s, graphics, mouseX, mouseY);
                    pollMenuKey(client, s);
                });
            }
        });
    }

    // Right Shift opens the menu on Minecraft's own screens (main menu, pause menu, options...) too.
    // Key bindings don't run while a screen is open, so the key is polled here.
    private static boolean shiftWasDown;

    // Set while a screen is being drawn; the actual screen change happens on the next tick.
    // (Switching screens in the middle of a draw is what made the old version flicker.)
    public static volatile boolean pendingMenu;

    private static void pollMenuKey(Minecraft mc, Screen screen) {
        long handle = GLFW.glfwGetCurrentContext();
        boolean down = handle != 0L && GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean pressed = down && !shiftWasDown;
        shiftWasDown = down;
        if (!pressed) return;
        if (screen.getFocused() instanceof EditBox) return;          // typing in a text box
        String n = screen.getClass().getSimpleName();
        if (n.contains("Sign") || n.contains("Book") || n.contains("Chat") || n.contains("Command")) return;
        pendingMenu = true;
    }

    // "duck + Bertis Client" in the bottom-right corner.
    private static void drawBrand(Minecraft mc, GuiGraphicsExtractor g, boolean title) {
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        if (sh < 150) return;

        int scale = title && sh >= 260 ? 2 : 1;
        int duckPx = 16 * scale;
        String name = "Bertis Client";
        int total = duckPx + 8 + mc.font.width(name);
        int x = sw - total - 14;
        int y = sh - duckPx - (title ? 22 : 12);

        g.fill(x - 8, y - 4, x + total + 8, y + duckPx + 4, 0x70000000);
        g.fill(x + total + 6, y - 4, x + total + 8, y + duckPx + 4, Settings.accentColor());
        drawDuck(g, x, y, scale);
        g.text(mc.font, name, x + duckPx + 8, y + (duckPx - mc.font.lineHeight) / 2 + 1,
                Settings.accentColor(), true);
    }
}
