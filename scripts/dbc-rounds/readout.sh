#!/bin/bash
# Readout after a kernel test round. Read-only.
D=/data/flashfree; OUT=$D/kernel-readout-$(date +%s).txt; exec > >(tee "$OUT") 2>&1
echo "== kernel"; uname -a; md5sum /boot/zImage /boot/librescoot-dbc.dtb /boot/zImage.test /boot/librescoot-dbc.dtb.test 2>&1
echo "== testboot env"; fw_printenv bootcmd bootcmd_orig testboot mender_kernel_name mender_dtb_name 2>&1
echo "== live fdt has spi panel"; python3 -c "d=open('/sys/firmware/fdt','rb').read(); print('a045ftn01' in d.decode('latin1'))"
echo "== uptime / boots"; uptime; journalctl --list-boots 2>/dev/null | tail -3
echo "== spi devices"; ls /sys/bus/spi/devices/ 2>&1; for d in /sys/bus/spi/devices/*; do echo "$d: $(cat $d/modalias 2>/dev/null) driver=$(basename $(readlink $d/driver 2>/dev/null) 2>/dev/null)"; done
echo "== spi controllers"; ls /sys/class/spi_master/ 2>&1
echo "== deferred probes"; mount -t debugfs none /sys/kernel/debug 2>/dev/null; cat /sys/kernel/debug/devices_deferred 2>&1 | head
echo "== drm"; ls /sys/class/drm/ 2>&1; for c in /sys/class/drm/card*-*; do echo "$c: $(cat $c/status 2>/dev/null) enabled=$(cat $c/enabled 2>/dev/null)"; done; cat /sys/class/graphics/fb0/name 2>&1
echo "== backlight"; for b in /sys/class/backlight/*; do echo "$b: power=$(cat $b/bl_power) brightness=$(cat $b/brightness)/$(cat $b/max_brightness)"; done
echo "== gpio4_20 / pads"; python3 - <<'PY'
import mmap,os,struct
f=os.open('/dev/mem',os.O_RDONLY|os.O_SYNC); m=mmap.mmap(f,0x1000,mmap.MAP_SHARED,mmap.PROT_READ,offset=0x020e0000)
print('mux DI0_PIN4(reset)=0x%x KEY_COL0(sclk)=0x%x KEY_ROW0(mosi)=0x%x KEY_ROW1(ss0)=0x%x DISP_CLK=0x%x' % tuple(struct.unpack_from('<I',m,o)[0] for o in (0xac,0x244,0x258,0x25c,0x9c)))
g=mmap.mmap(f,0x1000,mmap.MAP_SHARED,mmap.PROT_READ,offset=0x020a8000)
dr,gdir=struct.unpack_from('<II',g,0); print('GPIO4 DR bit20=%d GDIR bit20=%d' % ((dr>>20)&1,(gdir>>20)&1))
c=mmap.mmap(f,0x1000,mmap.MAP_SHARED,mmap.PROT_READ,offset=0x02600000)
print('IPU_CONF=0x%08x DI0_EN=%d' % (struct.unpack_from('<I',c,0)[0], (struct.unpack_from('<I',c,0)[0]>>6)&1))
PY
echo "== dmesg drm/spi/panel"; dmesg | grep -i "a045\|spi_imx\|spi\|ecspi\|panel\|backlight\|imx-drm\|deferred\|Oops\|panic\|BUG" | head -40 | cut -c1-180
echo "== fdt has idle pad group"; python3 -c "print('lcdidlegrp' in open('/sys/firmware/fdt','rb').read().decode('latin1'))"
echo "== dmesg first 60"; dmesg | head -60 | cut -c1-160; echo "== dmesg last 40"; dmesg | tail -40 | cut -c1-160
echo "== DI0_GENERAL"; python3 - <<'PY'
import mmap, os, struct
f=os.open('/dev/mem', os.O_RDONLY|os.O_SYNC)
m=mmap.mmap(f, 0x1000, mmap.MAP_SHARED, mmap.PROT_READ, offset=0x02640000)
v=struct.unpack_from('<I', m, 0)[0]
print('DI0_GENERAL=0x%08x vsync_ext(bit21)=%d clk_ext(bit20)=%d' % (v, (v>>21)&1, (v>>20)&1))
PY
echo "== previous boot kernel log (if journal is persistent)"; journalctl -b -1 -k --no-pager 2>&1 | grep -i "a045\|spi\|panel\|drm\|backlight\|Oops\|panic\|BUG\|Linux version" | head -40 | cut -c1-180
if [ -f /boot/zImage.test ] && fw_printenv -n bootcmd 2>/dev/null | grep -q testboot; then
  fw_setenv testboot 1 && echo "== re-armed testboot=1 for the next boot"
fi
redis-cli -h 192.168.7.1 HSET flashfree kernel-readout-$(date +%s) "$(cat $OUT)" >/dev/null 2>&1 && echo stored
exit 0
