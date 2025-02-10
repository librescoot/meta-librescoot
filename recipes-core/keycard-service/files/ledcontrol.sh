#!/bin/sh

/usr/bin/ioctl /dev/pwm_led0 0x00007546 -v $2 > /dev/null

