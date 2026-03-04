# Fixed Resizable by Snoop — RuneLite Plugin

Personal build of the Fixed Resizable Hybrid plugin for Old School RuneScape. Skins the "Resizable - Classic Layout" to match fixed mode with a ~245px right-side panel.

## Features

- **Full-screen game viewport** minus the right-side panel
- **Right-side interface** (inventory, minimap, tabs, prayer, spellbook, etc.) outside the viewport
- **Panel width** fixed at ~245px
- **Native widgets** fully interactive (no overlay redrawing)
- **Enable/disable** restores default layout
- **Window resizing** adjusts viewport and panel correctly
- **Watermark**: "All Bruised Up (ABU) — by Snoop" at bottom of panel (subtle, small font, low opacity)

## Quick Start

1. Run `run.bat`
2. Set layout to **Resizable - Classic Layout** (Settings → Display → Game Client Layout)
3. Enable **Fixed Resizable by Snoop** in the plugin list
4. Confirm the game area fills the left side and the side panel stays fixed at 245px on the right

## Configuration

All original Fixed Resizable Hybrid options are preserved:

- **Wide Chatbox** — Widens chatbox, viewport centering, button options
- **Minimap Settings** — Orb positioning (Fixed Mode / More Clearance)
- **Inv/Minimap Background** — Gap borders, background mode (solid/tiled stone/custom image), tint
- **Window Resizing** — Aspect ratio resize (16:9, 21:9, etc.)

## Build

```bash
.\gradlew.bat build
```

## Credits

Based on [Lapask/fixed-resizable-hybrid](https://github.com/Lapask/fixed-resizable-hybrid). Licensed under BSD-2-Clause.
