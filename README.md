# ⚓ Anchor Macro

<p align="center">
  <b>A packet-aware, modular Respawn Anchor automation mod for Minecraft Fabric.</b>
  <br>
  Fast • Resilient • Camera-Independent
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.20.6-62B47A?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft 1.20.6">
  <img src="https://img.shields.io/badge/Fabric-Mod-DBD0B4?style=for-the-badge" alt="Fabric">
  <img src="https://img.shields.io/badge/Environment-Client--Side-orange?style=for-the-badge" alt="Client-Side">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21">
</p>

---

## ✨ Overview

**Anchor Macro** is a high-performance client-side Fabric mod designed to streamline Respawn Anchor placement, charging, defense placement, and detonation in Minecraft PvP. It executes actions with packet-aware precision, ensuring minimum latency without locking controls or creating dead zones after explosions.

---

## ✨ Key Features

- 🧩 **4 Specialized Automation Modes:** Adaptable to any anchor PvP style.
- ⚡ **Camera-Independent Target Interactivity:** Calculates exact target faces without requiring crosshair placement.
- 🔄 **Event-Driven Task Engine:** Tracks server ACK confirmations and client block state updates to avoid stalled macros.
- 🛡️ **Smart Anti-Spam & Reach Protection:** Automatically purges out-of-reach tasks when knocked back by explosions.
- ⏱️ **Configurable Delays & Slot Routing:** Fine-tune micro-delays and fallback hotbar slots.
- ⌨️ **Keybind Chords:** Assign dedicated key combinations with modifier support (e.g., `O + F`).

---

## 🧩 Automation Modes

The mod features **4 distinct operational modes**. Only one mode can be active at a time.

| Mode | Charge | Defense Block | Detonate | Primary Use Case |
| :--- | :---: | :---: | :---: | :--- |
| ⚡ **Auto Glowstone** | ✅ | ❌ | ❌ | Automatically charges placed anchors; hands control back immediately. |
| 🛡️ **Safe Anchor** | ✅ | ✅ | ❌ | Charges anchor & places defense block; requires manual detonation. |
| 🛡️💥 **Full Safe Anchor** | ✅ | ✅ | ✅ | Complete automated sequence: Charge ➔ Defense Block ➔ Detonate. |
| ⚡💥 **Full Anchor** | ✅ | ❌ | ✅ | Maximum speed sequence: Charge ➔ Detonate (bypasses defense block). |

### Detailed Mode Mechanics

- **Auto Glowstone:** Detects a placed Respawn Anchor, switches to Glowstone in your hotbar, charges the anchor, and relinquishes control.
- **Safe Anchor:** Charges the anchor, places a non-Glowstone block between you and the anchor to absorb blast damage, then switches to your configured safe slot.
- **Full Safe Anchor:** Runs the complete **Safe Anchor** sequence and automatically detonates the anchor. *(If defense placement is obstructed, it automatically skips the defense step and proceeds directly to detonation).*
- **Full Anchor:** Charges and detonates the anchor instantly for maximum burst damage.

---

## ⚙️ Configuration & Controls

Press **`O`** in-game to open the **Anchor Macro Menu**.

### ⏱️ Settings (`Generic` Tab)

| Setting | Default | Description |
| :--- | :---: | :--- |
| **Master Switch** | `ON` | Global toggle to enable or disable all macro functionality. |
| **Safe Hotbar Slot** | `Slot 1` | The hotbar slot selected after placing defense blocks or completing sequences. |
| **Charge Delay** | `100 ms` | Wait time (in milliseconds) after placing an anchor before charging. |
| **Defense Delay** | `100 ms` | Wait time (in milliseconds) between charging, placing defense blocks, and detonation. |

> 🟨 **Tip:** Place a **Totem of Undying** in your configured **Safe Hotbar Slot** to automatically re-equip protection immediately after detonation.

### ⌨️ Keybinds (`Keybinds` Tab)

Keybindings can be configured in **Options → Controls → Key Binds → Anchor Macro** or directly within the mod menu:

- **Primary Hotkeys:** Set custom keys for each mode.
- **Chord Modifiers:** Set a modifier key in the mod menu to create chord bindings (e.g., primary key `F` + modifier `O` triggers the macro via `O + F`).

---

## 🛠️ Technical Details

- **Packet Synchronization:** Interacts via precise block packets combined with silent pitch/yaw packets and `HandSwingC2SPacket` to ensure compatibility with server validation rules.
- **Knockback Resilience:** If an explosion knocks you outside the interaction range ($\le 4.5\text{m}$), tasks automatically clear to avoid blocking subsequent anchor placements in the area.
- **Instant Overwrite:** Placing a new anchor at a previously detonated position instantly overwrites any existing task state at that coordinate.

---

## 📥 Installation

1. Install **[Fabric Loader](https://fabricmc.net/)** for **Minecraft 1.20.6**.
2. Install **[Fabric API](https://modrinth.com/mod/fabric-api)** for 1.20.6 and place it in `.minecraft/mods`.
3. Download the `jar` file and drop it into `.minecraft/mods`.

---

## 🏗️ Building from Source

Clone the repository:

```bash
git clone https://github.com/Kha2kZ/anchor.git
cd anchor
```

Build with Gradle:

```bash
./gradlew build
```

On Windows (PowerShell):

```powershell
.\gradlew.bat build
```

Build output file location:

```text
build/libs/anchor-macro-1.0.0.jar
```

---

## ⚠️ Disclaimer

Anchor Macro is a **client-side mod** intended for singleplayer or multiplayer servers where client automation is explicitly permitted. Server-side latency, packet rules, and anti-cheat software may affect macro performance.

---

<p align="center">
  <sub>⚓ Anchor Macro • Precision Automation for Minecraft Fabric 1.20.6</sub>
</p>
