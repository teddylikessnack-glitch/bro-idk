package com.example.myclient.mixin;

import com.example.myclient.Foliage;
import com.example.myclient.Settings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// "Hide Foliage": tells the renderer leaves/grass/plants have nothing to draw.
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Shadow
    public abstract Block getBlock();

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void bertis$hideFoliage(CallbackInfoReturnable<RenderShape> cir) {
        if (!Settings.PERF_ON[Settings.FOLIAGE]) return;
        if (Foliage.isFoliage(this.getBlock())) cir.setReturnValue(RenderShape.INVISIBLE);
    }
}
