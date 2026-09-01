package com.khai2kzgaming.anchormacro.mixin;

import com.khai2kzgaming.anchormacro.AnchorMacroActions;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    @Inject(method = "handleBlockUpdate", at = @At("TAIL"))
    private void anchorMacro$observeServerBlockUpdate(
            BlockPos pos,
            BlockState state,
            int flags,
            CallbackInfo callbackInfo
    ) {
        AnchorMacroActions.onServerBlockUpdate(pos, state);
    }

    @Inject(method = "setBlockState", at = @At("TAIL"))
    private void anchorMacro$observeClientBlockStateChange(
            BlockPos pos,
            BlockState state,
            int flags,
            int maxUpdateDepth,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (callbackInfo.getReturnValue()) {
            AnchorMacroActions.onClientBlockStateChange(pos, state);
        }
    }
}