package dev.bearclient;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Custom title screen: a pixel-art bear in a moonlit pine forest.
 * Buttons are drawn by hand (no vanilla widgets) so the scene needs no background override.
 */
public class BearTitleScreen extends Screen {
    // 18 columns wide. B=fur, E=inner ear, T=muzzle/belly, K=eyes/nose.
    private static final String[] BEAR = {
            "..BBB........BBB..",
            ".BBEBB......BBEBB.",
            ".BBBBBBBBBBBBBBBB.",
            "BBBBBBBBBBBBBBBBBB",
            "BBBBKKBBBBBBKKBBBB",
            "BBBBBBBBBBBBBBBBBB",
            "BBBBBBTTTTTTBBBBBB",
            "BBBBBTTTKKTTTBBBBB",
            "BBBBBTTTTTTTTBBBBB",
            ".BBBBBTTKKTTBBBBB.",
            "..BBBBBBBBBBBBBB..",
            "..BBBBBBBBBBBBBB..",
            ".BBBBBBBBBBBBBBBB.",
            "BBBBBTTTTTTTTBBBBB",
            "BBBBBTTTTTTTTBBBBB",
            "BBBBBTTTTTTTTBBBBB",
            ".BBBBBTTTTTTBBBBB.",
            ".BBBBBBBBBBBBBBBB.",
            "..BBBBB....BBBBB..",
            ".BBBBBB....BBBBBB."
    };

    private record Btn(String label, int x, int y, int w, Runnable action) {}

    private final List<Btn> buttons = new ArrayList<>();
    private final float[][] stars = new float[80][3];
    private final float[][] flies = new float[16][4];

    // Static layers are computed once per resize in init(), not every frame.
    private int horizon;
    private int[] farHill, midHill;
    private int[][] farTrees, midTrees, fgTrees;

    public BearTitleScreen() {
        super(Component.literal(BearClient.NAME));
        Random r = new Random(42);
        for (float[] s : stars) { s[0] = r.nextFloat(); s[1] = r.nextFloat() * 0.55f; s[2] = r.nextFloat() * 6.28f; }
        for (float[] f : flies) { f[0] = r.nextFloat(); f[1] = 0.55f + r.nextFloat() * 0.4f; f[2] = r.nextFloat() * 6.28f; f[3] = 0.5f + r.nextFloat(); }
    }

    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override
    protected void init() {
        horizon = (int) (height * 0.72f);
        int cols = width / 2 + 2;
        farHill = new int[cols];
        midHill = new int[cols];
        for (int i = 0; i < cols; i++) {
            double x = i * 2.0 / Math.max(1, width);
            farHill[i] = (int) (horizon - height * (0.10 + 0.05 * Math.sin(x * 5.0 + 1.0) + 0.03 * Math.sin(x * 11.0)));
            midHill[i] = (int) (horizon - height * (0.025 + 0.03 * Math.sin(x * 4.0 + 3.0) + 0.015 * Math.sin(x * 9.0 + 1.0)));
        }
        Random r = new Random(11);
        farTrees = scatter(r, 22, farHill, 0.10f, 0.14f);
        midTrees = scatter(r, 14, midHill, 0.19f, 0.27f);
        float[] fx = {0.02f, 0.10f, 0.92f, 0.99f};
        fgTrees = new int[fx.length][3];
        for (int i = 0; i < fx.length; i++) {
            fgTrees[i][0] = (int) (width * fx[i]);
            fgTrees[i][1] = height;
            fgTrees[i][2] = (int) (height * (0.6f + r.nextFloat() * 0.15f));
        }

        buttons.clear();
        int bx = Math.max(12, width / 4 - 100), y0 = (int) (height * 0.42f);
        buttons.add(new Btn("Singleplayer", bx, y0, 200, () -> minecraft.gui.setScreen(new SelectWorldScreen(this))));
        buttons.add(new Btn("Multiplayer", bx, y0 + 24, 200, () -> minecraft.gui.setScreen(new JoinMultiplayerScreen(this))));
        buttons.add(new Btn("Bear Modules", bx, y0 + 48, 200, () -> minecraft.gui.setScreen(new ModuleScreen(this))));
        buttons.add(new Btn("Options", bx, y0 + 72, 98, () -> minecraft.gui.setScreen(new OptionsScreen(this, minecraft.options))));
        buttons.add(new Btn("Quit", bx + 102, y0 + 72, 98, () -> minecraft.stop()));
    }

