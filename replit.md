# Anchor Macro

A client-side Fabric mod for Minecraft 1.20.6 that automates respawn-anchor charging, defense, and detonation sequences.

## Run & Operate

- `cd anchor-macro && ./gradlew build` — build the Anchor Macro mod
- The compiled mod JAR is written to `anchor-macro/build/libs/`
- Java 21 and Gradle 8.10.2+ are required; the checked-in Gradle wrapper provides the supported Gradle version
- No long-running preview workflow is configured because this repository contains a Minecraft mod, not a web server

## Stack

- Java 21
- Gradle with Fabric Loom 1.8.12
- Minecraft 1.20.6
- Fabric Loader 0.15.11 and Fabric API 0.100.8+1.20.6

## Where things live

- `anchor-macro/` — the focused mod project
- `anchor-macro/src/main/java/` — mod source code
- `anchor-macro/src/main/resources/` — Fabric metadata and assets
- `totem-slot-highlighter/` — separate imported mod, intentionally not modified by this setup

## Architecture decisions

- The existing standalone Gradle/Fabric structure is preserved.
- Builds use the checked-in Gradle wrapper rather than relying on a global Gradle version.
- The Replit setup targets `anchor-macro` only; the second imported mod remains independent.

## Product

Anchor Macro provides configurable Auto Glowstone, Safe Anchor, Full Safe Anchor, and Full Anchor modes. It is intended for client-side use in permitted singleplayer or multiplayer environments.

## User preferences

Focus setup and builds on `anchor-macro`; leave `totem-slot-highlighter` unchanged unless requested.

## Gotchas

- Fabric mods built for Minecraft 1.20.6 require Java 21.
- Server rules, latency, and anti-cheat systems can prevent automated actions from succeeding.

## Pointers

- See `anchor-macro/README.md` for installation, controls, modes, and configuration details.
