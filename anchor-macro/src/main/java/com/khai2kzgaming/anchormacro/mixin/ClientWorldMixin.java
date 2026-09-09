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

    /**
     * Bắt trực tiếp Server Packet S2C (như khi Server gửi tín hiệu Anchor nổ/đổi charge)
     */
    @Inject(method = "handleBlockUpdate", at = @At("TAIL"))
    private void anchorMacro$observeServerBlockUpdate(
            BlockPos pos,
            BlockState state,
            int flags,
            CallbackInfo callbackInfo
    ) {
        AnchorMacroActions.onServerBlockUpdate(pos, state);
    }

    /**
     * Bắt cập nhật khối từ Client Prediction (Giúp phản hồi tức thì ở Ping 60-100ms)
     */
    @Inject(method = "setBlockState", at = @At("TAIL"))
    private void anchorMacro$observeClientBlockStateChange(
            BlockPos pos,
            BlockState state,
            int flags,
            int maxUpdateDepth,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (callbackInfo.getReturnValue() != null && callbackInfo.getReturnValue()) {
            AnchorMacroActions.onClientBlockStateChange(pos, state);
        }
    }
}