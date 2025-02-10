#!/bin/sh

/usr/bin/ioctl /dev/pwm_led0 0x00007545 -v $2 > /dev/null

