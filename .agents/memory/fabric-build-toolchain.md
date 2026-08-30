---
name: Fabric build toolchain
description: Build constraints for the Minecraft 1.20.6 Fabric project.
---

Minecraft 1.20.6 Fabric builds require Java 21. Fabric Loom 1.8.12 requires Gradle 8.10 or newer, and Mojang libraries must be resolvable from `https://libraries.minecraft.net/`.

**Why:** The environment's default Gradle was newer than the initial Loom release, while the generated wrapper was too old for the selected Loom release; explicit toolchain and repository settings prevent both failures.

**How to apply:** Keep the mod wrapper on Gradle 8.10+ and retain Fabric, Mojang libraries, and Maven Central repositories when updating dependencies.