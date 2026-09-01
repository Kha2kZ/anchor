package com.khai2kzgaming.anchormacro.mixin;

import com.khai2kzgaming.anchormacro.AnchorMacroActions;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Unique
    private boolean anchorMacro$holdingRespawnAnchor;
    @Unique
    private BlockPos anchorMacro$placementPos;

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void anchorMacro$rememberAnchorPlacement(
            ClientPlayerEntity player,
            Hand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<ActionResult> callbackInfo
    ) {
        anchorMacro$holdingRespawnAnchor = !AnchorMacroActions.isInternalAnchorInteraction()
                && player.getStackInHand(hand).isOf(Items.RESPAWN_ANCHOR);
        anchorMacro$placementPos = anchorMacro$holdingRespawnAnchor
                ? hitResult.getBlockPos().offset(hitResult.getSide()).toImmutable()
                : null;
    }

    @Inject(method = "interactBlock", at = @At("TAIL"))
    private void anchorMacro$watchForAnchorPlacement(
            ClientPlayerEntity player,
            Hand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<ActionResult> callbackInfo
    ) {
        if (anchorMacro$holdingRespawnAnchor && anchorMacro$placementPos != null) {
            // The client result only says that a packet was sent. The tick tracker
            // waits for the actual anchor block to exist before starting the macro.
            AnchorMacroActions.onAnchorPlacementAttempt(anchorMacro$placementPos);
        }
        anchorMacro$holdingRespawnAnchor = false;
        anchorMacro$placementPos = null;
    }
}