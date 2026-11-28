package com.example.myclient.mixin;

import com.example.myclient.Settings;
import com.example.myclient.TitleOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 1) Swaps ONLY the cave panorama for the duck scene. The rest of the vanilla background code
//    still runs, so menus from other mods (Essential, Mod Menu...) are drawn on top of the scene.
// 2) Loading / connecting screens use a dirt background instead; those get the scene too.
// require = 0 -> if a target doesn't match this game version it is skipped instead of crashing.
@Mixin(value = Screen.class, priority = 100)
public abstract class ScreenBackgroundMixin {
    @Inject(method = "extractPanorama", at = @At("HEAD"), cancellable = true, require = 0)
    private void bertis$panorama(GuiGraphicsExtractor graphics, float partialTick, CallbackInfo ci) {
        if (!Settings.customMenus) return;
        TitleOverlay.drawScene(Minecraft.getInstance(), graphics);
        TitleOverlay.sceneOwner = this;
        ci.cancel();
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void bertis$loading(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                CallbackInfo ci) {
        if (!Settings.customMenus) return;
        if (TitleOverlay.isLoadingScreen(this.getClass().getSimpleName())) {
            TitleOverlay.drawScene(Minecraft.getInstance(), graphics);
            TitleOverlay.sceneOwner = this;
            ci.cancel();
        }
    }
}
