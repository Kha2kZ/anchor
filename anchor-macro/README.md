# Anchor Macro

A client-side Fabric mod for Minecraft 1.20.6 that automates the safe setup
steps around Respawn Anchors.

## Modes

### Auto Glowstone

After successfully placing a Respawn Anchor, the mod switches to Glowstone
in the hotbar and charges the anchor automatically. You keep control for the
rest of the sequence.

### Safe Anchor

After placing an anchor, the mod switches to Glowstone, charges the anchor,
places a Glowstone block in a supported space directly in front of you, and
switches back to the configured hotbar slot.

### Full Safe Anchor

Runs the Safe Anchor sequence, then switches to the configured hotbar slot and
activates the charged anchor automatically. It will not activate the anchor if
the protective Glowstone placement fails.

### Full Anchor

Switches to Glowstone, charges the anchor, and activates it automatically.

The shield placement checks one and two blocks ahead. If neither position is
replaceable with a solid block below it, it leaves the selected slot set to
your configured slot and shows a message instead of guessing.

## Enable and disable

The menu has a master `Anchor Macro: ON/OFF` control. Each of the four modes
also displays its own `ON/OFF` state. Only one mode is active at a time; click
the active mode again to turn the automation off.

## Menu

Press **O** to open the Anchor Macro menu. From there you can choose the mode,
toggle the mod on or off, and select the hotbar slot used after Safe Anchor.
The setting is saved in `config/anchor-macro.json`.

The mod is client-side and is intended for singleplayer or worlds where you
are allowed to use client automation. Server-side interaction rules, latency,
or anti-cheat systems can still prevent an automated action from succeeding.

## Install

1. Install Fabric Loader for Minecraft 1.20.6.
2. Install the matching Fabric API for 1.20.6.
3. Copy `build/libs/anchor-macro-1.0.0.jar` into your Minecraft `mods` folder.

## Build

From this directory:

```bash
./gradlew build
```