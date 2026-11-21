package com.example.myclient.mixin;

import com.example.myclient.TitleOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Replaces the panorama behind the Singleplayer and Multiplayer screens with the duck scene.
// require = 0 -> if this doesn't match the game version, it is skipped instead of crashing.
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void bertis$scene(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                              CallbackInfo ci) {
        String n = this.getClass().getSimpleName();
        if (n.equals("SelectWorldScreen") || n.equals("JoinMultiplayerScreen")) {
            TitleOverlay.drawScene(Minecraft.getInstance(), graphics);
            ci.cancel();
        }
    }
}
