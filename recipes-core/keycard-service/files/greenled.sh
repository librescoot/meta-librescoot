#!/bin/sh

if [ $1 == "1" ]
then
  i2cset -f -y 2 0x30 0x00 0xc0 0x3f 0x00 0xff 0x00 0xaf 0xaf 0xaf 0x61 i
else
  i2cset -f -y 2 0x30 0x00 0x40 0x3f 0x00 0x00 0x00 0x32 0x32 0x32 0x01 i
fi
