package dev.bearclient;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public final class HudRenderer {
    private static final int[] cursor = new int[Corner.ALL.length];

    private HudRenderer() {}

    /** Registered with Fabric's HudElementRegistry (runs only while the vanilla HUD is shown). */
    public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        BearClient.recordFrame();
        if (mc.player == null || mc.level == null) return;

        int w = g.guiWidth(), h = g.guiHeight();
        Font font = mc.font;
        int accent = BearClient.accent();

        // Pass 1: free-form overlays (tints, minimap, radar, keystrokes...)
        for (Module m : ModuleManager.ALL) if (m.isEnabled()) m.renderOverlay(g, mc, w, h);

        // Pass 2: stacked text panels per corner
        for (Corner c : Corner.ALL) cursor[c.ordinal()] = c.bottom ? h - 4 : 4;
        for (Module m : ModuleManager.ALL) {
            if (!m.isEnabled()) continue;
            List<String> lines = m.lines;
            if (lines.isEmpty()) continue;

            int maxW = 0;
            for (String s : lines) maxW = Math.max(maxW, font.width(s));
            int pw = maxW + 9, ph = lines.size() * 10 + 4;
            int i = m.corner.ordinal();
            int x = m.corner.x(w, pw, 4);
            int y = m.corner.bottom ? cursor[i] - ph : cursor[i];

            G.fill(g, x, y, x + pw, y + ph, 0x90000000);
            G.fill(g, x, y, x + 2, y + ph, accent);
            for (int n = 0; n < lines.size(); n++) {
                G.text(g, font, lines.get(n), x + 6, y + 3 + n * 10, 0xFFFFFFFF);
            }
            cursor[i] += m.corner.bottom ? -(ph + 2) : ph + 2;
        }
    }
}
