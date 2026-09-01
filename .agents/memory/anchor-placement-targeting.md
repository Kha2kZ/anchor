---
name: Anchor placement targeting
description: Reliable identification of newly placed Respawn Anchors and deterministic defense-side selection.
---

Placement tracking must distinguish actual Respawn Anchor placement from the mod's own charge or detonation interactions. Capture the held item before interaction for predicted placements, suppress tracking around internal anchor clicks, and use server block-update transitions as the authoritative fallback. Defense placement should be selected once from the player's side at placement time and target the adjacent face of that exact anchor position.

**Why:** A generic accepted block interaction can compute the block above an anchor as a “new placement,” causing stacked anchors to target the wrong Y level; repeatedly deriving defense placement from the moving player can also choose the wrong side or reject valid half-block positions.

**How to apply:** Preserve full BlockPos identity through the queue, deduplicate only by full position, and retry unconfirmed actions without recomputing the saved defense side.