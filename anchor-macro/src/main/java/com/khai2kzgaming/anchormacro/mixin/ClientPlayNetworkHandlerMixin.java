package com.khai2kzgaming.anchormacro.mixin;

import com.khai2kzgaming.anchormacro.AnchorMacroActions;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onBlockUpdate", at = @At("HEAD"))
    private void anchorMacro$watchForServerAnchorPlacement(
            BlockUpdateS2CPacket packet,
            CallbackInfo callbackInfo
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null
                || !packet.getState().isOf(Blocks.RESPAWN_ANCHOR)
                || client.world.getBlockState(packet.getPos()).isOf(Blocks.RESPAWN_ANCHOR)) {
            return;
        }

        AnchorMacroActions.onAnchorPlaced(packet.getPos());
    }
}