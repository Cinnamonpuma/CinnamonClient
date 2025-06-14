# CinnamonClient            <img src="https://github.com/user-attachments/assets/ab8e350a-ecb2-4548-865d-f7a0beb871bc" width="500"/>


**CinnamonClient** is a modular and customizable Minecraft 1.21.5 Fabric client mod focused on providing a lightweight and extensible framework for player-centric HUDs, UI utilities, shaders, and packet-level control.

It’s built from the ground up with **Kotlin**, **Mixin**, and **Fabric**, aiming to offer a dev-friendly experience while giving users fine-tuned control over their in-game interface.

---

## 🔧 Core Features

- 🎯 **Modular HUD System** – Toggle, move, and configure individual HUD elements in real time.
- 🎨 **Shader-Enhanced GUI** – Background shader support to elevate UI visuals.
- 🧰 **Custom UI Toolkit** – Reusable buttons, screen helpers, and widget utilities.
- 📦 **Ui Utils Rip Off** – Interact with Minecraft’s packet layer directly.
- ⚡ **Built for Performance** – Minimal dependencies and clean Kotlin architecture.

---

## 🧩 Modules Overview

All modules are fully toggleable and configurable through the in-game GUI.

### 📊 HUD Modules
| Module         | Description |
|----------------|-------------|
| `Keystrokes`   | Renders WASD/mouse buttons visually. Supports custom key mappings, colors, and layout. |
| `FPS Display`  | Shows your current FPS with customizable formatting, color, and position. |
| `Ping Display` | Renders ping to the server or localhost in SP. Custom position and color scaling. |

### 🛠️ UI Tools & Modules
| Module             | Description |
|--------------------|-------------|
| `HUD Edit Mode`    | Enter edit mode with `ESC` or bound key to drag & place modules. |
| `Packet Handler`    | Backing module to power screen interactions, button injection, styling. |
| `CinnamonScreen`   | Base class for creating styled screens with consistent theming and fonts. |

## 🎨 Customization

CinnamonClient puts **you** in control.

### 🧩 Drag-and-Drop HUD
- Use the **HUD Edit Mode** to reposition any module.
- Snapping and alignment helpers ensure clean layouts.
- Press `ESC` to exit layout mode.

### 🎛️ Per-Module Settings
- Each module offers individual settings in the GUI:
  - Font & size
  - Colors
  - Positioning
  

---
All coded my me - Cinnamonpuma
