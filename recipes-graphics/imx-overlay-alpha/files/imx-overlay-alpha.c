/*
 * imx-overlay-alpha: Control overlay framebuffer alpha for boot transitions
 *
 * Reads/writes /sys/class/graphics/fb1/overlay_alpha to control the
 * IPU Display Processor global alpha for seamless crossfade between
 * boot animation (fb0) and Flutter UI (fb1).
 *
 * Usage:
 *   imx-overlay-alpha <value>           Set alpha (0-255)
 *   imx-overlay-alpha fade <from> <to> <ms>  Fade between values
 *   imx-overlay-alpha wait-fade <from> <to> <ms>
 *       Wait for fb1 to have non-zero content, then fade
 */

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <time.h>
#include <sys/mman.h>
#include <sys/ioctl.h>
#include <linux/fb.h>

#define ALPHA_PATH "/sys/class/graphics/fb1/overlay_alpha"

static int set_alpha(int fd, int alpha)
{
	char buf[16];
	int len = snprintf(buf, sizeof(buf), "%d\n", alpha);
	if (pwrite(fd, buf, len, 0) < 0) {
		perror("write alpha");
		return -1;
	}
	return 0;
}

static void sleep_us(long us)
{
	struct timespec ts = { .tv_sec = us / 1000000, .tv_nsec = (us % 1000000) * 1000 };
	while (nanosleep(&ts, &ts) != 0)
		;
}

static int do_fade(int fd, int from, int to, int duration_ms)
{
	int steps = abs(to - from);
	if (steps == 0) return set_alpha(fd, to);
	if (steps > 60) steps = 60;

	long step_us = (long)duration_ms * 1000 / steps;
	struct timespec start, now;
	clock_gettime(CLOCK_MONOTONIC, &start);

	for (int i = 0; i <= steps; i++) {
		int alpha = from + (to - from) * i / steps;
		set_alpha(fd, alpha);

		if (i < steps) {
			clock_gettime(CLOCK_MONOTONIC, &now);
			long elapsed_us = (now.tv_sec - start.tv_sec) * 1000000L +
					  (now.tv_nsec - start.tv_nsec) / 1000;
			long target_us = (long)(i + 1) * step_us;
			if (target_us > elapsed_us)
				sleep_us(target_us - elapsed_us);
		}
	}

	return 0;
}

static int wait_for_content(const char *fb_path, int timeout_ms)
{
	int fb_fd = open(fb_path, O_RDONLY);
	if (fb_fd < 0) {
		perror("open fb1");
		return -1;
	}

	struct fb_var_screeninfo vinfo;
	struct fb_fix_screeninfo finfo;
	if (ioctl(fb_fd, FBIOGET_VSCREENINFO, &vinfo) < 0 ||
	    ioctl(fb_fd, FBIOGET_FSCREENINFO, &finfo) < 0) {
		perror("ioctl fb1");
		close(fb_fd);
		return -1;
	}

	size_t fb_size = finfo.smem_len;
	void *fb = mmap(NULL, fb_size, PROT_READ, MAP_SHARED, fb_fd, 0);
	if (fb == MAP_FAILED) {
		perror("mmap fb1");
		close(fb_fd);
		return -1;
	}

	int pixels = vinfo.xres * vinfo.yres;
	int bpp = vinfo.bits_per_pixel / 8;
	int elapsed_ms = 0;

	fprintf(stderr, "waiting for content on %s (%dx%d %dbpp)...\n",
		fb_path, vinfo.xres, vinfo.yres, vinfo.bits_per_pixel);

	while (elapsed_ms < timeout_ms) {
		/* Sample a few pixels across the framebuffer */
		const uint16_t *px = (const uint16_t *)fb;
		if (bpp == 2) {
			if (px[0] != 0 || px[pixels / 4] != 0 ||
			    px[pixels / 2] != 0 || px[pixels * 3 / 4] != 0 ||
			    px[pixels - 1] != 0) {
				fprintf(stderr, "content detected after %d ms\n", elapsed_ms);
				munmap(fb, fb_size);
				close(fb_fd);
				return 0;
			}
		} else {
			const uint32_t *px32 = (const uint32_t *)fb;
			if (px32[0] != 0 || px32[pixels / 2] != 0 ||
			    px32[pixels - 1] != 0) {
				fprintf(stderr, "content detected after %d ms\n", elapsed_ms);
				munmap(fb, fb_size);
				close(fb_fd);
				return 0;
			}
		}

		sleep_us(50000); /* 50ms poll interval */
		elapsed_ms += 50;
	}

	fprintf(stderr, "timeout after %d ms, proceeding anyway\n", timeout_ms);
	munmap(fb, fb_size);
	close(fb_fd);
	return 1;
}

int main(int argc, char *argv[])
{
	if (argc < 2) {
		fprintf(stderr,
			"usage: imx-overlay-alpha <value>\n"
			"       imx-overlay-alpha fade <from> <to> <ms>\n"
			"       imx-overlay-alpha wait-fade <from> <to> <ms> [timeout_ms]\n");
		return 1;
	}

	int fd = open(ALPHA_PATH, O_WRONLY);
	if (fd < 0) {
		perror("open " ALPHA_PATH);
		return 1;
	}

	if (strcmp(argv[1], "fade") == 0) {
		if (argc < 5) {
			fprintf(stderr, "usage: imx-overlay-alpha fade <from> <to> <ms>\n");
			close(fd);
			return 1;
		}
		int from = atoi(argv[2]);
		int to = atoi(argv[3]);
		int ms = atoi(argv[4]);
		int ret = do_fade(fd, from, to, ms);
		close(fd);
		return ret ? 1 : 0;

	} else if (strcmp(argv[1], "wait-fade") == 0) {
		if (argc < 5) {
			fprintf(stderr, "usage: imx-overlay-alpha wait-fade <from> <to> <ms> [timeout_ms]\n");
			close(fd);
			return 1;
		}
		int from = atoi(argv[2]);
		int to = atoi(argv[3]);
		int ms = atoi(argv[4]);
		int timeout = (argc > 5) ? atoi(argv[5]) : 15000;

		wait_for_content("/dev/fb1", timeout);

		int ret = do_fade(fd, from, to, ms);
		close(fd);
		return ret ? 1 : 0;

	} else {
		int alpha = atoi(argv[1]);
		if (alpha < 0 || alpha > 255) {
			fprintf(stderr, "alpha must be 0-255\n");
			close(fd);
			return 1;
		}
		int ret = set_alpha(fd, alpha);
		close(fd);
		return ret ? 1 : 0;
	}
}
