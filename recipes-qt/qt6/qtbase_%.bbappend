PACKAGECONFIG:append:class-target = " ${@bb.utils.contains('DISTRO_FEATURES', 'opengl', 'eglfs kms gbm', '', d)} linuxfb"
