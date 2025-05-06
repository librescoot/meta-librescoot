# Disable GUI support for vim on DBC machine
PACKAGECONFIG:remove:librescoot-dbc = "gui gtkgui x11"
DEPENDS:remove:librescoot-dbc = "gtk+ virtual/libx11"