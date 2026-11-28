package com.example.myclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.resources.Identifier;

// Same switches as F3+B / F3+G / F3+H, but with buttons.
public final class Debug {
    private Debug() {}

    private static boolean on(Identifier id) {
        return Minecraft.getInstance().debugEntries.isCurrentlyEnabled(id);
    }

    private static void toggle(Identifier id) {
        Minecraft.getInstance().debugEntries.toggleStatus(id);
    }

    public static boolean hitboxes() {
        return on(DebugScreenEntries.ENTITY_HITBOXES);
    }

    public static void toggleHitboxes() {
        toggle(DebugScreenEntries.ENTITY_HITBOXES);
    }

    public static boolean chunkBorders() {
        return on(DebugScreenEntries.CHUNK_BORDERS);
    }

    public static void toggleChunkBorders() {
        toggle(DebugScreenEntries.CHUNK_BORDERS);
    }

    public static boolean advancedTips() {
        return Minecraft.getInstance().options.advancedItemTooltips;
    }

    public static void toggleAdvancedTips() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.advancedItemTooltips = !mc.options.advancedItemTooltips;
        mc.options.save();
    }
}
