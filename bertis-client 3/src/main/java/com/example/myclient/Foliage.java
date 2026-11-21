package com.example.myclient;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Foliage {
    private static final Set<String> PLANTS = Set.of(
            "short_grass", "tall_grass", "fern", "large_fern", "vine", "dead_bush",
            "seagrass", "tall_seagrass", "kelp", "kelp_plant", "glow_lichen", "sugar_cane",
            "sweet_berry_bush", "bush", "firefly_bush", "leaf_litter", "short_dry_grass",
            "tall_dry_grass", "pink_petals", "wildflowers", "hanging_roots", "spore_blossom",
            "cave_vines", "cave_vines_plant", "weeping_vines", "weeping_vines_plant",
            "twisting_vines", "twisting_vines_plant"
    );

    private static final Map<Block, Boolean> CACHE = new ConcurrentHashMap<>();

    private Foliage() {}

    public static boolean isFoliage(Block block) {
        return CACHE.computeIfAbsent(block, Foliage::compute);
    }

    private static boolean compute(Block block) {
        BlockState state = block.defaultBlockState();
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)) return true;
        return PLANTS.contains(BuiltInRegistries.BLOCK.getKey(block).getPath());
    }
}
