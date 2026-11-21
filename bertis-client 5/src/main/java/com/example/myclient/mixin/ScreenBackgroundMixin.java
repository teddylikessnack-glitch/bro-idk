package com.example.myclient.mixin;

import com.example.myclient.TitleOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Replaces the cave panorama behind every screen that is shown outside a world
// (title, singleplayer, multiplayer, options, Mod Menu, connecting...) with the duck scene.
// require = 0 -> if this doesn't match the game version, it is skipped instead of crashing.
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void bertis$scene(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                              CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) return; // in a world: keep the normal blurred background
        TitleOverlay.drawScene(mc, graphics);
        TitleOverlay.sceneOwner = this;
        ci.cancel();
    }
}
