package dev.bearclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Top-down color map of every chunk the client has loaded (and remembers while you stay connected).
 * Stored as one int[256] per chunk; drawn with run-length merged fills, so no textures are needed.
 */
public final class WorldMapData {
    private static final int SCAN_RADIUS = 10;      // chunks around the player
    private static final int RESCAN_TICKS = 400;    // refresh a chunk every 20 s while near
    private static final int MAX_CHUNKS = 30_000;   // ~35 MB worst case

    private static final int[][] OFFSETS = buildOffsets();

    static final class Chunk {
        final int[] colors = new int[256]; // ARGB, 0 = unknown
        int lastScan = -1_000_000;
    }

    private static final Map<String, Map<Long, Chunk>> DIMENSIONS = new HashMap<>();
    private static Map<Long, Chunk> chunks;
    private static ClientLevel lastLevel;
    private static int ticks;

    private WorldMapData() {}

    private static int[][] buildOffsets() {
        List<int[]> list = new ArrayList<>();
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++)
            for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++)
                if (x * x + z * z <= SCAN_RADIUS * SCAN_RADIUS) list.add(new int[]{x, z});
        list.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
        return list.toArray(new int[0][]);
    }

    public static long key(int cx, int cz) { return ((long) cx << 32) | (cz & 0xFFFFFFFFL); }

    public static void clear() {
        DIMENSIONS.clear();
        chunks = null;
        lastLevel = null;
    }

    public static int knownChunks() { return chunks == null ? 0 : chunks.size(); }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        LocalPlayer p = mc.player;
        if (level == null || p == null) return;

        if (level != lastLevel) {
            lastLevel = level;
            chunks = DIMENSIONS.computeIfAbsent(G.shortKey(level.dimension()), k -> new HashMap<>());
        }
        ticks++;
        if ((ticks & 1) != 0) return;

        int pcx = p.getBlockX() >> 4, pcz = p.getBlockZ() >> 4;
        int budget = 2;
        for (int[] o : OFFSETS) {
            int cx = pcx + o[0], cz = pcz + o[1];
            long k = key(cx, cz);
            Chunk c = chunks.get(k);
            if (c != null && ticks - c.lastScan < RESCAN_TICKS) continue;
            LevelChunk lc = level.getChunkSource().getChunkNow(cx, cz);
            if (lc == null) continue;
            if (c == null) { c = new Chunk(); chunks.put(k, c); }
            scan(level, lc, c, p.getBlockY());
            c.lastScan = ticks;
            if (--budget == 0) break;
        }

        if (ticks % 400 == 0 && chunks.size() > MAX_CHUNKS) {
            chunks.keySet().removeIf(k -> {
                int cx = (int) (k >> 32), cz = (int) (long) k;
                return Math.abs(cx - pcx) > 48 || Math.abs(cz - pcz) > 48;
            });
        }
    }

    private static void scan(ClientLevel level, LevelChunk lc, Chunk out, int playerY) {
        int minY = level.getMinY();
        boolean ceiling = level.dimensionType().hasCeiling(); // Nether: slice at the player's height
        int baseX = lc.getPos().getMinBlockX(), baseZ = lc.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int[] heights = new int[256];

        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int wx = baseX + x, wz = baseZ + z;
                int y;
                if (ceiling) {
                    y = Math.min(level.getMaxY(), playerY + 1);
                    while (y > minY && lc.getBlockState(pos.set(wx, y, wz)).isAir()) y--;
                } else {
                    y = lc.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                }
                y = Math.max(minY, y);
                heights[(z << 4) | x] = y;

                BlockState state = lc.getBlockState(pos.set(wx, y, wz));
                int idx = (z << 4) | x;
                if (state.isAir()) { out.colors[idx] = 0; continue; }

                if (state.getFluidState().is(FluidTags.WATER)) {
                    int depth = 0;
                    while (depth < 14 && y - depth - 1 > minY
                            && lc.getBlockState(pos.set(wx, y - depth - 1, wz)).getFluidState().is(FluidTags.WATER)) depth++;
                    float f = Math.max(0.45f, 1f - depth * 0.045f);
                    out.colors[idx] = shade(MapColor.WATER.col, f);
                    continue;
                }

                int rgb = state.getMapColor(level, pos).col;
                if (rgb == 0) rgb = 0x202020;
                // simple relief: compare with the north-west neighbour, quantized so runs stay long
                int ref = (x > 0 && z > 0) ? heights[((z - 1) << 4) | (x - 1)] : y;
                float f = y > ref ? 1.14f : y < ref ? 0.86f : 1f;
                out.colors[idx] = shade(rgb, f);
            }
        }
    }

    private static int shade(int rgb, float f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * f));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * f));
        int b = Math.min(255, (int) ((rgb & 255) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * Draws the map. cols x rows cells, each cell = cellPx GUI pixels = bpc blocks, centered on (centerX, centerZ).
     * Horizontal runs of identical color are merged into a single fill.
     */
    public static void draw(GuiGraphicsExtractor g, int x, int y, int cols, int rows, int cellPx,
                            double centerX, double centerZ, int bpc) {
        if (chunks == null) return;
        int bx0 = (int) Math.floor(centerX) - (cols / 2) * bpc;
        int bz0 = (int) Math.floor(centerZ) - (rows / 2) * bpc;
        long lastKey = Long.MIN_VALUE;
        Chunk lastChunk = null;

        for (int r = 0; r < rows; r++) {
            int bz = bz0 + r * bpc;
            int runStart = 0, runColor = 0;
            for (int c = 0; c <= cols; c++) {
                int color = 0;
                if (c < cols) {
                    int bx = bx0 + c * bpc;
                    long k = key(bx >> 4, bz >> 4);
                    if (k != lastKey) { lastKey = k; lastChunk = chunks.get(k); }
                    if (lastChunk != null) color = lastChunk.colors[((bz & 15) << 4) | (bx & 15)];
                }
                if (color != runColor) {
                    if (runColor != 0) G.fill(g, x + runStart * cellPx, y + r * cellPx, x + c * cellPx, y + (r + 1) * cellPx, runColor);
                    runStart = c;
                    runColor = color;
                }
            }
        }
    }
}
