# Disable GUI support for vim on DBC machine
PACKAGECONFIG:remove:unu-dbc = "gui gtkgui x11"
DEPENDS:remove:unu-dbc = "gtk+ virtual/libx11"

PACKAGECONFIG:remove:librescoot-dbc-rpi4 = "gui gtkgui x11"
DEPENDS:remove:librescoot-dbc-rpi4 = "gtk+ virtual/libx11"
