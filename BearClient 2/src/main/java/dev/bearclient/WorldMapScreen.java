package dev.bearclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Full-screen world map: drag to pan, scroll to zoom, C to recenter, M / Esc to close. */
public class WorldMapScreen extends Screen {
    private static final int[] ZOOM = {1, 2, 4, 8, 16, 32};
    private static final int CELL = 3; // GUI pixels per map cell

    private static int zoomIdx = 1;
    private final Screen parent;
    private double cx, cz;

    public WorldMapScreen(Screen parent) {
        super(Component.literal("World Map"));
        this.parent = parent;
        recenter();
    }

    private void recenter() {
        var p = net.minecraft.client.Minecraft.getInstance().player;
        if (p != null) { cx = p.getX(); cz = p.getZ(); }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        G.fill(g, 0, 0, width, height, 0xFF0B0D14);
        int accent = BearClient.accent();

        if (ModuleManager.safe()) {
            G.centered(g, font, "World Map is hidden by Server-Safe Mode", width / 2, height / 2, 0xFFFFFFFF);
            G.centered(g, font, "Turn it off in the Bear menu (Right Shift)", width / 2, height / 2 + 12, 0xFF9AA0B5);
            return;
        }

        int bpc = ZOOM[zoomIdx];
        WorldMapData.draw(g, 0, 0, width / CELL + 1, height / CELL + 1, CELL, cx, cz, bpc);
        // draw() centers on the middle cell, so shift marker math to match
        int colsHalf = (width / CELL + 1) / 2, rowsHalf = (height / CELL + 1) / 2;
        double originX = Math.floor(cx) - colsHalf * bpc, originZ = Math.floor(cz) - rowsHalf * bpc;

        var p = minecraft.player;
        if (p != null) {
            int px = (int) ((p.getX() - originX) / bpc * CELL), py = (int) ((p.getZ() - originZ) / bpc * CELL);
            G.fill(g, px - 3, py - 3, px + 4, py + 4, 0xFF000000);
            G.fill(g, px - 2, py - 2, px + 3, py + 3, accent);
            double yaw = Math.toRadians(p.getYRot());
            int tx = px + (int) (-Math.sin(yaw) * 9), ty = py + (int) (Math.cos(yaw) * 9);
            G.fill(g, tx - 1, ty - 1, tx + 2, ty + 2, 0xFFFFFFFF);
        }

        G.fill(g, 0, 0, width, 14, 0xC0000000);
        G.text(g, font, "World Map  |  Drag: pan   Scroll: zoom   C: center   M / Esc: close", 6, 3, 0xFFFFFFFF);

        double wx = originX + mx / (double) CELL * bpc, wz = originZ + my / (double) CELL * bpc;
        G.fill(g, 0, height - 14, width, height, 0xC0000000);
        G.text(g, font, String.format("X: %d  Z: %d     1 px = %d block(s)     %d chunks known",
                (int) Math.floor(wx), (int) Math.floor(wz), bpc, WorldMapData.knownChunks()), 6, height - 11, accent);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        int bpc = ZOOM[zoomIdx];
        cx -= dx / CELL * bpc;
        cz -= dy / CELL * bpc;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (scrollY > 0 && zoomIdx > 0) zoomIdx--;
        else if (scrollY < 0 && zoomIdx < ZOOM.length - 1) zoomIdx++;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_M) { onClose(); return true; }
        if (key == InputConstants.KEY_C) { recenter(); return true; }
        return super.keyPressed(event);
    }
}
