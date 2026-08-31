package com.khai2kzgaming.anchormacro;

import com.khai2kzgaming.anchormacro.config.AnchorMacroConfig;
import java.util.Optional;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class AnchorMacroActions {
    private static PendingAction pendingAction;

    private AnchorMacroActions() {
    }

    public static void initialize() {
        // Kept as an explicit initialization point for the client entrypoint.
    }

    public static void onAnchorPlaced(BlockPos anchorPos) {
        if (!AnchorMacroClient.CONFIG.enabled) {
            return;
        }

        pendingAction = new PendingAction(
                anchorPos.toImmutable(),
                AnchorMacroClient.CONFIG.mode,
                AnchorMacroClient.CONFIG.safeHotbarSlot
        );
    }

    public static void tick(MinecraftClient client) {
        if (pendingAction == null || client.player == null || client.world == null) {
            return;
        }

        if (client.interactionManager == null) {
            pendingAction = null;
            return;
        }

        PendingAction action = pendingAction;
        if (action.waitTicks > 0) {
            action.waitTicks--;
            return;
        }

        if (!isRespawnAnchor(client.world, action.anchorPos)) {
            if (++action.waitTicks > 10) {
                notify(client, "Anchor Macro: anchor placement was not confirmed.");
                pendingAction = null;
            }
            return;
        }

        if (action.stage == Stage.CHARGE_ANCHOR) {
            if (!selectHotbarItem(client.player, Items.GLOWSTONE)) {
                notify(client, "Anchor Macro: no Glowstone found in the hotbar.");
                pendingAction = null;
                return;
            }

            int charges = client.world.getBlockState(action.anchorPos)
                    .get(RespawnAnchorBlock.CHARGES);
            if (charges < RespawnAnchorBlock.MAX_CHARGES) {
                ActionResult result = interactWithAnchor(client, action.anchorPos);
                if (!result.isAccepted()) {
                    notify(client, "Anchor Macro: could not charge the anchor.");
                    pendingAction = null;
                    return;
                }
            }

            if (action.mode == AnchorMacroConfig.Mode.AUTO_GLOWSTONE) {
                pendingAction = null;
                return;
            }

            if (action.mode == AnchorMacroConfig.Mode.FULL_ANCHOR) {
                action.stage = Stage.EXPLODE_ANCHOR;
            } else {
                action.stage = Stage.PLACE_SHIELD;
            }
            action.waitTicks = 1;
            return;
        }

        if (action.stage == Stage.PLACE_SHIELD) {
            Optional<ShieldPlacement> placement = findShieldPlacement(client.world, client.player);
            if (placement.isEmpty()) {
                notify(client, "Anchor Macro: no supported space in front of you for Glowstone.");
                client.player.getInventory().selectedSlot = action.safeHotbarSlot;
                pendingAction = null;
                return;
            }

            if (!selectHotbarItem(client.player, Items.GLOWSTONE)) {
                notify(client, "Anchor Macro: no Glowstone found for the shield.");
                client.player.getInventory().selectedSlot = action.safeHotbarSlot;
                pendingAction = null;
                return;
            }

            ShieldPlacement shield = placement.get();
            ActionResult result = client.interactionManager.interactBlock(
                    client.player,
                    Hand.MAIN_HAND,
                    shield.hitResult
            );
            client.player.getInventory().selectedSlot = action.safeHotbarSlot;
            if (!result.isAccepted()) {
                notify(client, "Anchor Macro: Glowstone shield placement failed.");
                pendingAction = null;
                return;
            }

            if (action.mode == AnchorMacroConfig.Mode.FULL_SAFE_ANCHOR) {
                action.stage = Stage.EXPLODE_ANCHOR;
                action.waitTicks = 1;
                return;
            }
            pendingAction = null;
        }

        if (action.stage == Stage.EXPLODE_ANCHOR) {
            ActionResult result = interactWithAnchor(client, action.anchorPos);
            if (!result.isAccepted()) {
                notify(client, "Anchor Macro: could not activate the charged anchor.");
            }
            pendingAction = null;
        }
    }

    private static boolean isRespawnAnchor(ClientWorld world, BlockPos pos) {
        return world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR);
    }

    private static ActionResult interactWithAnchor(MinecraftClient client, BlockPos anchorPos) {
        BlockHitResult hit = new BlockHitResult(
                Vec3d.ofCenter(anchorPos),
                Direction.UP,
                anchorPos,
                false
        );
        return client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
    }

    private static boolean selectHotbarItem(ClientPlayerEntity player, Item item) {
        int slot = findHotbarItem(player, item);
        if (slot < 0) {
            return false;
        }
        player.getInventory().selectedSlot = slot;
        return true;
    }

    private static int findHotbarItem(ClientPlayerEntity player, Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getStack(slot).isOf(item)) {
                return slot;
            }
        }
        return -1;
    }

    private static Optional<ShieldPlacement> findShieldPlacement(
            ClientWorld world,
            ClientPlayerEntity player
    ) {
        Direction facing = player.getHorizontalFacing();
        BlockPos base = player.getBlockPos();

        for (int distance = 1; distance <= 2; distance++) {
            BlockPos target = base.offset(facing, distance);
            BlockPos support = target.down();
            if (!world.getBlockState(target).isReplaceable()
                    || world.getBlockState(support).isAir()) {
                continue;
            }

            BlockHitResult hit = new BlockHitResult(
                    Vec3d.ofCenter(support).add(0.0, 0.5, 0.0),
                    Direction.UP,
                    support,
                    false
            );
            return Optional.of(new ShieldPlacement(target, hit));
        }

        return Optional.empty();
    }

    private static void notify(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal(message), true);
        }
    }

    private enum Stage {
        CHARGE_ANCHOR,
        PLACE_SHIELD,
        EXPLODE_ANCHOR
    }

    private static final class PendingAction {
        private final BlockPos anchorPos;
        private final AnchorMacroConfig.Mode mode;
        private final int safeHotbarSlot;
        private Stage stage = Stage.CHARGE_ANCHOR;
        private int waitTicks = 1;

        private PendingAction(
                BlockPos anchorPos,
                AnchorMacroConfig.Mode mode,
                int safeHotbarSlot
        ) {
            this.anchorPos = anchorPos;
            this.mode = mode;
            this.safeHotbarSlot = AnchorMacroConfig.clampHotbarSlot(safeHotbarSlot);
        }
    }

    private record ShieldPlacement(BlockPos target, BlockHitResult hitResult) {
    }
}