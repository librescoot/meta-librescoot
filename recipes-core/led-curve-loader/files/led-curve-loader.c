/* Loads fade and cue curve data into the imx-pwm-led driver so that
 * PLAY_FADE / PLAY_CUE are not a no-op when vehicle-service is absent
 * (e.g. on the installer's bootstrap image). Mirrors the loading logic
 * in vehicle-service's internal/hardware/leds.go.
 */

#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <unistd.h>

#define PWM_LED_CONFIGURE  0x00007540
#define PWM_LED_OPEN_FADE  0x00007541
#define PWM_LED_OPEN_CUE   0x00007542
#define PWM_LED_SET_ACTIVE 0x00007549
#define PWM_LED_SET_ADAPT  0x0000754C

#define PWM_PERIOD    12000
#define PWM_PRESCALER 0
#define PWM_INVERT    0
#define PWM_REPEAT    3

#define PWM_CFG_BIT_PRESCALER 16
#define PWM_CFG_BIT_INVERT    28
#define PWM_CFG_BIT_REPEAT    29

#define MAX_FADE_SIZE 4096
#define MAX_CUES      16

#define LED_DEV   "/dev/pwm_led0"
#define FADES_DIR "/usr/share/led-curves/fades"
#define CUES_DIR  "/usr/share/led-curves/cues"

static int write_all(int fd, const char *buf, size_t len)
{
	size_t off = 0;

	while (off < len) {
		ssize_t n = write(fd, buf + off, len - off);
		if (n <= 0)
			return -1;
		off += (size_t)n;
	}
	return 0;
}

static char *read_file(const char *path, size_t *out_size)
{
	FILE *f = fopen(path, "rb");
	struct stat st;
	char *buf;

	if (!f)
		return NULL;

	if (fstat(fileno(f), &st) != 0) {
		fclose(f);
		return NULL;
	}

	buf = malloc((size_t)st.st_size ? (size_t)st.st_size : 1);
	if (!buf) {
		fclose(f);
		return NULL;
	}

	if (st.st_size > 0 && fread(buf, 1, (size_t)st.st_size, f) != (size_t)st.st_size) {
		free(buf);
		fclose(f);
		return NULL;
	}

	fclose(f);
	*out_size = (size_t)st.st_size;
	return buf;
}

static void load_dir(int fd, const char *dir, const char *pattern, int open_cmd,
		      int max_idx, size_t size_mod, long max_size)
{
	DIR *d = opendir(dir);
	struct dirent *entry;

	if (!d) {
		fprintf(stderr, "failed to open %s: %s\n", dir, strerror(errno));
		return;
	}

	while ((entry = readdir(d)) != NULL) {
		char path[PATH_MAX];
		size_t size;
		char *buf;
		int idx;

		if (sscanf(entry->d_name, pattern, &idx) != 1)
			continue;
		if (idx < 0 || idx >= max_idx)
			continue;

		snprintf(path, sizeof(path), "%s/%s", dir, entry->d_name);

		buf = read_file(path, &size);
		if (!buf) {
			fprintf(stderr, "failed to read %s: %s\n", path, strerror(errno));
			continue;
		}

		if (size % size_mod != 0 || (max_size > 0 && (long)size > max_size)) {
			fprintf(stderr, "invalid data size %zu in %s\n", size, path);
			free(buf);
			continue;
		}

		if (ioctl(fd, open_cmd, idx) != 0) {
			fprintf(stderr, "failed to open index %d in %s: %s\n", idx, dir, strerror(errno));
			free(buf);
			continue;
		}

		if (write_all(fd, buf, size) != 0)
			fprintf(stderr, "failed to write data for index %d in %s: %s\n", idx, dir, strerror(errno));

		free(buf);
	}

	closedir(d);
}

int main(void)
{
	unsigned int config;
	int fd;

	fd = open(LED_DEV, O_RDWR);
	if (fd < 0) {
		fprintf(stderr, "failed to open %s: %s\n", LED_DEV, strerror(errno));
		return 1;
	}

	config = PWM_PERIOD |
		 (PWM_PRESCALER << PWM_CFG_BIT_PRESCALER) |
		 (PWM_INVERT << PWM_CFG_BIT_INVERT) |
		 (PWM_REPEAT << PWM_CFG_BIT_REPEAT);
	if (ioctl(fd, PWM_LED_CONFIGURE, config) != 0)
		fprintf(stderr, "failed to configure PWM: %s\n", strerror(errno));

	if (ioctl(fd, PWM_LED_SET_ADAPT, 1) != 0)
		fprintf(stderr, "failed to set adaptive mode: %s\n", strerror(errno));

	if (ioctl(fd, PWM_LED_SET_ACTIVE, 1) != 0)
		fprintf(stderr, "failed to activate %s: %s\n", LED_DEV, strerror(errno));

	load_dir(fd, FADES_DIR, "fade%d", PWM_LED_OPEN_FADE, MAX_FADE_SIZE, 2, MAX_FADE_SIZE);
	load_dir(fd, CUES_DIR, "cue%d", PWM_LED_OPEN_CUE, MAX_CUES, 4, -1);

	close(fd);
	return 0;
}
