package com.example.myclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

// Drag the real HUD boxes around. Mouse state is polled so it doesn't depend on input event types.
public class HudEditScreen extends Screen {
    private int dragging = -1, grabDX, grabDY;
    private boolean wasDown;

    public HudEditScreen() {
        super(Component.literal("Edit HUD"));
    }

    private int resetX, doneX, btnY;
    private static final int BW = 110, BH = 20;

    @Override
    protected void init() {
        wasDown = leftDown(Minecraft.getInstance());
        dragging = -1;
        btnY = this.height - 40;
        resetX = this.width / 2 - BW - 4;
        doneX = this.width / 2 + 4;
        this.addRenderableWidget(Button.builder(Component.empty(), b -> {
            Settings.resetLayout();
            Settings.save();
        }).bounds(resetX, btnY, BW, BH).build());
        this.addRenderableWidget(Button.builder(Component.empty(), b -> this.onClose())
                .bounds(doneX, btnY, BW, BH).build());
    }

    // While a screen is open the game stops updating its own mouse flags, so ask GLFW directly.
    private static boolean leftDown(Minecraft mc) {
        long handle = GLFW.glfwGetCurrentContext();
        if (handle != 0L && GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            return true;
        }
        return mc.mouseHandler.isLeftPressed();
    }

    private static boolean inside(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Minecraft mc = Minecraft.getInstance();
        int sw = this.width, sh = this.height;
        int accent = Settings.accentColor();

        if (mc.level == null) TitleOverlay.drawScene(mc, g);   // opened from the main menu
        g.fill(0, 0, sw, sh, 0x70000000);

        List<Hud.Tile> tiles = Hud.build(mc, true);
        Hud.place(tiles, sw, sh);

        boolean down = leftDown(mc);
        if (down && !wasDown) {
            dragging = -1;
            if (!inside(resetX, btnY, BW * 2 + 8, BH, mx, my)) {
                for (int i = tiles.size() - 1; i >= 0; i--) {
                    Hud.Tile t = tiles.get(i);
                    if (inside(t.x, t.y, t.w, t.h, mx, my)) {
                        dragging = t.id;
                        grabDX = mx - t.x;
                        grabDY = my - t.y;
                        Hud.freezeLayout(tiles, sw, sh);
                        break;
                    }
                }
            }
        }
        if (down && dragging >= 0) {
            for (Hud.Tile t : tiles) {
                if (t.id != dragging) continue;
                t.x = Math.max(0, Math.min(Math.max(0, sw - t.w), mx - grabDX));
                t.y = Math.max(0, Math.min(Math.max(0, sh - t.h), my - grabDY));
                Settings.POS_X[t.id] = t.x / (float) sw;
                Settings.POS_Y[t.id] = t.y / (float) sh;
            }
        }
        if (!down && wasDown && dragging >= 0) {
            dragging = -1;
            Settings.save();
        }
        wasDown = down;

        for (Hud.Tile t : tiles) {
            Hud.drawTile(g, mc, t);
            boolean hot = t.id == dragging || (dragging < 0 && inside(t.x, t.y, t.w, t.h, mx, my));
            int c = hot ? 0xFFFFF2A8 : 0xAAFFD84A;
            g.fill(t.x - 1, t.y - 1, t.x + t.w + 1, t.y, c);
            g.fill(t.x - 1, t.y + t.h, t.x + t.w + 1, t.y + t.h + 1, c);
            g.fill(t.x - 1, t.y, t.x, t.y + t.h, c);
            g.fill(t.x + t.w, t.y, t.x + t.w + 1, t.y + t.h, c);
        }

        String title = "Edit HUD";
        String hint = "Drag the boxes anywhere you like. Turn a module off in the menu to hide it.";
        g.text(mc.font, title, (sw - mc.font.width(title)) / 2, 14, accent, true);
        g.text(mc.font, hint, (sw - mc.font.width(hint)) / 2, 28, 0xFFCCCCCC, true);

        UiStyle.paintRect(g, mc, resetX, btnY, BW, BH, "Reset Layout",
                inside(resetX, btnY, BW, BH, mx, my), false, true);
        UiStyle.paintRect(g, mc, doneX, btnY, BW, BH, "Done",
                inside(doneX, btnY, BW, BH, mx, my), true, true);
    }

    @Override
    public void onClose() {
        Settings.save();
        super.onClose();
    }
}