    private int[][] scatter(Random r, int n, int[] hill, float minH, float maxH) {
        int[][] t = new int[n][3];
        for (int i = 0; i < n; i++) {
            int x = (int) (r.nextFloat() * width);
            t[i][0] = x;
            t[i][1] = hill[Math.min(hill.length - 1, x / 2)] + 4;
            t[i][2] = (int) (height * (minH + r.nextFloat() * (maxH - minH)));
        }
        return t;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        long now = System.currentTimeMillis();
        float time = now / 1000f;
        drawScene(g, now, time);

        G.pushScaled(g, width / 2f, height * 0.10f, 4f);
        G.centered(g, font, Component.literal("BEAR CLIENT").withStyle(ChatFormatting.BOLD), 0, 0, 0xFFE8A04A);
        G.pop(g);

        for (Btn b : buttons) {
            boolean hov = mx >= b.x && mx < b.x + b.w && my >= b.y && my < b.y + 20;
            G.fill(g, b.x - 1, b.y - 1, b.x + b.w + 1, b.y + 21, hov ? 0xFFE8A04A : 0xFF3A2A1A);
            G.fill(g, b.x, b.y, b.x + b.w, b.y + 20, hov ? 0xF0704A22 : 0xE0161018);
            G.centered(g, font, b.label, b.x + b.w / 2, b.y + 6, hov ? 0xFFFFFFFF : 0xFFE0D2C0);
        }

        G.text(g, font, BearClient.NAME + " " + BearClient.VERSION + "  |  Minecraft " + BearClient.MC_VERSION,
                6, height - 12, 0xFF9AA0B5);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (Btn b : buttons) {
                if (event.x() >= b.x && event.x() < b.x + b.w && event.y() >= b.y && event.y() < b.y + 20) {
                    b.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void drawScene(GuiGraphicsExtractor g, long now, float time) {
        // Sky + dusk glow
        G.gradient(g, 0, 0, width, horizon, 0xFF080D26, 0xFF2E2557);
        G.gradient(g, 0, (int) (horizon * 0.55f), width, horizon, 0x00E8884A, 0x66E8884A);

        // Twinkling stars
        for (float[] s : stars) {
            int a = (int) (130 + 110 * Math.sin(time * 1.8f + s[2]));
            int sx = (int) (s[0] * width), sy = (int) (s[1] * horizon);
            G.fill(g, sx, sy, sx + 1, sy + 1, (a << 24) | 0xFFFFFF);
        }

        // Moon
        int mr = Math.max(6, height / 14), mxp = (int) (width * 0.82f), myp = (int) (height * 0.17f);
        circle(g, mxp, myp, mr * 2, 0x14F4EFD0);
        circle(g, mxp, myp, mr * 3 / 2, 0x18F4EFD0);
        circle(g, mxp, myp, mr, 0xFFF4EFD0);

        // Far hills + trees
        for (int i = 0; i < farHill.length; i++) G.fill(g, i * 2, farHill[i], i * 2 + 2, horizon, 0xFF16213D);
        for (int[] t : farTrees) pine(g, t[0], t[1], t[2], 0xFF101A33);

        // Mid hills + trees
        for (int i = 0; i < midHill.length; i++) G.fill(g, i * 2, midHill[i], i * 2 + 2, horizon, 0xFF0F2E26);
        for (int[] t : midTrees) pine(g, t[0], t[1], t[2], 0xFF0B2A1E);

        // Mist + ground
        G.gradient(g, 0, horizon - (int) (height * 0.08f), width, horizon, 0x00B8C8E0, 0x2EB8C8E0);
        G.gradient(g, 0, horizon, width, height, 0xFF0F3322, 0xFF05140C);

        drawBear(g, now, time);

        // Fireflies
        for (float[] f : flies) {
            int x = (int) (f[0] * width + Math.sin(time * 0.6f * f[3] + f[2]) * 18);
            int y = (int) (f[1] * height + Math.sin(time * 0.9f + f[2]) * 10);
            int a = (int) (120 + 100 * Math.sin(time * 2.5f * f[3] + f[2]));
            G.fill(g, x - 2, y - 2, x + 3, y + 3, ((a / 4) << 24) | 0xD8FF6A);
            G.fill(g, x, y, x + 1, y + 1, (a << 24) | 0xF4FFB0);
        }

        // Foreground framing trees
        for (int[] t : fgTrees) pine(g, t[0], t[1], t[2], 0xFF06150E);
    }

    private void drawBear(GuiGraphicsExtractor g, long now, float time) {
        int px = Math.max(2, height / 60);
        int bw = BEAR[0].length() * px, bh = BEAR.length * px;
        int bx = (int) (width * 0.74f) - bw / 2;
        int baseY = (int) (height * 0.9f);
        int by = baseY - bh + (int) Math.round(Math.sin(time * 1.6) * px * 0.35);
        boolean blink = now % 4200 < 140;

        G.fill(g, bx - px, baseY - px / 2, bx + bw + px, baseY + px / 2, 0x55000000); // ground shadow

        for (int r = 0; r < BEAR.length; r++) {
            String row = BEAR[r];
            int c = 0;
            while (c < row.length()) {
                char ch = row.charAt(c);
                int end = c;
                while (end < row.length() && row.charAt(end) == ch) end++; // merge runs into one fill
                if (ch != '.') {
                    int color = switch (ch) {
                        case 'B' -> 0xFF6B4423;
                        case 'E' -> 0xFFA06A4A;
                        case 'T' -> 0xFFC9A074;
                        case 'K' -> (blink && r == 4) ? 0xFF6B4423 : 0xFF140C08;
                        default -> 0xFFFF00FF;
                    };
                    G.fill(g, bx + c * px, by + r * px, bx + end * px, by + (r + 1) * px, color);
                }
                c = end;
            }
        }
    }

    private void pine(GuiGraphicsExtractor g, int cx, int baseY, int ht, int color) {
        int trunkW = Math.max(2, ht / 16), trunkH = ht / 7;
        G.fill(g, cx - trunkW / 2, baseY - trunkH, cx - trunkW / 2 + trunkW, baseY, 0xFF140E09);
        int foliage = ht - trunkH / 2, top = baseY - ht, layers = 4;
        float maxHalf = ht * 0.27f;
        for (int r = 0; r < foliage; r += 2) {
            float f = r / (float) foliage;
            float lf = (f * layers) % 1f;
            int half = (int) (maxHalf * (0.15f + 0.85f * f) * (0.6f + 0.4f * lf));
            G.fill(g, cx - half, top + r, cx + half + 1, top + r + 2, color);
        }
    }

    private void circle(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt((double) r * r - (double) dy * dy);
            G.fill(g, cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }
}
