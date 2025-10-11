# Plymouth Boot Animation - Testing Notes

**Branch:** `plymouth-boot-animation` (in meta-librescoot)
**Date:** 2025-09-27

## Summary of Changes

### 1. Framebuffer Console Support (Committed to scarthgap)
- **File:** `recipes-kernel/linux/linux-fslc/config-video.cfg`
- **Commit:** 6c9ea99 "feat(boot): restore boot logo with framebuffer console"
- **Changes:**
  - Enabled `CONFIG_FB=y`
  - Enabled `CONFIG_DRM_FBDEV_EMULATION=y`
  - Enabled `CONFIG_FRAMEBUFFER_CONSOLE=y`
  - Disabled `CONFIG_FRAMEBUFFER_CONSOLE_DEFERRED_TAKEOVER` for immediate logo display

**Why:** The mainline kernel + etnaviv switch required explicit framebuffer console configuration. This restores the kernel boot logo that was working with the old NXP BSP.

### 2. systemd Boot Args (Committed to plymouth-boot-animation)
- **File:** `recipes-bsp/u-boot/u-boot-imx/dbc/0004-add-console-and-plymouth-boot-args.patch`
- **Commit:** a971555 "feat(boot): hide systemd status messages for clean Plymouth display"
- **Changes:** Added `systemd.show_status=0` to boot parameters

**Current boot args:**
```
console=ttymxc0,115200 systemd.show_status=0 plymouth.ignore-serial-consoles splash quiet loglevel=3 vt.global_cursor_default=0
```

## Investigation Findings

### Why Plymouth Didn't Work Before

**Old NXP BSP (linux-imx with mxcfb):**
- Used proprietary mxcfb framebuffer driver
- Not a proper DRM/KMS driver
- Plymouth's DRM renderer couldn't initialize
- No `/dev/dri/cardN` devices with proper KMS support

**New Mainline BSP (linux-fslc with etnaviv + imx-drm):**
- Proper DRM/KMS support
- `/dev/dri/card0` = Vivante GC2000 GPU (etnaviv)
- `/dev/dri/card1` = IPU display controller (imx-drm)
- Plymouth's DRM renderer should work now

### DRM Driver Configuration (Built-in)

All critical DRM drivers are compiled into the kernel (`=y`), not as modules:
- `CONFIG_DRM=y`
- `CONFIG_DRM_IMX=y`
- `CONFIG_DRM_IMX_IPUV3=y`
- `CONFIG_IMX_IPUV3_CORE=y`
- `CONFIG_DRM_ETNAVIV=y`
- `CONFIG_FB=y`
- `CONFIG_FRAMEBUFFER_CONSOLE=y`

**This means:** No kernel modules need to be loaded in initramfs for Plymouth to work.

### Initramfs Decision

**Decision: Test without initramfs first**

**Reasoning:**
1. Plymouth recipe already has `initrd` PACKAGECONFIG enabled
2. DRM drivers are built-in (no module loading needed)
3. Plymouth service will start after root mount (late boot)
4. Testing this first tells us if DRM renderer works at all
5. If successful but too brief, then we can add initramfs later

**Initramfs would require:**
- `plymouth-initrd` package (requires dracut)
- Creating custom initramfs image recipe
- Bundling into kernel with `INITRAMFS_IMAGE_BUNDLE = "1"`
- More complex, more can go wrong
- Better to validate basic functionality first

### Existing Plymouth Configuration

**Already in place:**
- ✅ `SPLASH = "plymouth"` in image recipe
- ✅ `IMAGE_FEATURES += "splash"` in image recipe
- ✅ `IMAGE_INSTALL:append = " plymouth plymouth-animation"`
- ✅ `PACKAGECONFIG:append = " drm"` in plymouth bbappend
- ✅ Custom librescoot theme with 14 frame animation
- ✅ Boot args include `splash quiet plymouth.ignore-serial-consoles`
- ✅ scootui.service waits for `plymouth-quit-wait.service`
- ✅ plymouthd.conf sets `Theme=librescoot` and `ShowDelay=0`

## Testing Plan for Device Access

### Test 1: Check if Plymouth Starts At All

```bash
# After boot, check if Plymouth services ran
journalctl -u plymouth-start.service
journalctl -u plymouthd.service
journalctl -u plymouth-quit.service
journalctl -u plymouth-quit-wait.service

# Check if Plymouth found DRM devices
journalctl | grep -i plymouth

# Check DRM devices
ls -la /dev/dri/
# Should see card0 (etnaviv) and card1 (IPU)
```

**Expected outcomes:**

**Success:**
- Plymouth services show as started
- Logs show "Using DRM renderer"
- Logs show device like "/dev/dri/card1"
- Animation may be brief but visible

**Failure:**
- Plymouth fails to start
- Logs show "No suitable renderer found"
- Or "Failed to open DRM device"

### Test 2: Visual Observation

Watch the boot sequence carefully:
1. U-boot (black screen with text)
2. **Kernel logo** (should appear - LibreScoot logo)
3. **Plymouth animation?** (may be very brief, 1-2 seconds)
4. Flutter UI

**Take video with phone if possible** to review frame-by-frame.

### Test 3: Check Boot Timing

```bash
systemd-analyze
systemd-analyze blame | head -20

# Specifically check Plymouth timing
systemd-analyze critical-chain plymouth-quit-wait.service
systemd-analyze critical-chain scootui.service
```

This tells us how much time Plymouth has to display before Flutter starts.

### Test 4: Manual Plymouth Test

