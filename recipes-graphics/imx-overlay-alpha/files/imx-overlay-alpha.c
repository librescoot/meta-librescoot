/*
 * imx-overlay-alpha: Control overlay framebuffer alpha for boot transitions
 *
 * Reads/writes /sys/class/graphics/fb1/overlay_alpha to control the
 * IPU Display Processor global alpha for seamless crossfade between
 * boot animation (fb0) and Flutter UI (fb1).
 *
 * Usage:
 *   imx-overlay-alpha <value>
 *       Set alpha directly (0-255)
 *   imx-overlay-alpha fade <from> <to> <ms>
 *       Fade between two alpha values
 *   imx-overlay-alpha redis-fade <host> <port> <channel> <message> <from> <to> <ms> [timeout_ms]
 *       Subscribe to Redis channel, wait for message, then fade.
 *       Also checks HGET <channel> ready=true for restart case.
 */

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <time.h>
#include <errno.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>

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

/* Connect a TCP socket to host:port. Returns fd or -1. */
static int tcp_connect(const char *host, int port)
{
	struct addrinfo hints = {0}, *res;
	char portstr[16];
	snprintf(portstr, sizeof(portstr), "%d", port);
	hints.ai_family = AF_UNSPEC;
	hints.ai_socktype = SOCK_STREAM;
	if (getaddrinfo(host, portstr, &hints, &res) != 0) {
		fprintf(stderr, "getaddrinfo %s:%d failed\n", host, port);
		return -1;
	}
	int sock = socket(res->ai_family, res->ai_socktype, res->ai_protocol);
	if (sock < 0) {
		freeaddrinfo(res);
		return -1;
	}
	if (connect(sock, res->ai_addr, res->ai_addrlen) < 0) {
		close(sock);
		freeaddrinfo(res);
		return -1;
	}
	freeaddrinfo(res);
	return sock;
}

/* Read one RESP line (stripping \r\n) into buf. Returns length or -1 on error/timeout. */
static int resp_read_line(int sock, char *buf, int maxlen)
{
	int n = 0;
	while (n < maxlen - 1) {
		char c;
		int r = recv(sock, &c, 1, 0);
		if (r <= 0)
			return -1;
		if (c == '\n') {
			buf[n] = '\0';
			return n;
		}
		if (c != '\r')
			buf[n++] = c;
	}
	buf[n] = '\0';
	return n;
}

/*
 * Send a RESP command and read the single-line response.
 * Returns the bulk string value in out_buf, or NULL on error.
 */
static const char *resp_command(int sock, const char *cmd,
				char *out_buf, int out_len)
{
	if (send(sock, cmd, strlen(cmd), 0) < 0)
		return NULL;

	char line[256];
	/* Read response type line */
	if (resp_read_line(sock, line, sizeof(line)) < 0)
		return NULL;

	if (line[0] == '$') {
		int blen = atoi(line + 1);
		if (blen < 0) {
			/* Null bulk string */
			out_buf[0] = '\0';
			return out_buf;
		}
		if (resp_read_line(sock, out_buf, out_len) < 0)
			return NULL;
		return out_buf;
	} else if (line[0] == '+' || line[0] == ':') {
		snprintf(out_buf, out_len, "%s", line + 1);
		return out_buf;
	}
	return NULL;
}

/*
 * Check if HGET <hash> <field> == expected.
 * Returns 1 if match, 0 otherwise.
 */
static int redis_hget_equals(const char *host, int port,
			     const char *hash, const char *field,
			     const char *expected)
{
	int sock = tcp_connect(host, port);
	if (sock < 0)
		return 0;

	struct timeval tv = { .tv_sec = 2, .tv_usec = 0 };
	setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));

	char cmd[512];
	snprintf(cmd, sizeof(cmd),
		 "*3\r\n$4\r\nHGET\r\n$%zu\r\n%s\r\n$%zu\r\n%s\r\n",
		 strlen(hash), hash, strlen(field), field);

	char val[256] = {0};
	int match = 0;
	if (resp_command(sock, cmd, val, sizeof(val)))
		match = (strcmp(val, expected) == 0);

	close(sock);
	return match;
}

/*
 * Subscribe to <channel> and wait for <message>.
 * Returns 0 when received, 1 on timeout, -1 on error.
 *
 * RESP subscribe messages arrive as 3-element arrays:
 *   *3\r\n $7\r\n message\r\n $<n>\r\n <channel>\r\n $<m>\r\n <data>\r\n
 */
