package com.khai2kzgaming.anchormacro.mixin;

import com.khai2kzgaming.anchormacro.AnchorMacroActions;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void anchorMacro$trackAnchorPlacementAttempt(
            ClientPlayerEntity player,
            Hand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<ActionResult> callbackInfo
    ) {
        if (AnchorMacroActions.isInternalAnchorInteraction()) return;
        if (!player.getStackInHand(hand).isOf(Items.RESPAWN_ANCHOR)) return;

        BlockPos clickedPos = hitResult.getBlockPos();
        BlockState clickedState = player.getWorld().getBlockState(clickedPos);

        // Nếu click vào Respawn Anchor có sẵn mà không giữ Shift -> Đây là tương tác, không phải đặt Anchor mới
        if (clickedState.isOf(Blocks.RESPAWN_ANCHOR) && !player.isSneaking()) {
            return;
        }

        // Xử lý địa hình Sandbox (Cỏ, Tuyết, Nước, Khối Replaceable):
        // Nếu block bị click có thể thay thế, Anchor sẽ nằm NGAY TẠI clickedPos.
        // Nếu click vào mặt cứng, Anchor mới nằm ở offset(side).
        BlockPos targetPlacedPos = clickedState.isReplaceable()
                ? clickedPos
                : clickedPos.offset(hitResult.getSide());

        AnchorMacroActions.onAnchorPlacementAttempt(targetPlacedPos);
    }
}