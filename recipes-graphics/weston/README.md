# Weston Configuration for LibreScoot

This directory contains modifications to the Weston Wayland compositor configuration to hide the desktop environment before ScootUI starts.

## Changes

1. **Custom Weston Configuration (`weston.ini`)**:
   - Uses `fullscreen-shell.so` instead of the default desktop shell to eliminate desktop UI components
   - Configures a custom splash screen background
   - Disables the panel/topbar
   - Sets idle-time to 0 to prevent screen blanking

2. **Custom Background Image**:
   - Adds a LibreScoot-branded splash image that displays while ScootUI is loading

## Implementation Details

The changes address issue #3 in the meta-librescoot repository, which requested hiding the Wayland desktop before ScootUI starts. Rather than completely removing Wayland, this solution configures it to be minimal and visually consistent with the LibreScoot brand.

### Files

- `weston.ini`: Custom configuration file for Weston
- `weston_%.bbappend`: Recipe append file that installs the custom configuration and splash image

### Related Components

The ScootUI systemd service starts after Weston is ready (via the `Requires=weston.socket` dependency), ensuring a smooth transition from the splash screen to the application UI.
