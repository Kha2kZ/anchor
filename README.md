⚓ Anchor Macro

<p align="center">
  <b>A modular Respawn Anchor automation mod for Minecraft.</b>
  <br>
  Fast • Configurable • Modular
</p><p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.x-62B47A?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft">
  <img src="https://img.shields.io/badge/Fabric-Mod-DBD0B4?style=for-the-badge" alt="Fabric">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21">
</p><p align="center">
  <a href="#-features">Features</a>
  •
  <a href="#-modules">Modules</a>
  •
  <a href="#-settings">Settings</a>
  •
  <a href="#-controls">Controls</a>
  •
  <a href="#-building">Building</a>
</p>---

✨ Features

- ⚡ Automatic Anchor Charging
- 💥 Automatic Detonation
- 🛡️ Defensive Block Placement
- 🔄 Multiple Automation Modes
- ⏱️ Configurable Action Delays
- 🎯 Optional Hotbar Selection
- ⌨️ Custom Module Keybinds
- ⚙️ In-game Configuration Menu
- 🟨 Totem Slot Highlighting

---

🧩 Modules

Module| 🔥 Charge| 🛡️ Defense| 💥 Detonate| Description
Auto Glowstone| ✅| —| —| Automatically charges a placed Respawn Anchor.
Auto Anchor| ✅| —| ✅| Automatically charges and detonates the anchor.
Auto Safe Anchor| ✅| ✅| —| Charges the anchor and places a defensive block in front of the player.
Full Safe Anchor| ✅| ✅| ✅| Combines charging, defense placement, and detonation.
Full Anchor| ✅| —| ✅| Full charge-and-detonate automation.

«💡 Each module can be toggled independently using its assigned keybind.»

---

⚙️ Settings

Anchor Macro provides configurable timing controls for automation.

Setting| Default| Description
⏱️ Charge Delay| "100 ms"| Delay applied to the anchor charging action.
🛡️ Defense Delay| "100 ms"| Delay applied before the defensive block action.

🎯 Hotbar Selection

The mod can optionally switch to a selected hotbar slot immediately before detonation.

This allows a specific item to be equipped for the final step of the sequence.

«🟨 Tip: A Totem of Undying can be placed in the selected slot when using this feature as an additional safety measure.»

---

🎮 Controls

⚙️ Configuration Menu

Press:

O

to open the Anchor Macro menu.

From here, you can configure the available module settings and timing options.

⌨️ Module Keybinds

Open:

Options
  └─ Controls
      └─ Key Binds

Scroll to the bottom of the keybind list to find the Anchor Macro bindings.

Each module can be assigned its own toggle key.

---

🏗️ Building

Clone the repository:

git clone https://github.com/Kha2kZ/anchor.git
cd anchor

Build with Gradle:

./gradlew build

On Windows:

gradlew.bat build

Build artifacts are generated in:

build/libs/

---

📁 Project Structure

anchor/
│
├── 📦 anchor-macro/
│   └── Main mod implementation
│
├── 🟨 totem-slot-highlighter/
│   └── Totem slot highlighting
│
├── 🖼️ attached_assets/
│   └── Project assets
│
├── 🤖 .github/
│   └── GitHub Actions
│
├── ⚙️ build.gradle
└── ⚙️ settings.gradle

---

🧪 Compatibility

Anchor Macro is built for the Fabric modding ecosystem.

Make sure your:

Minecraft
   ↓
Fabric Loader
   ↓
Fabric API
   ↓
Java

versions match the versions specified by the current project configuration.

---

⚠️ Disclaimer

Anchor Macro is an independent project and is not affiliated with Mojang Studios or Microsoft.

Use the mod responsibly and follow the rules of the server or environment where it is used.

---

<p align="center">
  <sub>⚓ Anchor Macro • Modular automation for Minecraft</sub>
</p>