static int redis_subscribe_wait(const char *host, int port,
				const char *channel, const char *message,
				int timeout_ms)
{
	int sock = tcp_connect(host, port);
	if (sock < 0) {
		perror("redis connect");
		return -1;
	}

	struct timeval tv = {
		.tv_sec  = timeout_ms / 1000,
		.tv_usec = (timeout_ms % 1000) * 1000,
	};
	setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));

	char cmd[256];
	snprintf(cmd, sizeof(cmd),
		 "*2\r\n$9\r\nSUBSCRIBE\r\n$%zu\r\n%s\r\n",
		 strlen(channel), channel);
	if (send(sock, cmd, strlen(cmd), 0) < 0) {
		close(sock);
		return -1;
	}

	/*
	 * Read RESP lines. Track last 3 bulk string values seen.
	 * When they are {"message", channel, target_message} we're done.
	 */
	char parts[3][256] = {{0}};
	int part_idx = 0;
	int in_bulk = 0;
	char line[256];

	while (1) {
		if (resp_read_line(sock, line, sizeof(line)) < 0) {
			close(sock);
			/* EAGAIN/EWOULDBLOCK = timeout */
			return (errno == EAGAIN || errno == EWOULDBLOCK) ? 1 : -1;
		}

		if (line[0] == '*') {
			part_idx = 0;
			memset(parts, 0, sizeof(parts));
			in_bulk = 0;
		} else if (line[0] == '$') {
			in_bulk = (atoi(line + 1) >= 0);
		} else if (in_bulk) {
			if (part_idx < 3)
				strncpy(parts[part_idx++], line, 255);
			in_bulk = 0;

			if (part_idx == 3 &&
			    strcmp(parts[0], "message") == 0 &&
			    strcmp(parts[1], channel) == 0 &&
			    strcmp(parts[2], message) == 0) {
				close(sock);
				return 0;
			}
		}
	}
}

/*
 * Wait for scootui to signal readiness via Redis, then fade the overlay in.
 * Checks HGET <channel> ready first (handles service restarts), then subscribes.
 */
static int do_redis_fade(int alpha_fd,
			 const char *host, int port,
			 const char *channel, const char *message,
			 int from, int to, int fade_ms, int timeout_ms)
{
	/* Fast path: already ready (e.g. dispatcher restarted after first boot) */
	if (redis_hget_equals(host, port, channel, "ready", "true")) {
		fprintf(stderr, "%s already ready\n", channel);
		return do_fade(alpha_fd, from, to, fade_ms);
	}

	fprintf(stderr, "waiting for PUBLISH %s %s (timeout %ds)...\n",
		channel, message, timeout_ms / 1000);

	int ret = redis_subscribe_wait(host, port, channel, message, timeout_ms);
	if (ret != 0)
		fprintf(stderr, "timeout after %ds, fading anyway\n", timeout_ms / 1000);
	else
		fprintf(stderr, "received %s %s, fading\n", channel, message);

	return do_fade(alpha_fd, from, to, fade_ms);
}

int main(int argc, char *argv[])
{
	if (argc < 2) {
		fprintf(stderr,
			"usage: imx-overlay-alpha <value>\n"
			"       imx-overlay-alpha fade <from> <to> <ms>\n"
			"       imx-overlay-alpha redis-fade <host> <port> <channel> <message>"
			" <from> <to> <ms> [timeout_ms]\n");
		return 1;
	}

	int fd = open(ALPHA_PATH, O_WRONLY);
	if (fd < 0) {
		perror("open " ALPHA_PATH);
		return 1;
	}

	int ret = 0;

	if (strcmp(argv[1], "fade") == 0) {
		if (argc < 5) {
			fprintf(stderr, "usage: imx-overlay-alpha fade <from> <to> <ms>\n");
			close(fd);
			return 1;
		}
		ret = do_fade(fd, atoi(argv[2]), atoi(argv[3]), atoi(argv[4]));

	} else if (strcmp(argv[1], "redis-fade") == 0) {
		if (argc < 9) {
			fprintf(stderr,
				"usage: imx-overlay-alpha redis-fade <host> <port>"
				" <channel> <message> <from> <to> <ms> [timeout_ms]\n");
			close(fd);
			return 1;
		}
		const char *host    = argv[2];
		int         port    = atoi(argv[3]);
		const char *channel = argv[4];
		const char *message = argv[5];
		int         from    = atoi(argv[6]);
		int         to      = atoi(argv[7]);
		int         fade_ms = atoi(argv[8]);
		int         timeout = argc > 9 ? atoi(argv[9]) : 30000;
		ret = do_redis_fade(fd, host, port, channel, message,
				    from, to, fade_ms, timeout);

	} else {
		int alpha = atoi(argv[1]);
		if (alpha < 0 || alpha > 255) {
			fprintf(stderr, "alpha must be 0-255\n");
			close(fd);
			return 1;
		}
		ret = set_alpha(fd, alpha);
	}

	close(fd);
	return ret ? 1 : 0;
}
