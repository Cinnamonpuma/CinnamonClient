<table>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/95fb0910-5221-4ad6-ba35-f272971d752f" width="100"/></td>
    <td style="vertical-align: middle; padding-left: 10px;"><h1>CinnamonClient</h1></td>
  </tr>
</table>

**CinnamonClient** is a modular, customizable Minecraft 1.21.5 Fabric client mod designed to provide a clean and extensible framework for HUDs, UI utilities, shaders, and low-level control.

Developed in **Kotlin** using **Mixin** and **Fabric**, CinnamonClient offers a lightweight yet powerful experience for both players and developers.

---

## Features

- Modular HUD with drag-and-drop editing
- Custom shader support for enhanced GUI visuals
- Built-in fullbright toggle for improved visibility
- Developer-friendly UI toolkit with reusable components
- Direct packet-layer interaction for advanced control
- Clean, performant architecture with minimal dependencies

---

## Modules

All modules are toggleable and configurable via the in-game GUI.

### HUD Elements

| Module         | Description |
|----------------|-------------|
| **Keystrokes** | Displays WASD and mouse buttons. Includes custom mappings, layout options, and color settings. |
| **FPS Display** | Shows current frames per second. Fully configurable. |
| **Ping Display** | Displays server or localhost ping. Adjustable style and position. |
| **Armor HUD** | Renders current armor and durability. Supports custom layout and styling. |

### Utility Modules

| Module             | Description |
|--------------------|-------------|
| **HUD Edit Mode** | Activate edit mode to reposition and align modules on-screen. Supports snapping and clean layouts. |
| **Fullbright**     | Toggles maximum brightness for improved visibility in low-light environments. |
| **Packet Handler** | Internal module to support injected buttons, screen styling, and network-layer features. |
| **CinnamonScreen** | A styled screen base class with consistent fonts and theming. Use it to build your own custom UIs. |

---

## Customization

### HUD Editing

Enter **HUD Edit Mode** by pressing `ESC` or a bound key to reposition modules with precision. Built-in snapping and alignment tools make layout clean and intuitive.

### Per-Module Settings

Each module supports configuration from within the GUI:

- Positioning
- Colors
- Fonts and sizes
- Module-specific settings

---

## Development

CinnamonClient is written entirely in **Kotlin** and uses **Fabric** with **Mixin** for low-level hooks and modular structure.

Designed to be a reliable foundation for creating and extending Minecraft UI experiences.

---

Fully Developed by **Cinnamonpuma**