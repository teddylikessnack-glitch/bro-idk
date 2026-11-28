package com.example.myclient;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Decorative plants only (never leaves). Each category can be switched on/off in the menu.
public final class Foliage {
    // category order must match Settings.PERF_OPT_NAMES[FOLIAGE]
    private static final String[][] CATS = {
            {"short_grass", "tall_grass", "fern", "large_fern", "dead_bush", "short_dry_grass",
                    "tall_dry_grass", "leaf_litter", "bush"},
            {"pink_petals", "wildflowers", "firefly_bush"},
            {},
            {"brown_mushroom", "red_mushroom", "crimson_fungus", "warped_fungus", "crimson_roots",
                    "warped_roots", "nether_sprouts"},
            {"vine", "cave_vines", "cave_vines_plant", "weeping_vines", "weeping_vines_plant",
                    "twisting_vines", "twisting_vines_plant", "hanging_roots", "glow_lichen",
                    "spore_blossom", "pale_hanging_moss", "moss_carpet", "pale_moss_carpet"},
            {"seagrass", "tall_seagrass", "kelp", "kelp_plant", "lily_pad"},
            {"sugar_cane", "sweet_berry_bush"}
    };

    private static final Map<Block, Integer> CACHE = new ConcurrentHashMap<>();

    private Foliage() {}

    public static boolean isFoliage(Block block) {
        int cat = CACHE.computeIfAbsent(block, Foliage::category);
        return cat >= 0 && Settings.PERF_OPT[Settings.FOLIAGE][cat] == 0;
    }

    private static int category(Block block) {
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        if (path.endsWith("_sapling") || path.equals("mangrove_propagule")) return 2;
        for (int c = 0; c < CATS.length; c++) {
            for (String s : CATS[c]) if (s.equals(path)) return c;
        }
        BlockState state = block.defaultBlockState();
        if (state.is(BlockTags.FLOWERS)) return 1;
        return -1;
    }
}
