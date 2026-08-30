package com.khai2kzgaming.totemhighlighter.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    private static final int TOTEM_OVERLAY = 0x55FF2020;
    private static final int TOTEM_BORDER = 0xFFFF3333;

    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void totemSlotHighlighter$drawTotemHighlight(
            DrawContext context,
            Slot slot,
            CallbackInfo callbackInfo
    ) {
        if (!slot.getStack().isOf(Items.TOTEM_OF_UNDYING)) {
            return;
        }

        // Restrict the effect to the player's inventory, including the hotbar.
        // This avoids changing the appearance of container slots such as chests.
        if (!(slot.inventory instanceof PlayerInventory)) {
            return;
        }

        int x = slot.x;
        int y = slot.y;

        // A translucent fill keeps the totem icon and item count readable.
        context.fill(x, y, x + 16, y + 16, TOTEM_OVERLAY);

        // A one-pixel border makes the highlight easy to spot at a glance.
        context.fill(x, y, x + 16, y + 1, TOTEM_BORDER);
        context.fill(x, y + 15, x + 16, y + 16, TOTEM_BORDER);
        context.fill(x, y, x + 1, y + 16, TOTEM_BORDER);
        context.fill(x + 15, y, x + 16, y + 16, TOTEM_BORDER);
    }
}