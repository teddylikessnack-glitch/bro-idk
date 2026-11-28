package dev.bearclient;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class Modules {
    private Modules() {}

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    private static void add(Module m) { ModuleManager.ALL.add(m); }

    // ---------- helpers for option-based performance modules ----------

    static final class Tweak<T> {
        private final Function<Options, OptionInstance<T>> getter;
        private final T value;
        private T saved;

        Tweak(Function<Options, OptionInstance<T>> getter, T value) {
            this.getter = getter;
            this.value = value;
        }

        void apply(Options o) {
            OptionInstance<T> opt = getter.apply(o);
            saved = opt.get();
            opt.set(value);
        }

        void restore(Options o) {
            if (saved != null) {
                getter.apply(o).set(saved);
                saved = null;
            }
        }
    }

    static final class TweakModule extends Module {
        private final Tweak<?>[] tweaks;

        TweakModule(String name, String desc, Tweak<?>... tweaks) {
            super(name, Category.PERFORMANCE, desc);
            this.tweaks = tweaks;
        }

        @Override public void onEnable(Minecraft mc) { for (Tweak<?> t : tweaks) t.apply(mc.options); }
        @Override public void onDisable(Minecraft mc) { for (Tweak<?> t : tweaks) t.restore(mc.options); }
    }

    private static void key(GuiGraphicsExtractor g, Font f, String label, KeyMapping kb, int x, int y, int w) {
        boolean on = kb.isDown();
        G.fill(g, x, y, x + w, y + 20, on ? 0xC0FFFFFF : 0x90000000);
        G.centered(g, f, label, x + w / 2, y + 6, on ? 0xFF000000 : 0xFFFFFFFF);
    }

    static void registerAll() {
        // Registered first so the watermark sits at the top of the top-left stack.
        add(new Module("Watermark", Category.CUSTOMIZATION, "Shows the client name and version.") {
            @Override public List<String> buildLines(Minecraft mc) {
                return List.of(BearClient.NAME + " v" + BearClient.VERSION);
            }
        }.at(Corner.TOP_LEFT));

        // ===================== PERFORMANCE =====================
        add(new TweakModule("Fast Graphics", "Fast graphics, no clouds, simple lighting. Restored when disabled.",
                new Tweak<GraphicsStatus>(Options::graphicsMode, GraphicsStatus.FAST),
                new Tweak<CloudStatus>(Options::cloudStatus, CloudStatus.OFF),
                new Tweak<Boolean>(Options::ambientOcclusion, false)));

        add(new TweakModule("Low Particles", "Minimal particles to cut overdraw in fights and explosions.",
                new Tweak<ParticleStatus>(Options::particles, ParticleStatus.MINIMAL)));

        add(new TweakModule("Entity Distance", "Renders entities at half distance.",
                new Tweak<Double>(Options::entityDistanceScaling, 0.5)));

        add(new Module("Dynamic Distance", Category.PERFORMANCE,
                "Lowers render distance when FPS drops, raises it again when FPS recovers.") {
            private int base;

            @Override public void onEnable(Minecraft mc) { base = mc.options.renderDistance().get(); }

            @Override public void onDisable(Minecraft mc) {
                if (base > 0) mc.options.renderDistance().set(base);
            }

            @Override public void onTick(Minecraft mc) {
                if (mc.player == null || mc.player.tickCount % 100 != 0) return;
                OptionInstance<Integer> opt = mc.options.renderDistance();
                int fps = mc.getFps(), cur = opt.get();
                if (fps < 40 && cur > 6) opt.set(cur - 2);
                else if (fps > 100 && cur < base) opt.set(cur + 1);
            }
        });

        add(new Module("Unfocused Limit", Category.PERFORMANCE,
                "Caps FPS to 15 while the game window is in the background.") {
            private Integer saved;

            @Override public void onTick(Minecraft mc) {
                boolean active = mc.isWindowActive();
                OptionInstance<Integer> fps = mc.options.framerateLimit();
                if (!active && saved == null) { saved = fps.get(); fps.set(15); }
                else if (active && saved != null) { fps.set(saved); saved = null; }
            }

            @Override public void onDisable(Minecraft mc) {
                if (saved != null) { mc.options.framerateLimit().set(saved); saved = null; }
            }
        });

        // ===================== DEBUG =====================
        add(new Module("FPS Counter", Category.DEBUG, "FPS and average frame time.") {
            @Override public List<String> buildLines(Minecraft mc) {
                return List.of("FPS: " + mc.getFps(), String.format("Frame: %.1f ms", BearClient.avgFrameMs()));
            }
        }.at(Corner.TOP_LEFT));

        add(new Module("Coordinates", Category.DEBUG, "Position, facing direction and biome.") {
            @Override public List<String> buildLines(Minecraft mc) {
                BlockPos p = mc.player.blockPosition();
                String biome = mc.level.getBiome(p).unwrapKey().map(G::shortKey).orElse("?");
                return List.of(
                        String.format("XYZ: %.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()),
                        "Facing: " + mc.player.getDirection().getSerializedName(),
                        "Biome: " + biome);
            }
        }.at(Corner.TOP_LEFT));

        add(new Module("Memory Usage", Category.DEBUG, "Used / max Java heap memory.") {
            @Override public List<String> buildLines(Minecraft mc) {
                Runtime rt = Runtime.getRuntime();
                long used = (rt.totalMemory() - rt.freeMemory()) >> 20, max = rt.maxMemory() >> 20;
                return List.of("Mem: " + used + " / " + max + " MB");
            }
        }.at(Corner.TOP_LEFT));

        add(new Module("Ping", Category.DEBUG, "Latency to the server.") {
            @Override public List<String> buildLines(Minecraft mc) {
                var conn = mc.getConnection();
                if (conn == null) return List.of("Ping: -");
                PlayerInfo info = conn.getPlayerInfo(mc.player.getUUID());
                return List.of(info == null ? "Ping: -" : "Ping: " + info.getLatency() + " ms");
            }
        }.at(Corner.TOP_LEFT));

        add(new Module("World Info", Category.DEBUG, "Entities, light level and dimension.") {
            @Override public List<String> buildLines(Minecraft mc) {
                int n = 0;
                for (Entity ignored : mc.level.entitiesForRendering()) n++;
                BlockPos p = mc.player.blockPosition();
                return List.of(
                        "Entities: " + n,
                        "Light: " + mc.level.getMaxLocalRawBrightness(p),
                        "Dim: " + G.shortKey(mc.level.dimension()));
            }
        }.at(Corner.TOP_LEFT));

        add(new Module("Speed", Category.DEBUG, "Horizontal speed in blocks per second.") {
            private Vec3 last;
            private double speed;

            @Override public void onTick(Minecraft mc) {
                if (mc.player == null) { last = null; return; }
                Vec3 p = mc.player.position();
                if (last != null) speed = speed * 0.8 + Math.hypot(p.x - last.x, p.z - last.z) * 20 * 0.2;
                last = p;
            }

            @Override public List<String> buildLines(Minecraft mc) {
                return List.of(String.format("Speed: %.2f b/s", speed));
            }
        }.at(Corner.TOP_LEFT));

        add(new Module("Frametime Graph", Category.DEBUG, "Bar graph of the last 120 frames (green = 60+ FPS).") {
            @Override public void renderOverlay(GuiGraphicsExtractor g, Minecraft mc, int w, int h) {
                int gw = BearClient.frameCount(), gh = 32;
                int x = corner.x(w, gw, 4), y = corner.y(h, gh, 4);
                G.fill(g, x - 1, y - 1, x + gw + 1, y + gh + 1, 0x90000000);
                for (int i = 0; i < gw; i++) {
                    float ms = BearClient.frameMs(i);
                    int bar = Math.min(gh, Math.max(1, (int) (ms * 1.5f)));
                    int color = ms < 16.7f ? 0xFF55FF55 : ms < 33.4f ? 0xFFFFD34D : 0xFFFF5555;
                    G.fill(g, x + i, y + gh - bar, x + i + 1, y + gh, color);
                }
            }
        }.at(Corner.BOTTOM_LEFT));

        // ===================== CUSTOMIZATION =====================
        add(new Module("Clock", Category.CUSTOMIZATION, "Real-world time.") {
            @Override public List<String> buildLines(Minecraft mc) {
                return List.of(LocalTime.now().format(CLOCK));
            }
        }.at(Corner.TOP_LEFT));

        add(new Module("Keystrokes", Category.CUSTOMIZATION, "WASD and space display.") {
            @Override public void renderOverlay(GuiGraphicsExtractor g, Minecraft mc, int w, int h) {
                Font f = mc.font;
                int x = 6, y = h / 2 - 30;
                key(g, f, "W", mc.options.keyUp, x + 22, y, 20);
                key(g, f, "A", mc.options.keyLeft, x, y + 22, 20);
                key(g, f, "S", mc.options.keyDown, x + 22, y + 22, 20);
                key(g, f, "D", mc.options.keyRight, x + 44, y + 22, 20);
                key(g, f, "SPACE", mc.options.keyJump, x, y + 44, 64);
            }
        });

        add(new Module("Armor HUD", Category.CUSTOMIZATION, "Armor pieces with durability next to the hotbar.") {
            private final EquipmentSlot[] slots = {
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

            @Override public void renderOverlay(GuiGraphicsExtractor g, Minecraft mc, int w, int h) {
                int x = w / 2 - 91 - 24, y = h - 96;
                for (EquipmentSlot slot : slots) {
                    ItemStack s = mc.player.getItemBySlot(slot);
                    if (!s.isEmpty()) {
                        G.item(g, s, x, y);
                        G.itemDecorations(g, mc.font, s, x, y);
                    }
                    y += 18;
                }
            }
        });

        ModuleManager.rainbow = new Module("Rainbow Accent", Category.CUSTOMIZATION,
                "Animates the HUD accent color through the rainbow.") {};
        add(ModuleManager.rainbow);

        ModuleManager.safeMode = new Module("Server-Safe Mode", Category.CUSTOMIZATION,
                "Hides Radar, Minimap and World Map. Turn on for servers that ban map or radar mods.") {};
        add(ModuleManager.safeMode);

        // ===================== OVERLAY =====================
        add(new Module("Minimap", Category.OVERLAY, "Top-down map of explored terrain. Press M for the full world map.") {
            @Override public void renderOverlay(GuiGraphicsExtractor g, Minecraft mc, int w, int h) {
                if (ModuleManager.safe()) return;
                int cols = 48, rows = 48, cell = 2, bpc = 2;
                int size = cols * cell;
                int x = corner.x(w, size, 4), y = corner.y(h, size, 4);
                int accent = BearClient.accent();

                G.fill(g, x - 2, y - 2, x + size + 2, y + size + 2, accent);
                G.fill(g, x, y, x + size, y + size, 0xFF0B0D14);
                WorldMapData.draw(g, x, y, cols, rows, cell, mc.player.getX(), mc.player.getZ(), bpc);

                // player marker (north is up): center dot + a short pointer in the facing direction
                int px = x + size / 2, py = y + size / 2;
                double yaw = Math.toRadians(mc.player.getYRot());
                int tx = px + (int) (-Math.sin(yaw) * 7), ty = py + (int) (Math.cos(yaw) * 7);
                G.fill(g, px - 2, py - 2, px + 3, py + 3, 0xFF000000);
                G.fill(g, px - 1, py - 1, px + 2, py + 2, 0xFFFFFFFF);
                G.fill(g, tx - 1, ty - 1, tx + 2, ty + 2, accent);
                G.text(g, mc.font, "N", x + size / 2 - 2, y + 2, 0xFFFFFFFF);
            }
        }.at(Corner.TOP_RIGHT));

        add(new Module("Radar", Category.OVERLAY, "Nearby entities (red hostile, green passive, white players).") {
            private final List<LivingEntity> near = new ArrayList<>();
            private final double range = 32;

            @Override public void onTick(Minecraft mc) {
                if (mc.player == null || mc.level == null || mc.player.tickCount % 5 != 0) return;
                near.clear();
                double r2 = range * range;
                for (Entity e : mc.level.entitiesForRendering()) {
                    if (e instanceof LivingEntity le && e != mc.player && !le.isInvisible()
                            && le.distanceToSqr(mc.player) <= r2) {
                        near.add(le);
                        if (near.size() >= 80) break;
                    }
                }
            }

            @Override public void onDisable(Minecraft mc) { near.clear(); }

            @Override public void renderOverlay(GuiGraphicsExtractor g, Minecraft mc, int w, int h) {
                if (ModuleManager.safe()) return;
                int size = 90, accent = BearClient.accent();
                int x = corner.x(w, size, 4), y = corner.y(h, size, 4);
                G.fill(g, x, y, x + size, y + size, 0x90000000);
                G.fill(g, x, y, x + size, y + 1, accent);
                G.fill(g, x, y + size - 1, x + size, y + size, accent);
                G.fill(g, x, y, x + 1, y + size, accent);
                G.fill(g, x + size - 1, y, x + size, y + size, accent);

                double cx = x + size / 2.0, cy = y + size / 2.0;
                double yaw = Math.toRadians(mc.player.getYRot());
                double cos = Math.cos(yaw), sin = Math.sin(yaw);
                double scale = (size / 2.0 - 3) / range;
                for (LivingEntity e : near) {
                    double dx = e.getX() - mc.player.getX(), dz = e.getZ() - mc.player.getZ();
                    double right = -dx * cos - dz * sin;
                    double fwd = -dx * sin + dz * cos;
                    int sx = (int) (cx + right * scale), sy = (int) (cy - fwd * scale);
                    if (sx < x + 2 || sx > x + size - 3 || sy < y + 2 || sy > y + size - 3) continue;
                    int color = e instanceof Player ? 0xFFFFFFFF
                            : e instanceof Monster ? 0xFFFF4444
                            : e instanceof AgeableMob ? 0xFF55FF55 : 0xFFAAAAAA;
                    G.fill(g, sx - 1, sy - 1, sx + 2, sy + 2, color);
                }
                G.fill(g, (int) cx - 1, (int) cy - 1, (int) cx + 2, (int) cy + 2, accent);
            }
        }.at(Corner.BOTTOM_RIGHT));

        add(new Module("Compass", Category.OVERLAY, "Heading and yaw at the top of the screen.") {
            @Override public void renderOverlay(GuiGraphicsExtractor g, Minecraft mc, int w, int h) {
                float yaw = Mth.wrapDegrees(mc.player.getYRot());
                float pos = (yaw % 360 + 360) % 360;
                int idx = Math.round(pos / 45f) % 8;
                G.centered(g, mc.font, DIRS[idx] + "  " + Math.round(pos) + "\u00B0", w / 2, 6, BearClient.accent());
            }
        });

        add(new Module("Low Health FX", Category.OVERLAY, "Pulsing red vignette when you are at 3 hearts or less.") {
            @Override public void renderOverlay(GuiGraphicsExtractor g, Minecraft mc, int w, int h) {
                float hp = mc.player.getHealth();
                if (hp > 6f || !mc.player.isAlive()) return;
                float danger = 1f - Math.max(hp, 0f) / 7f;
                float pulse = 0.65f + 0.35f * (float) Math.sin(System.currentTimeMillis() / 180.0);
                int steps = 14, step = Math.max(2, Math.min(w, h) / 70);
                for (int i = 0; i < steps; i++) {
                    int a = (int) (110 * danger * pulse * (1f - i / (float) steps));
                    if (a <= 0) continue;
                    int col = (a << 24) | 0xB00000, o = i * step;
                    G.fill(g, o, o, w - o, o + step, col);
                    G.fill(g, o, h - o - step, w - o, h - o, col);
                    G.fill(g, o, o + step, o + step, h - o - step, col);
                    G.fill(g, w - o - step, o + step, w - o, h - o - step, col);
                }
            }
        });

        add(new Module("Warm Tint", Category.OVERLAY, "Subtle warm color filter over the whole screen.") {
            @Override public void renderOverlay(GuiGraphicsExtractor g, Minecraft mc, int w, int h) {
                G.fill(g, 0, 0, w, h, 0x1AFFA23C);
            }
        });
    }
}
