<table>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/95fb0910-5221-4ad6-ba35-f272971d752f" width="100"/></td>
    <td style="vertical-align: middle; padding-left: 10px;"><h1>CinnamonClient</h1></td>
  </tr>
</table>

**CinnamonClient** is a modular and performance-focused Minecraft 1.21.5 client mod for Fabric. It offers a clean, customizable HUD system, a modern UI framework, and advanced developer utilities—built entirely in Kotlin.

Designed for users, CinnamonClient emphasizes clarity, extensibility, and real-time control.

---

## Overview

- Lightweight and modular architecture
- Fully customizable HUD with live editing
- UI toolkit for building polished screens
- Packet-level integration and controls
- Clean Kotlin codebase with minimal dependencies

---

## Core Modules

All modules are toggleable and configurable through the in-game GUI.

### HUD Elements

| Name              | Description |
|-------------------|-------------|
| **Keystrokes**    | Visualizes input (WASD, mouse buttons). Supports layout customization, remapping, and color control. |
| **FPS Display**   | Displays current frames per second. Format, color, and position are configurable. |
| **Ping Display**  | Shows your latency to the server or local host. Adjustable style and scaling. |
| **Armor HUD**     | Renders armor icons and durability. Layout and positioning are fully customizable. |
| **Coordinates HUD** | Displays player's X, Y, Z coordinates. Supports axis labeling, formatting, and screen placement. |

### Utility Modules

| Name                | Description |
|---------------------|-------------|
| **HUD Edit Mode**   | Allows drag-and-drop repositioning of all HUD elements. Includes snapping and alignment helpers. |
| **Fullbright**      | Enables full brightness regardless of lighting conditions. |
| **Packet Handler**  | Provides packet-layer utilities and powers internal UI interactions. |
---

## Customization

### Live HUD Editing

Activate **HUD Edit Mode** via the main menu screen to move and align modules directly on-screen. Includes edge snapping and alignment guides for precision. Also includes scaling.

### Per-Module Configuration

Each module includes dedicated settings via the GUI:

- Scaling
- Color customization
- Screen positioning

---

## Development

CinnamonClient is built using:

- **Kotlin** — clean syntax and expressive language features
- **Fabric** — lightweight and widely supported modding framework
- **Mixin** — powerful method injection for low-level control

Its architecture is designed for easy module creation and clean UI logic separation.

---

## Author

All code is written by **Cinnamonpuma**. For inquiries, contributions, or custom builds, contact via GitHub or Discord.