package com.khai2kzgaming.anchormacro.mixin;

import com.khai2kzgaming.anchormacro.AnchorMacroActions;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
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
    @Inject(method = "interactBlock", at = @At("TAIL"))
    private void anchorMacro$watchForAnchorPlacement(
            ClientPlayerEntity player,
            Hand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<ActionResult> callbackInfo
    ) {
        if (hand != Hand.MAIN_HAND
                || !callbackInfo.getReturnValue().isAccepted()) {
            return;
        }

        BlockPos placedPos = hitResult.getBlockPos().offset(hitResult.getSide());
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null
                && client.world.getBlockState(placedPos).isOf(Blocks.RESPAWN_ANCHOR)) {
            AnchorMacroActions.onAnchorPlaced(placedPos);
        }
    }
}