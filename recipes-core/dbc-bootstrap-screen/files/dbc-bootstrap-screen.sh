#!/bin/sh

console=${1:-/dev/tty1}
printf '\033%%G\033[?25l\033[2J\033[H\n    Librescoot\n\n    Installation / recovery\n\n    Follow the installer on your laptop.\n    Keep power and cables connected.\n\n    Am Laptop dem Installer folgen.\n    Strom und Kabel angeschlossen lassen.\n\n    Hazards on? Check the installer.\n    Warnblinker an? Installer pruefen.\n' > "$console"
