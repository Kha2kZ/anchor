package com.khai2kzgaming.anchormacro;

import com.khai2kzgaming.anchormacro.config.AnchorMacroConfig;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class AnchorMacroActions {
    private static final int MAX_QUEUED_ACTIONS = 64;
    private static final long SERVER_ACK_TIMEOUT_TICKS = 40L;
    private static final long RETRY_DELAY_TICKS = 2L;
    private static final long PLACEMENT_ATTEMPT_TIMEOUT_TICKS = 100L;
    private static final Deque<PendingAction> pendingActions = new ArrayDeque<>();
    private static final Deque<PlacementAttempt> placementAttempts = new ArrayDeque<>();
    private static boolean internalAnchorInteraction;
    private static long clientTick;

    private AnchorMacroActions() {
    }

    public static void initialize() {
        // Kept as an explicit initialization point for the client entrypoint.
    }

    public static boolean isInternalAnchorInteraction() {
        return internalAnchorInteraction;
    }

    public static void onAnchorPlacementAttempt(BlockPos anchorPos) {
        if (!AnchorMacroClient.CONFIG.enabled) {
            return;
        }

        BlockPos immutablePos = anchorPos.toImmutable();
        if (hasTrackedAnchor(immutablePos)) {
            return;
        }

        if (placementAttempts.size() >= MAX_QUEUED_ACTIONS) {
            return;
        }
        placementAttempts.addLast(new PlacementAttempt(
                immutablePos,
                clientTick + PLACEMENT_ATTEMPT_TIMEOUT_TICKS
        ));
    }

    private static boolean hasTrackedAnchor(BlockPos anchorPos) {
        for (PendingAction pending : pendingActions) {
            if (pending.anchorPos.equals(anchorPos)) {
                return true;
            }
        }
        for (PlacementAttempt attempt : placementAttempts) {
            if (attempt.anchorPos.equals(anchorPos)) {
                return true;
            }
        }
        return false;
    }

    private static void queueConfirmedPlacements(ClientWorld world) {
        Iterator<PlacementAttempt> attempts = placementAttempts.iterator();
        while (attempts.hasNext()) {
            PlacementAttempt attempt = attempts.next();
            if (world.getBlockState(attempt.anchorPos).isOf(Blocks.RESPAWN_ANCHOR)) {
                if (pendingActions.size() < MAX_QUEUED_ACTIONS) {
                    addPendingAction(attempt.anchorPos);
                }
                attempts.remove();
            } else if (clientTick >= attempt.expiresAtTick) {
                attempts.remove();
            }
        }
    }

    private static void addPendingAction(BlockPos anchorPos) {
        MinecraftClient client = MinecraftClient.getInstance();
        Direction defenseDirection = client.player == null
                ? Direction.NORTH
                : findDefenseDirection(client.player, anchorPos);

        pendingActions.addLast(new PendingAction(
                anchorPos.toImmutable(),
                AnchorMacroClient.CONFIG.mode,
                AnchorMacroClient.CONFIG.safeHotbarSlot,
                AnchorMacroClient.CONFIG.chargeDelayMs,
                AnchorMacroClient.CONFIG.defenseDelayMs,
                defenseDirection
            ));
    }

    public static void onServerBlockUpdate(BlockPos pos, BlockState state) {
        for (PendingAction action : pendingActions) {
            if (action.anchorPos.equals(pos)) {
                if (state.isOf(Blocks.RESPAWN_ANCHOR)) {
                    action.placementConfirmed = true;
                    if (action.chargeRequested
                            && state.get(RespawnAnchorBlock.CHARGES) > action.chargesBeforeRequest) {
                        action.chargeConfirmed = true;
                    }
                } else if (action.stage == Stage.EXPLODE_ANCHOR && action.explosionRequested) {
                    action.explosionConfirmed = true;
                }
            }

            if (action.shieldTarget != null
                    && action.shieldTarget.equals(pos)
                    && action.defenseBlock != null
                    && state.isOf(action.defenseBlock)
                    && action.shieldRequested) {
                action.shieldConfirmed = true;
            }
        }
    }

    public static void tick(MinecraftClient client) {
        clientTick++;

        if (!AnchorMacroClient.CONFIG.enabled) {
            pendingActions.clear();
            placementAttempts.clear();
            return;
        }

        if (client.player == null || client.world == null) {
            return;
        }

        queueConfirmedPlacements(client.world);
        if (pendingActions.isEmpty()) {
            return;
        }

        if (client.interactionManager == null) {
            pendingActions.clear();
            return;
        }

        PendingAction action = pendingActions.peekFirst();

        if (!isRespawnAnchor(client.world, action.anchorPos)) {
            if (action.stage == Stage.EXPLODE_ANCHOR && action.explosionRequested) {
                complete(action);
                return;
            }
            // Placement is already confirmed before an action enters this queue.
            // If the player removes the anchor while it is processing, abandon
            // this action instead of reporting a misleading server timeout.
            complete(action);
            return;
        }

        if (!action.isReady(clientTick)) {
            return;
        }

        if (action.stage == Stage.CHARGE_ANCHOR) {
            int charges = client.world.getBlockState(action.anchorPos)
                    .get(RespawnAnchorBlock.CHARGES);

            if (action.chargeRequested) {
                if (action.chargeConfirmed || charges > action.chargesBeforeRequest) {
                    advanceAfterCharge(client, action);
                    return;
                }

                if (clientTick - action.chargeRequestedAtTick >= SERVER_ACK_TIMEOUT_TICKS) {
                    retryCharge(action);
                }
                return;
            }

            if (!selectHotbarItem(client.player, Items.GLOWSTONE)) {
                notify(client, "Anchor Macro: no Glowstone found in the hotbar.");
                complete(action);
                return;
            }

            if (charges >= RespawnAnchorBlock.MAX_CHARGES) {
                advanceAfterCharge(client, action);
                return;
            }

            ActionResult result = interactWithAnchor(client, action.anchorPos, action.defenseDirection);
            if (!result.isAccepted()) {
                retryCharge(action);
                return;
            }

            action.chargeRequested = true;
            action.chargesBeforeRequest = charges;
            action.chargeRequestedAtTick = clientTick;
            return;
        }

        if (action.stage == Stage.PLACE_SHIELD) {
            if (action.shieldRequested) {
                if (action.shieldConfirmed
                        || (action.defenseBlock != null
                        && client.world.getBlockState(action.shieldTarget).isOf(action.defenseBlock))) {
                    if (action.mode == AnchorMacroConfig.Mode.FULL_SAFE_ANCHOR) {
                        action.stage = Stage.EXPLODE_ANCHOR;
                        action.waitFor(action.defenseDelayMs);
                        return;
                    }
                    complete(action);
                    return;
                }

                if (clientTick - action.shieldRequestedAtTick >= SERVER_ACK_TIMEOUT_TICKS) {
                    retryShield(action);
                }
                return;
            }

            ShieldPlacement shield = action.shieldPlacement;
            if (shield == null) {
                Optional<ShieldPlacement> placement = findShieldPlacement(
                        client,
                        action.anchorPos,
                        action.defenseDirection
                );
                if (placement.isEmpty()) {
                    defenseFailed(client, action);
                    return;
                }
                shield = placement.get();
                action.shieldPlacement = shield;
            } else if (!client.world.getBlockState(shield.target).isReplaceable()
                    && (action.defenseBlock == null
                    || !client.world.getBlockState(shield.target).isOf(action.defenseBlock))) {
                defenseFailed(client, action);
                return;
            }

            int defenseSlot = action.defenseBlock == null
                    ? findDefenseBlockSlot(client.player, action.safeHotbarSlot)
                    : findHotbarBlock(client.player, action.defenseBlock);
            if (defenseSlot < 0) {
                defenseFailed(client, action);
                return;
            }
            client.player.getInventory().selectedSlot = defenseSlot;
            ItemStack defenseStack = client.player.getInventory().getStack(defenseSlot);
            Block defenseBlock = getPlaceableDefenseBlock(defenseStack);
            if (defenseBlock == null) {
                defenseFailed(client, action);
                return;
            }
            action.defenseBlock = defenseBlock;

            ActionResult result = client.interactionManager.interactBlock(
                    client.player,
                    Hand.MAIN_HAND,
                    shield.hitResult
            );
            client.player.getInventory().selectedSlot = action.safeHotbarSlot;
            if (!result.isAccepted()) {
                retryShield(action);
                return;
            }

            action.shieldTarget = shield.target;
            action.shieldRequested = true;
            action.shieldConfirmed = false;
            action.shieldRequestedAtTick = clientTick;
            return;
        }

        if (action.stage == Stage.EXPLODE_ANCHOR) {
            if (action.explosionRequested) {
                if (action.explosionConfirmed) {
                    complete(action);
                    return;
                }
                if (clientTick - action.explosionRequestedAtTick >= SERVER_ACK_TIMEOUT_TICKS) {
                    retryExplosion(action);
                }
                return;
            }

            ActionResult result = interactWithAnchor(client, action.anchorPos, action.defenseDirection);
            if (!result.isAccepted()) {
                retryExplosion(action);
                return;
            }
            action.explosionRequested = true;
            action.explosionConfirmed = false;
            action.explosionRequestedAtTick = clientTick;
        }
    }

    private static void advanceAfterCharge(MinecraftClient client, PendingAction action) {
        if (action.mode == AnchorMacroConfig.Mode.AUTO_GLOWSTONE) {
            complete(action);
            return;
        }

        if (action.mode == AnchorMacroConfig.Mode.FULL_ANCHOR) {
            client.player.getInventory().selectedSlot = action.safeHotbarSlot;
            action.stage = Stage.EXPLODE_ANCHOR;
        } else {
            action.stage = Stage.PLACE_SHIELD;
        }
        action.waitFor(action.defenseDelayMs);
    }

    private static void retryCharge(PendingAction action) {
        action.chargeRetries++;
        action.chargeRequested = false;
        action.chargeConfirmed = false;
        action.nextActionAtTick = clientTick + RETRY_DELAY_TICKS;
    }

    private static void retryShield(PendingAction action) {
        action.shieldRetries++;
        action.shieldRequested = false;
        action.shieldConfirmed = false;
        action.nextActionAtTick = clientTick + RETRY_DELAY_TICKS;
    }

    private static void retryExplosion(PendingAction action) {
        action.explosionRetries++;
        action.explosionRequested = false;
        action.explosionConfirmed = false;
        action.nextActionAtTick = clientTick + RETRY_DELAY_TICKS;
    }

    private static void defenseFailed(MinecraftClient client, PendingAction action) {
        client.player.getInventory().selectedSlot = action.safeHotbarSlot;
        if (action.mode == AnchorMacroConfig.Mode.FULL_SAFE_ANCHOR) {
            action.stage = Stage.EXPLODE_ANCHOR;
            action.waitFor(action.defenseDelayMs);
        } else {
            complete(action);
        }
    }

    private static void complete(PendingAction action) {
        if (pendingActions.peekFirst() == action) {
            pendingActions.removeFirst();
        }
    }

    private static boolean isRespawnAnchor(ClientWorld world, BlockPos pos) {
        return world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR);
    }

    private static ActionResult interactWithAnchor(
            MinecraftClient client,
            BlockPos anchorPos,
            Direction hitSide
    ) {
        Vec3d hitPosition = Vec3d.ofCenter(anchorPos);
        hitPosition = hitPosition.add(
                hitSide.getOffsetX() * 0.5,
                hitSide.getOffsetY() * 0.5,
                hitSide.getOffsetZ() * 0.5
        );
        BlockHitResult hit = new BlockHitResult(
                hitPosition,
                hitSide,
                anchorPos,
                false
        );
        internalAnchorInteraction = true;
        try {
            return client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
        } finally {
            internalAnchorInteraction = false;
        }
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

    private static java.util.Optional<ShieldPlacement> findShieldPlacement(
            MinecraftClient client,
            BlockPos anchorPos,
            Direction defenseDirection
    ) {
        ClientWorld world = client.world;
        if (world == null) {
            return Optional.empty();
        }
        BlockPos target = anchorPos.offset(defenseDirection);
        if (!world.getBlockState(target).isReplaceable()) {
            return Optional.empty();
        }
        if (client.player != null
                && client.player.getBoundingBox().intersects(new Box(target))) {
            return Optional.empty();
        }

        // The anchor is deliberately the support block. This works regardless
        // of the player's camera aim because the packet contains this exact
        // block position and face. The defense item must not be Glowstone:
        // Glowstone would invoke the anchor and charge it again.
        Vec3d hitPosition = Vec3d.ofCenter(anchorPos).add(
                defenseDirection.getOffsetX() * 0.5,
                0.0,
                defenseDirection.getOffsetZ() * 0.5
        );
        BlockHitResult hit = new BlockHitResult(
                hitPosition,
                defenseDirection,
                anchorPos,
                false
        );
        return Optional.of(new ShieldPlacement(target, hit));
    }

    private static int findDefenseBlockSlot(ClientPlayerEntity player, int preferredSlot) {
        int clampedPreferredSlot = AnchorMacroConfig.clampHotbarSlot(preferredSlot);
        if (getPlaceableDefenseBlock(player.getInventory().getStack(clampedPreferredSlot)) != null) {
            return clampedPreferredSlot;
        }
        for (int slot = 0; slot < 9; slot++) {
            if (getPlaceableDefenseBlock(player.getInventory().getStack(slot)) != null) {
                return slot;
            }
        }
        return -1;
    }

    private static int findHotbarBlock(ClientPlayerEntity player, Block block) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (getPlaceableDefenseBlock(stack) == block) {
                return slot;
            }
        }
        return -1;
    }

    private static Block getPlaceableDefenseBlock(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }
        Block block = blockItem.getBlock();
        return block == Blocks.GLOWSTONE || block == Blocks.RESPAWN_ANCHOR
                ? null
                : block;
    }

    private static Direction findDefenseDirection(
            ClientPlayerEntity player,
            BlockPos anchorPos
    ) {
        double deltaX = player.getX() - (anchorPos.getX() + 0.5);
        double deltaZ = player.getZ() - (anchorPos.getZ() + 0.5);
        if (Math.abs(deltaX) < 0.001 && Math.abs(deltaZ) < 0.001) {
            return player.getHorizontalFacing().getOpposite();
        }
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            return deltaX >= 0.0 ? Direction.EAST : Direction.WEST;
        }
        return deltaZ >= 0.0 ? Direction.SOUTH : Direction.NORTH;
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
        private final int chargeDelayMs;
        private final int defenseDelayMs;
        private final Direction defenseDirection;
        private Stage stage = Stage.CHARGE_ANCHOR;
        private boolean placementConfirmed;
        private boolean chargeRequested;
        private boolean chargeConfirmed;
        private int chargesBeforeRequest;
        private long chargeRequestedAtTick;
        private int chargeRetries;
        private boolean shieldRequested;
        private boolean shieldConfirmed;
        private BlockPos shieldTarget;
        private ShieldPlacement shieldPlacement;
        private Block defenseBlock;
        private long shieldRequestedAtTick;
        private int shieldRetries;
        private boolean explosionRequested;
        private boolean explosionConfirmed;
        private long explosionRequestedAtTick;
        private int explosionRetries;
        private long nextActionAtTick;

        private PendingAction(
                BlockPos anchorPos,
                AnchorMacroConfig.Mode mode,
                int safeHotbarSlot,
                int chargeDelayMs,
                int defenseDelayMs,
                Direction defenseDirection
        ) {
            this.anchorPos = anchorPos;
            this.mode = mode;
            this.safeHotbarSlot = AnchorMacroConfig.clampHotbarSlot(safeHotbarSlot);
            this.chargeDelayMs = AnchorMacroConfig.clampDelayMs(chargeDelayMs);
            this.defenseDelayMs = AnchorMacroConfig.clampDelayMs(defenseDelayMs);
            this.defenseDirection = defenseDirection;
            this.nextActionAtTick = clientTick + millisecondsToTicks(this.chargeDelayMs);
        }

        private boolean isReady(long tick) {
            return tick >= nextActionAtTick;
        }

        private void waitFor(int delayMs) {
            nextActionAtTick = clientTick + millisecondsToTicks(delayMs);
        }

        private static long millisecondsToTicks(int milliseconds) {
            return (milliseconds + 49L) / 50L;
        }
    }

    private static final class PlacementAttempt {
        private final BlockPos anchorPos;
        private final long expiresAtTick;

        private PlacementAttempt(BlockPos anchorPos, long expiresAtTick) {
            this.anchorPos = anchorPos;
            this.expiresAtTick = expiresAtTick;
        }
    }

    private record ShieldPlacement(BlockPos target, BlockHitResult hitResult) {
    }
}