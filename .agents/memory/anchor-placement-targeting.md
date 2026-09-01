---
name: Anchor placement targeting
description: Reliable identification of newly placed Respawn Anchors and deterministic defense-side selection.
---

Placement tracking must distinguish actual Respawn Anchor placement from the mod's own charge or detonation interactions. Capture the held item before interaction for predicted placements, suppress tracking around internal anchor clicks, and use server block-update transitions as the authoritative fallback. Defense placement should be selected once from the player's side at placement time and target the adjacent face of that exact anchor position.

**Why:** A generic accepted block interaction can compute the block above an anchor as a “new placement,” causing stacked anchors to target the wrong Y level; repeatedly deriving defense placement from the moving player can also choose the wrong side or reject valid half-block positions.

**How to apply:** Preserve full BlockPos identity through the queue, deduplicate only by full position, and retry unconfirmed actions without recomputing the saved defense side.

Placement detection and server-state acknowledgment must remain separate: enqueue an action only from the local placement interaction, then use authoritative block-state updates only to confirm that action. Never enqueue again from the acknowledgment path.

**Why:** Treating the same server update as a second placement queued duplicate actions, causing repeated charges and stale defense stages.

**How to apply:** Match acknowledgments to the pending action's exact anchor or saved defense target, and keep retrying the same saved action when the server has not confirmed it.

When Glowstone is the required defense item, never click the Respawn Anchor to place it; click the solid ground block below the exact adjacent target and use its top face.

**Why:** The anchor's normal block-use handler consumes Glowstone as a charge, while a ground-top interaction places Glowstone beside it without triggering another charge.

**How to apply:** Save the player-facing horizontal side at anchor placement time, target that adjacent block at the anchor's level, and build the hit result from the support block below it so camera aim is irrelevant.