package com.khai2kzgaming.anchormacro;

import com.khai2kzgaming.anchormacro.config.AnchorMacroConfig;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class AnchorMacroActions {
    private static final double MAX_REACH_SQUARED = 20.25; // (4.5 blocks)^2
    private static final long STAGE_TIMEOUT_TICKS = 10L;   // Timeout ngắn (0.5s) chống treo vùng
    private static final long TASK_LIFETIME_TICKS = 40L;   // Tối đa 2s cho 1 cycle full

    // Chuyển sang Map độc lập từng BlockPos thay vì Deque nghẽn
    private static final Map<BlockPos, AnchorTask> activeTasks = new HashMap<>();
    private static boolean internalAnchorInteraction;
    private static long clientTick;
    private static ClientWorld trackedWorld;

    private AnchorMacroActions() {}

    public static void initialize() {}

    public static boolean isInternalAnchorInteraction() {
        return internalAnchorInteraction;
    }

    /**
     * Gọi ngay khi client vừa gửi packet đặt Anchor. Ghi đè tức thì mọi task cũ ở vị trí này.
     */
    public static void onAnchorPlacementAttempt(BlockPos anchorPos) {
        if (!AnchorMacroClient.CONFIG.enabled) return;

        BlockPos pos = anchorPos.toImmutable();
        activeTasks.put(pos, new AnchorTask(
                pos,
                AnchorMacroClient.CONFIG.mode,
                AnchorMacroClient.CONFIG.safeHotbarSlot,
                AnchorMacroClient.CONFIG.chargeDelayMs,
                AnchorMacroClient.CONFIG.defenseDelayMs
        ));
    }

    public static void onServerBlockUpdate(BlockPos pos, BlockState state) {
        observeBlockState(pos, state);
    }

    public static void onClientBlockStateChange(BlockPos pos, BlockState state) {
        observeBlockState(pos, state);
    }

    private static void observeBlockState(BlockPos pos, BlockState state) {
        AnchorTask task = activeTasks.get(pos);
        if (task != null) {
            if (state.isOf(Blocks.RESPAWN_ANCHOR)) {
                if (task.chargeRequested && state.get(RespawnAnchorBlock.CHARGES) > task.chargesBeforeRequest) {
                    task.chargeConfirmed = true;
                }
            } else if (!state.isOf(Blocks.RESPAWN_ANCHOR) && task.stage == Stage.EXPLODE_ANCHOR) {
                activeTasks.remove(pos); // Xóa ngay khi block bị phá hủy/nổ
            }
        }
    }

    public static void tick(MinecraftClient client) {
        if (client.world != trackedWorld) {
            activeTasks.clear();
            trackedWorld = client.world;
            clientTick = 0L;
        }
        clientTick++;

        if (!AnchorMacroClient.CONFIG.enabled || client.player == null || client.world == null || client.interactionManager == null) {
            activeTasks.clear();
            return;
        }

        // 1. Dọn dẹp các task hỏng, rác, quá hạn hoặc mất block
        cleanupTasks(client.world, client.player);

        if (activeTasks.isEmpty()) return;

        // 2. Tìm Task gần người chơi nhất trong tầm Reach hợp lệ (Tránh bị khóa vùng)
        AnchorTask targetTask = findClosestValidTask(client.player, client.world);
        if (targetTask == null) return;

        if (!targetTask.isReady(clientTick)) return;

        // --- STAGE 1: CHARGE ANCHOR ---
        if (targetTask.stage == Stage.CHARGE_ANCHOR) {
            int charges = client.world.getBlockState(targetTask.anchorPos).get(RespawnAnchorBlock.CHARGES);

            if (targetTask.chargeRequested) {
                if (targetTask.chargeConfirmed || charges > targetTask.chargesBeforeRequest) {
                    advanceAfterCharge(client, targetTask);
                    return;
                }
                if (clientTick - targetTask.stageStartTick >= STAGE_TIMEOUT_TICKS) {
                    activeTasks.remove(targetTask.anchorPos); // Hủy nếu kẹt
                }
                return;
            }

            if (!selectHotbarItem(client.player, Items.GLOWSTONE)) {
                activeTasks.remove(targetTask.anchorPos);
                return;
            }

            if (charges >= RespawnAnchorBlock.MAX_CHARGES) {
                advanceAfterCharge(client, targetTask);
                return;
            }

            ActionResult result = interactLegitWithBlock(client, targetTask.anchorPos);
            if (result.isAccepted()) {
                targetTask.chargeRequested = true;
                targetTask.chargesBeforeRequest = charges;
                targetTask.stageStartTick = clientTick;
            } else {
                activeTasks.remove(targetTask.anchorPos);
            }
            return;
        }

        // --- STAGE 2: PLACE SHIELD ---
        if (targetTask.stage == Stage.PLACE_SHIELD) {
            if (targetTask.shieldRequested) {
                if (targetTask.shieldConfirmed || client.world.getBlockState(targetTask.shieldTarget).isOf(Blocks.GLOWSTONE)) {
                    if (targetTask.mode == AnchorMacroConfig.Mode.FULL_SAFE_ANCHOR) {
                        targetTask.stage = Stage.EXPLODE_ANCHOR;
                        targetTask.stageStartTick = clientTick;
                        targetTask.waitFor(targetTask.defenseDelayMs);
                        return;
                    }
                    activeTasks.remove(targetTask.anchorPos);
                    return;
                }
                if (clientTick - targetTask.stageStartTick >= STAGE_TIMEOUT_TICKS) {
                    defenseFailed(client, targetTask);
                }
                return;
            }

            ShieldPlacement shield = targetTask.shieldPlacement;
            if (shield == null) {
                Optional<ShieldPlacement> placement = findOptimalShieldPlacement(client.player, client.world, targetTask.anchorPos);
                if (placement.isEmpty()) {
                    defenseFailed(client, targetTask); // Không có sàn -> Bỏ shield nổ luôn
                    return;
                }
                shield = placement.get();
                targetTask.shieldPlacement = shield;
            }

            if (!selectHotbarItem(client.player, Items.GLOWSTONE)) {
                defenseFailed(client, targetTask);
                return;
            }

            ActionResult result = interactLegitWithHitResult(client, shield.hitResult);
            client.player.getInventory().selectedSlot = targetTask.safeHotbarSlot;

            if (result.isAccepted()) {
                targetTask.shieldTarget = shield.target;
                targetTask.shieldRequested = true;
                targetTask.stageStartTick = clientTick;
            } else {
                defenseFailed(client, targetTask);
            }
            return;
        }

        // --- STAGE 3: EXPLODE ANCHOR ---
        if (targetTask.stage == Stage.EXPLODE_ANCHOR) {
            if (targetTask.explosionRequested) {
                activeTasks.remove(targetTask.anchorPos);
                return;
            }

            client.player.getInventory().selectedSlot = targetTask.safeHotbarSlot;

            ActionResult result = interactLegitWithBlock(client, targetTask.anchorPos);
            if (result.isAccepted()) {
                targetTask.explosionRequested = true;
                activeTasks.remove(targetTask.anchorPos); // Nổ xong giải phóng ngay
            } else {
                activeTasks.remove(targetTask.anchorPos);
            }
        }
    }

    private static void advanceAfterCharge(MinecraftClient client, AnchorTask task) {
        if (task.mode == AnchorMacroConfig.Mode.AUTO_GLOWSTONE) {
            activeTasks.remove(task.anchorPos);
            return;
        }

        if (task.mode == AnchorMacroConfig.Mode.FULL_ANCHOR) {
            client.player.getInventory().selectedSlot = task.safeHotbarSlot;
            task.stage = Stage.EXPLODE_ANCHOR;
        } else {
            task.stage = Stage.PLACE_SHIELD;
        }
        task.stageStartTick = clientTick;
        task.waitFor(task.defenseDelayMs);
    }

    private static void defenseFailed(MinecraftClient client, AnchorTask task) {
        if (client.player != null) {
            client.player.getInventory().selectedSlot = task.safeHotbarSlot;
        }
        task.stage = Stage.EXPLODE_ANCHOR;
        task.stageStartTick = clientTick;
        task.waitFor(task.defenseDelayMs);
    }

    private static void cleanupTasks(ClientWorld world, ClientPlayerEntity player) {
        Vec3d eyePos = player.getEyePos();
        Iterator<Map.Entry<BlockPos, AnchorTask>> iterator = activeTasks.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, AnchorTask> entry = iterator.next();
            BlockPos pos = entry.getKey();
            AnchorTask task = entry.getValue();

            // Xóa nếu quá hạn tổng thời gian, hoặc block không phải Anchor nữa
            if (clientTick - task.createdTick > TASK_LIFETIME_TICKS || !world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR)) {
                iterator.remove();
                continue;
            }

            // Xóa nếu quá xa khoảng cách Reach (Do nổ bị văng lùi)
            if (eyePos.squaredDistanceTo(Vec3d.ofCenter(pos)) > MAX_REACH_SQUARED) {
                iterator.remove();
            }
        }
    }

    private static AnchorTask findClosestValidTask(ClientPlayerEntity player, ClientWorld world) {
        Vec3d eyePos = player.getEyePos();
        AnchorTask closestTask = null;
        double minDistance = Double.MAX_VALUE;

        for (AnchorTask task : activeTasks.values()) {
            double distSq = eyePos.squaredDistanceTo(Vec3d.ofCenter(task.anchorPos));
            if (distSq <= MAX_REACH_SQUARED && distSq < minDistance) {
                minDistance = distSq;
                closestTask = task;
            }
        }

        return closestTask;
    }

    // =========================================================================
    // LEGIT PACKET SYSTEM (SILENT LOOK + SWING HAND)
    // =========================================================================

    private static ActionResult interactLegitWithBlock(MinecraftClient client, BlockPos targetPos) {
        if (client.player == null || client.world == null || client.interactionManager == null) {
            return ActionResult.PASS;
        }

        BlockHitResult hitResult = calculateLegitHitResult(client.player, targetPos);
        return interactLegitWithHitResult(client, hitResult);
    }

    private static ActionResult interactLegitWithHitResult(MinecraftClient client, BlockHitResult hitResult) {
        if (client.player == null || client.interactionManager == null || client.player.networkHandler == null) {
            return ActionResult.PASS;
        }

        // Check lại Reach distance chuẩn xác từng điểm hit
        if (client.player.getEyePos().squaredDistanceTo(hitResult.getPos()) > MAX_REACH_SQUARED) {
            return ActionResult.PASS;
        }

        // 1. Silent Look Packet
        sendSilentLook(client.player, hitResult.getPos());

        // 2. Interact Block Packet
        internalAnchorInteraction = true;
        ActionResult result;
        try {
            result = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
        } finally {
            internalAnchorInteraction = false;
        }

        // 3. Hand Swing Packet (Quan trọng để qua mặt Anti-cheat & Server Vanilla)
        if (result.isAccepted()) {
            client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }

        return result;
    }

    private static void sendSilentLook(ClientPlayerEntity player, Vec3d targetVec) {
        Vec3d eyePos = player.getEyePos();
        double dx = targetVec.x - eyePos.x;
        double dy = targetVec.y - eyePos.y;
        double dz = targetVec.z - eyePos.z;
        double dh = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dh));

        if (player.networkHandler != null) {
            player.networkHandler.sendPacket(
                new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, player.isOnGround())
            );
        }
    }

    private static BlockHitResult calculateLegitHitResult(ClientPlayerEntity player, BlockPos targetPos) {
        Vec3d eyePos = player.getEyePos();
        Direction bestFace = Direction.UP;
        double minDistance = Double.MAX_VALUE;

        for (Direction side : Direction.values()) {
            Vec3d faceCenter = Vec3d.ofCenter(targetPos).add(
                side.getOffsetX() * 0.5,
                side.getOffsetY() * 0.5,
                side.getOffsetZ() * 0.5
            );
            double dist = eyePos.squaredDistanceTo(faceCenter);
            if (dist < minDistance) {
                minDistance = dist;
                bestFace = side;
            }
        }

        Vec3d hitVec = Vec3d.ofCenter(targetPos).add(
            bestFace.getOffsetX() * 0.45,
            bestFace.getOffsetY() * 0.45,
            bestFace.getOffsetZ() * 0.45
        );

        return new BlockHitResult(hitVec, bestFace, targetPos, false);
    }

    private static Optional<ShieldPlacement> findOptimalShieldPlacement(ClientPlayerEntity player, ClientWorld world, BlockPos anchorPos) {
        double dx = player.getX() - (anchorPos.getX() + 0.5);
        double dz = player.getZ() - (anchorPos.getZ() + 0.5);

        int primaryX = 0;
        int primaryZ = 0;

        if (Math.abs(dx) > 0.35) primaryX = dx > 0 ? 1 : -1;
        if (Math.abs(dz) > 0.35) primaryZ = dz > 0 ? 1 : -1;

        if (primaryX == 0 && primaryZ == 0) {
            Direction facing = player.getHorizontalFacing();
            primaryX = facing.getOffsetX();
            primaryZ = facing.getOffsetZ();
        }

        int[][] candidateOffsets = new int[][] {
            {primaryX, primaryZ},
            {primaryX, 0},
            {0, primaryZ},
            {primaryX, -primaryZ},
            {-primaryX, primaryZ},
            {-primaryX, 0},
            {0, -primaryZ},
            {-primaryX, -primaryZ}
        };

        for (int[] offset : candidateOffsets) {
            int ox = offset[0];
            int oz = offset[1];
            if (ox == 0 && oz == 0) continue;

            BlockPos shieldTarget = anchorPos.add(ox, 0, oz);

            if (!world.getBlockState(shieldTarget).isReplaceable()) continue;
            if (player.getBoundingBox().intersects(new Box(shieldTarget))) continue;

            BlockPos supportFloor = shieldTarget.down();
            if (world.getBlockState(supportFloor).isSolidBlock(world, supportFloor)) {
                Vec3d hitVec = Vec3d.ofCenter(supportFloor).add(0.0, 0.5, 0.0);
                BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, supportFloor, false);
                return Optional.of(new ShieldPlacement(shieldTarget, hit));
            }

            for (Direction side : Direction.values()) {
                BlockPos neighbor = shieldTarget.offset(side);
                if (neighbor.equals(anchorPos)) continue;

                if (world.getBlockState(neighbor).isSolidBlock(world, neighbor)) {
                    Direction hitSide = side.getOpposite();
                    Vec3d hitVec = Vec3d.ofCenter(neighbor).add(
                        hitSide.getOffsetX() * 0.5,
                        hitSide.getOffsetY() * 0.5,
                        hitSide.getOffsetZ() * 0.5
                    );
                    BlockHitResult hit = new BlockHitResult(hitVec, hitSide, neighbor, false);
                    return Optional.of(new ShieldPlacement(shieldTarget, hit));
                }
            }
        }

        return Optional.empty();
    }

    private static boolean selectHotbarItem(ClientPlayerEntity player, Item item) {
        int slot = findHotbarItem(player, item);
        if (slot < 0) return false;
        player.getInventory().selectedSlot = slot;
        return true;
    }

    private static int findHotbarItem(ClientPlayerEntity player, Item item) {
        int currentSlot = player.getInventory().selectedSlot;
        if (player.getInventory().getStack(currentSlot).isOf(item)) {
            return currentSlot;
        }
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getStack(slot).isOf(item)) return slot;
        }
        return -1;
    }

    private enum Stage { CHARGE_ANCHOR, PLACE_SHIELD, EXPLODE_ANCHOR }

    private static final class AnchorTask {
        private final BlockPos anchorPos;
        private final AnchorMacroConfig.Mode mode;
        private final int safeHotbarSlot;
        private final int chargeDelayMs;
        private final int defenseDelayMs;
        private final long createdTick;

        private Stage stage = Stage.CHARGE_ANCHOR;
        private long stageStartTick;
        private boolean chargeRequested;
        private boolean chargeConfirmed;
        private int chargesBeforeRequest;
        private boolean shieldRequested;
        private boolean shieldConfirmed;
        private BlockPos shieldTarget;
        private ShieldPlacement shieldPlacement;
        private boolean explosionRequested;
        private long nextActionAtTick;

        private AnchorTask(BlockPos anchorPos, AnchorMacroConfig.Mode mode, int safeHotbarSlot, int chargeDelayMs, int defenseDelayMs) {
            this.anchorPos = anchorPos;
            this.mode = mode;
            this.safeHotbarSlot = AnchorMacroConfig.clampHotbarSlot(safeHotbarSlot);
            this.chargeDelayMs = AnchorMacroConfig.clampDelayMs(chargeDelayMs);
            this.defenseDelayMs = AnchorMacroConfig.clampDelayMs(defenseDelayMs);
            this.createdTick = clientTick;
            this.stageStartTick = clientTick;
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

    private record ShieldPlacement(BlockPos target, BlockHitResult hitResult) {}
}