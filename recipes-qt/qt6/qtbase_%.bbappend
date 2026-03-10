PACKAGECONFIG:remove:class-target = "${@bb.utils.contains('DISTRO_FEATURES', 'opengl', 'no-opengl', '', d)}"
PACKAGECONFIG:append:class-target = " ${@bb.utils.contains('DISTRO_FEATURES', 'opengl', 'gles2 eglfs kms gbm', '', d)} linuxfb"
