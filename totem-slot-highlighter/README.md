# Totem Slot Highlighter

A client-side Fabric mod for Minecraft 1.20.6 that makes Totems of Undying
stand out in the player's inventory and hotbar.

## Behavior

Whenever an inventory screen is open, slots in the player's inventory that
contain a Totem of Undying receive:

- a translucent red background
- a bright red border

The totem item, stack count, and tooltip remain visible. Slots in containers
such as chests are not recolored.

## Install

1. Install Fabric Loader for Minecraft 1.20.6.
2. Install the matching Fabric API for 1.20.6.
3. Copy `build/libs/totem-slot-highlighter-1.0.0.jar` into the Minecraft
   `mods` folder.

This is a client-only mod and does not need to be installed on a server.

## Build

From this directory:

```bash
gradle build
```

The compiled mod is written to `build/libs/`.