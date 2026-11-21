package com.example.myclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

// Paints a custom main menu OVER the vanilla title screen: a pixel-art scene of a duck
// walking to a lemonade stand, plus restyled buttons. The real vanilla buttons stay in place
// underneath, so Singleplayer / Multiplayer / Options / Quit all keep working.
public final class TitleOverlay {
    private static final long CYCLE = 16000;
    private static final long WALK = 9000;

    // Set by the background mixin: the screen whose background the scene was drawn for.
    public static volatile Object sceneOwner;

    private TitleOverlay() {}

    private static void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + h, c);
    }

    // rectangle in "pixel units" relative to an origin
    private static void u(GuiGraphicsExtractor g, int ox, int oy, int p, int ux, int uy, int uw, int uh, int c) {
        g.fill(ox + ux * p, oy + uy * p, ox + (ux + uw) * p, oy + (uy + uh) * p, c);
    }

    // The duck + lemonade stand scene, drawn full-screen.
    public static void drawScene(Minecraft mc, GuiGraphicsExtractor g) {
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        long now = System.currentTimeMillis();
        int p = Math.max(2, sh / 100);
        int groundY = (sh * 78 / 100) / p * p;

        // ---- sky, sun, clouds ----
        g.fillGradient(0, 0, sw, groundY, 0xFF4FA8FF, 0xFFBDE8FF);

        int sunX = sw * 82 / 100 / p * p;
        int sunY = sh * 10 / 100 / p * p;
        rect(g, sunX - p, sunY - p, 10 * p, 10 * p, 0xFFFFEC8C);
        rect(g, sunX, sunY, 8 * p, 8 * p, 0xFFFFD640);

        for (int i = 0; i < 3; i++) {
            double speed = 6 + 3 * i;
            int cx = (int) ((now / 1000.0 * speed + i * sw / 3.0) % (sw + 24 * p)) - 12 * p;
            int cy = (sh * (8 + 9 * i) / 100) / p * p;
            rect(g, cx, cy + p, 14 * p, 3 * p, 0xFFFFFFFF);
            rect(g, cx + 3 * p, cy, 8 * p, 2 * p, 0xFFFFFFFF);
        }

        // ---- hills + ground ----
        for (int i = 0; i * 12 * p < sw; i++) {
            int h = (int) ((5 + 3 * Math.sin(i * 0.8) + 2 * Math.cos(i * 1.7)) * p);
            rect(g, i * 12 * p, groundY - h, 12 * p, h, 0xFF78BE6E);
        }
        rect(g, 0, groundY, sw, 2 * p, 0xFF6EC850);
        rect(g, 0, groundY + 2 * p, sw, sh - groundY - 2 * p, 0xFF58AA46);
        int dirtY = groundY + (sh - groundY) * 45 / 100;
        rect(g, 0, dirtY, sw, sh - dirtY, 0xFF86603F);
        int[] flowerCols = {0xFFFF7EB6, 0xFFFFFFFF, 0xFFFF5555, 0xFFFFE066};
        for (int i = 0; i < 12; i++) {
            int fx = ((i * 53 + 17) % Math.max(1, sw / p)) * p;
            rect(g, fx, groundY - p, p, p, flowerCols[i % 4]);
            rect(g, fx, groundY, p, p, 0xFF3F8F34);
        }

        // ---- lemonade stand ----
        int sx = (sw * 68 / 100) / p * p;
        int wood = 0xFF8C5A32, plank = 0xFFB07440, darkWood = 0xFF824E28;
        int yel = 0xFFFFD640, white = 0xFFFFFFFF;
        u(g, sx, groundY, p, 1, -20, 2, 20, wood);          // posts
        u(g, sx, groundY, p, 25, -20, 2, 20, wood);
        u(g, sx, groundY, p, 3, -20, 22, 10, 0xFF966E46);    // back wall
        u(g, sx, groundY, p, 0, -10, 28, 10, plank);         // counter
        u(g, sx, groundY, p, 0, -11, 28, 2, darkWood);
        u(g, sx, groundY, p, 0, -6, 28, 1, 0xFF784C28);
        u(g, sx, groundY, p, 0, -3, 28, 1, 0xFF784C28);
        for (int i = 0; i < 14; i++) {                       // striped awning
            int c = i % 2 == 0 ? yel : white;
            u(g, sx, groundY, p, i * 2, -26, 2, 5, c);
            if (i % 2 == 0) u(g, sx, groundY, p, i * 2, -21, 2, 1, c);
        }
        u(g, sx, groundY, p, 0, -27, 28, 1, 0xFFE6AA1E);
        // sign
        u(g, sx, groundY, p, 5, -9, 18, 6, 0xFFF0D696);
        u(g, sx, groundY, p, 5, -9, 18, 1, darkWood);
        u(g, sx, groundY, p, 5, -4, 18, 1, darkWood);
        int signW = 18 * p - 4;
        String sign = "LEMONADE";
        if (mc.font.width(sign) > signW) sign = "LEMONS";
        if (mc.font.width(sign) > signW) sign = "LEMON";
        g.text(mc.font, sign, sx + 5 * p + (18 * p - mc.font.width(sign)) / 2,
                groundY - 9 * p + (6 * p - mc.font.lineHeight) / 2 + 1, 0xFF5A3A14, false);
        // pitcher, cups, lemons on the counter
        u(g, sx, groundY, p, 4, -17, 4, 6, 0xFFFFF596);
        u(g, sx, groundY, p, 8, -16, 1, 3, 0xFFFFF596);
        u(g, sx, groundY, p, 14, -14, 3, 3, white);
        u(g, sx, groundY, p, 14, -14, 3, 1, 0xFFFFE25A);
        u(g, sx, groundY, p, 19, -14, 3, 3, white);
        u(g, sx, groundY, p, 19, -14, 3, 1, 0xFFFFE25A);
        u(g, sx, groundY, p, 23, -13, 3, 2, 0xFFFFE628);
        u(g, sx, groundY, p, 25, -14, 1, 1, 0xFF46A032);

        // ---- the duck walks to the stand ----
        long t = now % CYCLE;
        int stopX = sx - 20 * p;
        int startX = -16 * p;
        int duckX;
        int frame = 0;
        if (t < WALK) {
            duckX = startX + (int) ((stopX - startX) * (t / (double) WALK));
            duckX = duckX / p * p;
            frame = (int) ((now / 220) % 2);
        } else {
            duckX = stopX;
        }
        int duckY = groundY - 15 * p;
        Branding.drawDuckFrame(g, duckX, duckY, p, frame);

        if (t > WALK + 500 && t < WALK + 4500) {
            String q = "Quack!";
            int bw = mc.font.width(q) + 8;
            int bh = mc.font.lineHeight + 6;
            int bx = duckX + 8 * p - bw / 2;
            int by = duckY - bh - 8;
            rect(g, bx, by, bw, bh, 0xFFFFFFFF);
            rect(g, bx + bw / 2 - 2, by + bh, 4, 4, 0xFFFFFFFF);
            g.text(mc.font, q, bx + 4, by + 3, 0xFF202020, false);
        }
    }

    public static void draw(Minecraft mc, Screen screen, GuiGraphicsExtractor g, int mx, int my) {
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int accent = Settings.accentColor();

        // Normal case: the mixin already drew the scene BEHIND the screen, so the vanilla logo,
        // Mod Menu, Essential etc. are all untouched. Fallback: cover the screen ourselves.
        boolean byMixin = sceneOwner == screen;
        if (!byMixin) drawScene(mc, g);

        List<AbstractWidget> styled = new ArrayList<>();
        List<AbstractWidget> others = new ArrayList<>();
        for (GuiEventListener l : screen.children()) {
            if (!(l instanceof AbstractWidget w)) continue;
            if (UiStyle.isMainMenuButton(UiStyle.key(w))) styled.add(w);
            else if (w.visible) others.add(w);
        }

        for (AbstractWidget w : styled) UiStyle.paint(g, mc, w, mx, my);

        if (byMixin) {
            // keep the Bertis title: a slim banner at the very top, above the vanilla logo
            drawTitleBlock(mc, g, 4, 2, false);
            return;
        }

        // ---- fallback only: our own title + redraw every other widget on top ----
        int topY = sh / 4;
        if (!styled.isEmpty()) {
            topY = Integer.MAX_VALUE;
            for (AbstractWidget w : styled) topY = Math.min(topY, w.getY());
        }
        int scale = sh >= 240 ? 3 : 2;
        int titleH = mc.font.lineHeight * scale;
        drawTitleBlock(mc, g, Math.max(8, topY - titleH - 24), scale, true);

        for (AbstractWidget w : others) w.extractRenderState(g, mx, my, 0.0f);
    }

    // "Bertis Client" title in a dark box with an accent line; optional "by CayL12" underneath.
    private static void drawTitleBlock(Minecraft mc, GuiGraphicsExtractor g, int ty, int scale, boolean byLine) {
        int sw = mc.getWindow().getGuiScaledWidth();
        int accent = Settings.accentColor();
        String name = "Bertis Client";
        int titleW = mc.font.width(name) * scale;
        int titleH = mc.font.lineHeight * scale;
        int tx = (sw - titleW) / 2;
        int extra = byLine ? 18 : 4;
        g.fill(tx - 10, ty - 3, tx + titleW + 10, ty + titleH + extra, 0x80000000);
        g.fill(tx - 10, ty - 3, tx + titleW + 10, ty - 1, accent);
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) tx, (float) ty);
        pose.scale((float) scale, (float) scale);
        g.text(mc.font, name, 0, 0, accent, true);
        pose.popMatrix();
        if (byLine) {
            String by = "by CayL12";
            g.text(mc.font, by, (sw - mc.font.width(by)) / 2, ty + titleH + 4, 0xFFCCCCCC, true);
        }
    }
}
