# Hybrid Inventory - RuneLite Plugin

A RuneLite plugin that displays a fixed-classic style inventory panel on the right side of the screen in resizable mode.

## Features

- **Fixed-classic layout**: 28 slots in 4x7 grid, ~241px width (no stretching/scaling)
- **Right-edge anchored**: Panel repositions dynamically on window resize
- **Fully functional**: All 28 slots clickable with full item menus (Use, Drop, Eat, Wear, etc.)
- **Resizable only**: Auto-disables in fixed mode
- **Clean overlay**: Original inventory widget hidden when enabled
- **Watermark**: "All Bruised Up (ABU)" at bottom of panel

## Quick Start

**Double-click `install.bat`** — it builds and installs the plugin automatically.

Then:
1. Restart RuneLite (if it was open)
2. Enable **Hybrid Inventory** in the plugin list
3. Use **Resizable** or **Fullscreen** mode (not Fixed)

## Configuration

| Option | Description |
|--------|--------------|
| Enable Hybrid Inventory | Toggle the plugin on/off (default: on) |

## Troubleshooting

- **Panel not showing**: Ensure you're in Resizable or Fullscreen mode (not Fixed)
- **Build fails**: Verify Java 11+ is installed and `JAVA_HOME` is set
- **Clicks not working**: Try disabling other inventory-related plugins
