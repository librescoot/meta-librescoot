#!/bin/sh

/usr/bin/ioctl /dev/pwm_led$1 0x00007545 -v $2 > /dev/null