If Plymouth isn't visible during boot, try manual test:

```bash
# Stop scootui
systemctl stop scootui.service

# Start Plymouth manually
/usr/sbin/plymouthd --debug --tty=/dev/tty1
/usr/bin/plymouth show-splash

# Wait a few seconds to see animation
sleep 10

# Stop Plymouth
/usr/bin/plymouth quit
```

This isolates whether Plymouth+DRM works independently of boot timing.

## Interpretation of Results

### Scenario A: Plymouth Works but Too Brief

**Indicators:**
- Logs show Plymouth started successfully
- DRM renderer initialized
- But animation only visible for 1-2 seconds
- Quickly replaced by Flutter

**Next Steps:**
1. Consider adding initramfs for earlier Plymouth start
2. Or delay Flutter startup slightly with `RestartSec=3` in scootui.service
3. Or accept brief animation (depends on user experience goals)

### Scenario B: Plymouth Doesn't Start at All

**Indicators:**
- Plymouth services failed
- Logs show renderer errors
- No DRM device found

**Debug Steps:**
1. Check which DRM device Plymouth is trying to use
2. Verify `/dev/dri/card1` permissions
3. Check for conflicting framebuffer usage
4. Try `FLUTTER_DRM_DEVICE="/dev/dri/card0"` temporarily (to free up card1)
5. Check plymouth theme file permissions

**Possible Issues:**
- Plymouth trying to use card0 instead of card1
- Permission issues with DRM devices
- Theme files not installed correctly
- systemd service ordering issues

### Scenario C: Plymouth Works Perfectly

**Indicators:**
- Smooth transition: kernel logo → Plymouth animation → Flutter
- No visible text/flicker
- Clean visual experience

**Next Steps:**
1. Test multiple reboots to confirm reliability
2. Document the solution
3. Merge to scarthgap
4. Close the Plymouth ticket

### Scenario D: Display Issues (Flicker, Corruption, Wrong Resolution)

**Indicators:**
- Plymouth starts but display looks wrong
- Screen corruption
- Wrong aspect ratio
- Flickering

**Debug Steps:**
1. Check display mode in Plymouth vs kernel
2. May need to add Plymouth device configuration
3. Check if IPU (card1) is being configured correctly
4. Test with different Plymouth themes (simpler ones)

## If Initramfs Becomes Necessary

If Plymouth works but is too brief, here's the approach:

### Option 1: Minimal Initramfs (Recommended)

Create `recipes-core/images/initramfs-plymouth-image.bb`:

```bitbake
SUMMARY = "Minimal initramfs for Plymouth boot splash"
DESCRIPTION = "Provides Plymouth early boot animation"

PACKAGE_INSTALL = " \
    initramfs-framework-base \
    plymouth \
    plymouth-animation \
    plymouth-set-default-theme \
    ${VIRTUAL-RUNTIME_base-utils} \
"

# Note: DRM drivers are built-in, no modules needed

IMAGE_FSTYPES = "${INITRAMFS_FSTYPES}"
IMAGE_ROOTFS_SIZE = "8192"
IMAGE_ROOTFS_EXTRA_SPACE = "0"

inherit core-image
```

Then in `librescoot-dbc-image.bb`:
```bitbake
INITRAMFS_IMAGE = "initramfs-plymouth-image"
INITRAMFS_IMAGE_BUNDLE = "1"
```

### Option 2: Use Dracut (More Complex)

- Requires `plymouth-initrd` package
- Requires dracut in build
- Automatically generates initramfs with Plymouth
- Heavier weight solution

## Hardware Context

**i.MX6 DualLite Display Architecture:**
- **Vivante GC2000 GPU** (`/dev/dri/card0`) - 3D rendering via etnaviv
- **IPU (Image Processing Unit)** (`/dev/dri/card1`) - Display scanout via imx-drm
- **Display:** LVDS connected to IPU
- **Flutter:** Uses `/dev/dri/card1` directly (DRM/GBM backend)

**Important:** Plymouth and Flutter both need DRM master access to card1. The handoff happens when Plymouth quits and Flutter starts.

## Files Changed

```
meta-librescoot/
├── recipes-kernel/linux/linux-fslc/config-video.cfg [modified, committed to scarthgap]
└── recipes-bsp/u-boot/u-boot-imx/dbc/
    └── 0004-add-console-and-plymouth-boot-args.patch [modified, on plymouth-boot-animation branch]
```

## Commands to Build and Test

```bash
# In librescoot repo
cd /Users/teal/src/librescoot/librescoot

# Clean to ensure u-boot is rebuilt with new boot args
./build.sh dbc --clean

# Full build
./build.sh dbc

# Flash to device and test
```

## Questions to Answer Through Testing

1. **Does Plymouth's DRM renderer initialize successfully with etnaviv/imx-drm?**
2. **Which DRM device does Plymouth choose (card0 or card1)?**
3. **How long is Plymouth visible before Flutter takes over?**
4. **Is the handoff from Plymouth to Flutter smooth or flickery?**
5. **Does the kernel logo → Plymouth transition work cleanly?**
6. **Are there any systemd status messages visible despite systemd.show_status=0?**

## References

- Plymouth recipe: `yocto/sources/meta-openembedded/meta-oe/recipes-core/plymouth/plymouth_24.004.60.bb`
- initramfs base: `yocto/sources/poky/meta/recipes-core/images/core-image-minimal-initramfs.bb`
- Framebuffer console commit: 6c9ea99 (in scarthgap)
- Boot args commit: a971555 (in plymouth-boot-animation branch)
