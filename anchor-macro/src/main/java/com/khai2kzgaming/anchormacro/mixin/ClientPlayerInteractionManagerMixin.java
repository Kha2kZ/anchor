package com.khai2kzgaming.anchormacro.mixin;

import com.khai2kzgaming.anchormacro.AnchorMacroActions;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
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
        if (!AnchorMacroActions.isInternalAnchorInteraction()
                && player.getStackInHand(hand).isOf(Items.RESPAWN_ANCHOR)) {
            // Track at HEAD so a client-side PASS/FAIL result cannot hide a
            // placement that the server accepts. The tick tracker validates the
            // actual block before starting the action.
            AnchorMacroActions.onAnchorPlacementAttempt(
                    hitResult.getBlockPos().offset(hitResult.getSide())
            );
        }
    }
}