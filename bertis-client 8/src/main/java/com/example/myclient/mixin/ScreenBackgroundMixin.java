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

// Paints the duck scene over the cave panorama on every screen shown outside a world.
// The vanilla background code still runs (we do NOT cancel it), so other mods that hook into
// it, like Essential, keep working. Low priority number = we run first at the end of the method,
// so other mods' drawing lands on top of our scene.
// require = 0 -> if this doesn't match the game version, it is skipped instead of crashing.
@Mixin(value = Screen.class, priority = 100)
public abstract class ScreenBackgroundMixin {
    @Inject(method = "extractBackground", at = @At("TAIL"), require = 0)
    private void bertis$scene(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                              CallbackInfo ci) {
        if (!Settings.customMenus) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) return; // in a world: keep the normal blurred background
        TitleOverlay.drawScene(mc, graphics);
        TitleOverlay.sceneOwner = this;
    }
}
