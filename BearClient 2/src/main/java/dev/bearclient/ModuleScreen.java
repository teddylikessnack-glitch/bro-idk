package dev.bearclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ModuleScreen extends Screen {
    private static final int COL_W = 124, ROW_H = 20, GAP = 8, TOP = 60;
    private static final Category[] CATS = Category.values();

    private final Screen parent;

    public ModuleScreen(Screen parent) {
        super(Component.literal("Bear Client Modules"));
        this.parent = parent;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }

    private int colX(int i) {
        int total = CATS.length * COL_W + (CATS.length - 1) * GAP;
        return (width - total) / 2 + i * (COL_W + GAP);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        G.gradient(g, 0, 0, width, height, 0xE0101018, 0xF0080810);
        int accent = BearClient.accent();

        G.pushScaled(g, width / 2f, 12f, 2f);
        G.centered(g, font, Component.literal("BEAR CLIENT").withStyle(ChatFormatting.BOLD), 0, 0, accent);
        G.pop(g);

        Module hovered = null;
        for (int ci = 0; ci < CATS.length; ci++) {
            int x = colX(ci);
            G.fill(g, x, TOP - 22, x + COL_W, TOP - 2, 0xE0000000);
            G.fill(g, x, TOP - 4, x + COL_W, TOP - 2, accent);
            G.centered(g, font, CATS[ci].label, x + COL_W / 2, TOP - 16, 0xFFFFFFFF);

            List<Module> list = ModuleManager.in(CATS[ci]);
            for (int j = 0; j < list.size(); j++) {
                Module m = list.get(j);
                int y = TOP + j * (ROW_H + 2);
                boolean hov = inside(mx, my, x, y, COL_W, ROW_H);
                int bg = m.isEnabled() ? (hov ? 0xF0B07A38 : 0xE08C5E2A) : (hov ? 0xE0303040 : 0xC0181820);
                G.fill(g, x, y, x + COL_W, y + ROW_H, bg);
                G.text(g, font, m.name, x + 6, y + 6, m.isEnabled() ? 0xFFFFFFFF : 0xFFB0B0C0);
                if (m.usesCorner) G.text(g, font, m.corner.tag, x + COL_W - 16, y + 6, 0xFF8A8FA8);
                if (hov) hovered = m;
            }
        }

        G.centered(g, font, hovered != null ? hovered.description : "Hover a module to see what it does",
                width / 2, height - 30, hovered != null ? 0xFFFFFFFF : 0xFF6F748A);
        G.centered(g, font, "Left click: toggle   Right click: move HUD corner   M: world map   Right Shift / Esc: close",
                width / 2, height - 16, 0xFF9AA0B5);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        for (int ci = 0; ci < CATS.length; ci++) {
            int x = colX(ci);
            List<Module> list = ModuleManager.in(CATS[ci]);
            for (int j = 0; j < list.size(); j++) {
                if (!inside(mx, my, x, TOP + j * (ROW_H + 2), COL_W, ROW_H)) continue;
                Module m = list.get(j);
                if (button == 0) m.toggle();
                else if (button == 1 && m.usesCorner) m.corner = m.corner.next();
                ModuleManager.save();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RSHIFT) { onClose(); return true; }
        return super.keyPressed(event);
    }
}
