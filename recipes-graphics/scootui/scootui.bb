SUMMARY = "scootui"
DESCRIPTION = "ScootUI"
AUTHOR = "Danylo Storozhev, André Bierlein, Teal Bauer"
HOMEPAGE = "None"
BUGTRACKER = "None"
SECTION = "graphics"

LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

BBINCLUDELOGS = "yes"

SCOOTUI_BRANCH ??= "main"
SCOOTUI_SRCREV ??= "${AUTOREV}"
SRCREV = "${SCOOTUI_SRCREV}"
SRC_URI = "git://github.com/librescoot/scootui.git;lfs=0;branch=${SCOOTUI_BRANCH};protocol=https;destsuffix=git"
SRC_URI += "file://scootui.service"
SRC_URI += "file://scootui-rpi4.service"

PV = "0.4.15+git"
# PR = "r0"

inherit flutter-app systemd

S = "${WORKDIR}/git"

PUBSPEC_APPNAME = "scooter_cluster"
FLUTTER_APPLICATION_INSTALL_SUFFIX = "scootui"
PUBSPEC_ENFORCE_LOCKFILE = "0"
PUBSPEC_IGNORE_LOCKFILE = "1"
FLUTTER_APPLICATION_PATH = ""
FLUTTER_BUILD_ARGS = "bundle --no-pub"

# Flutter 3.32.x: flutter pub get checks security advisories from pub.dev even
# with --offline, failing with exit 69 in the Yocto sandbox. The advisory cache
# in the pub_cache archive has stale timestamps so pub tries to re-fetch.
#
# Additionally, flutter pub get runs gen_localizations internally, which creates
# build/ as a side effect. build_app() in meta-flutter's common.inc sees build/
# and runs flutter clean, which wipes .dart_tool/package_config.json. This
# causes flutter build bundle --no-pub to fail with missing package_config.json.
#
# Fix: override do_compile to (a) refresh advisory timestamps, (b) delete build/
# between pub get and the build_app() call so flutter clean is not triggered.
python do_compile() {
    import glob
    import json
    import os
    import shutil
    from datetime import datetime, timezone

    # Refresh advisory cache timestamps so pub skips the network fetch
    cache_dir = os.path.join(d.getVar('PUB_CACHE'), 'hosted', 'pub.dev', '.cache')
    if os.path.exists(cache_dir):
        now = datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%S.000000Z')
        empty = {"advisories": [], "advisoriesUpdated": now}
        for versions_file in glob.glob(os.path.join(cache_dir, '*-versions.json')):
            pkg = os.path.basename(versions_file).replace('-versions.json', '')
            advisory_file = os.path.join(cache_dir, f'{pkg}-advisories.json')
            if os.path.exists(advisory_file):
                with open(advisory_file) as f:
                    data = json.load(f)
                data['advisoriesUpdated'] = now
                with open(advisory_file, 'w') as f:
                    json.dump(data, f)
            else:
                with open(advisory_file, 'w') as f:
                    json.dump(empty, f)
        bb.note(f'Refreshed advisory cache timestamps in {cache_dir}')

    pubspec_yaml_appname = get_pubspec_yaml_appname(d)
    pubspec_appname = d.getVar("PUBSPEC_APPNAME")
    if pubspec_appname != pubspec_yaml_appname:
        bb.fatal("Set PUBSPEC_APPNAME to match name value in pubspec.yaml")

    flutter_sdk = d.getVar('FLUTTER_SDK')
    env = os.environ
    env['PATH'] = f'{flutter_sdk}/bin:{d.getVar("PATH")}'
    env['PUB_CACHE'] = d.getVar('PUB_CACHE')
    staging_dir_target = d.getVar('STAGING_DIR_TARGET')
    env['PKG_CONFIG_PATH'] = f'{staging_dir_target}/usr/lib/pkgconfig:{staging_dir_target}/usr/share/pkgconfig:{d.getVar("PKG_CONFIG_PATH")}'
    workdir = d.getVar("WORKDIR")
    env['XDG_CONFIG_HOME'] = f'{workdir}'

    bb.note(f'{env}')

    source_dir = d.getVar('S')
    flutter_application_path = d.getVar('FLUTTER_APPLICATION_PATH')
    source_root = os.path.join(source_dir, flutter_application_path)

    cmd = d.getVar('FLUTTER_PREBUILD_CMD')
    if cmd != '':
        run_command(d, cmd, source_root, env)

    run_command(d, 'flutter config --no-analytics', source_root, env)
    run_command(d, 'flutter config --no-cli-animations', source_root, env)
    run_command(d, 'flutter pub get --enforce-lockfile --offline -v', source_root, env)

    # flutter pub get runs gen_localizations which creates build/ as a side effect.
    # build_app() skips flutter clean when build/ is absent, preserving package_config.json.
    build_dir = os.path.join(source_root, 'build')
    if os.path.exists(build_dir):
        shutil.rmtree(build_dir)
        bb.note('Removed build/ after pub get to prevent flutter clean from wiping .dart_tool')

    build_type = d.getVar('BUILD_TYPE')
    if build_type == 'web':
        build_web(d, source_root, env)
    elif build_type == 'app':
        build_app(d, source_dir, flutter_sdk, pubspec_appname, flutter_application_path, staging_dir_target, source_root, env)
}

SYSTEMD_SERVICE:${PN} = "scootui.service"
SYSTEMD_AUTO_ENABLE:${PN} = "disable"

do_install:append:unu-dbc() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/scootui.service ${D}${systemd_system_unitdir}/
}

do_install:append:librescoot-dbc-rpi4() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/scootui-rpi4.service ${D}${systemd_system_unitdir}/scootui.service
}
