SUMMARY = "Weston, a Wayland compositor, i.MX fork"
DESCRIPTION = "Weston is the reference implementation of a Wayland compositor"
HOMEPAGE = "http://wayland.freedesktop.org"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://COPYING;md5=d79ee9e66bb0f95d3386a7acae780b70 \
                    file://libweston/compositor.c;endline=26;md5=f47553ae598090444273db00adfb5b66"

#DEFAULT_PREFERENCE = "-1"

WESTON_SRC ?= "git://source.codeaurora.org/external/imx/weston-imx.git;protocol=https"
SRCBRANCH = "weston-imx-5.0"
SRC_URI = "${WESTON_SRC};branch=${SRCBRANCH} \
           file://weston.png \
           file://weston.desktop \
           file://0001-make-error-portable.patch \
           file://xwayland.weston-start \
           file://0001-weston-launch-Provide-a-default-version-that-doesn-t.patch \
           file://0003-weston-touch-calibrator-Advertise-the-touchscreen-ca.patch \
"
# Use argb8888 as gbm-format for i.MX8MQ only
SRC_URI_append_mx8mq = " file://0001-weston.ini-using-argb8888-as-gbm-default-on-mscale-8.patch \
                         file://0002-weston.ini-configure-desktop-shell-size-in-weston-co.patch \
"

SRCREV = "6ed61e30a8afdb31b0f375c2042baa2c2de7e085"
S = "${WORKDIR}/git"

UPSTREAM_CHECK_URI = "https://wayland.freedesktop.org/releases.html"

inherit autotools pkgconfig useradd distro_features_check
# Disable OpenGL for parts with GPU support for 2D but not 3D
REQUIRED_DISTRO_FEATURES          = "opengl"
REQUIRED_DISTRO_FEATURES_imxgpu2d = ""
REQUIRED_DISTRO_FEATURES_imxgpu3d = "opengl"

DEPENDS = "libxkbcommon gdk-pixbuf pixman cairo glib-2.0 jpeg"
DEPENDS += "wayland wayland-protocols libinput virtual/egl pango wayland-native"

WESTON_MAJOR_VERSION = "${@'.'.join(d.getVar('PV').split('.')[0:1])}"


EXTRA_OECONF = "--enable-setuid-install \
                --disable-rdp-compositor \
                "
EXTRA_OECONF_append_qemux86 = "\
		WESTON_NATIVE_BACKEND=fbdev-backend.so \
		"
EXTRA_OECONF_append_qemux86-64 = "\
		WESTON_NATIVE_BACKEND=fbdev-backend.so \
		"
EXTRA_OECONF_append_mx6 = "\
		WESTON_NATIVE_BACKEND=fbdev-backend.so \
		"
EXTRA_OECONF_append_mx7 = "\
		WESTON_NATIVE_BACKEND=fbdev-backend.so \
		"

IMX_EXTRA_OECONF_OPENGL          = ""
IMX_EXTRA_OECONF_OPENGL_imxgpu2d = " --disable-opengl"
IMX_EXTRA_OECONF_OPENGL_imxgpu3d = ""
EXTRA_OECONF_append = "${IMX_EXTRA_OECONF_OPENGL}"

PACKAGECONFIG ??= "${@bb.utils.contains('DISTRO_FEATURES', 'wayland', 'kms fbdev wayland egl', '', d)} \
                   ${@bb.utils.contains('DISTRO_FEATURES', 'x11 wayland', 'xwayland', '', d)} \
                   ${@bb.utils.filter('DISTRO_FEATURES', 'opengl pam systemd x11', d)} \
                   clients launch"
# drm is not supported on mx6/mx7
PACKAGECONFIG_remove_mx6 = "kms"
PACKAGECONFIG_remove_mx7 = "kms"
PACKAGECONFIG_append_imxgpu   = " imxgpu"
PACKAGECONFIG_append_imxgpu2d = " imxg2d"
PACKAGECONFIG_append_imxgpu3d = " cairo-glesv2"
#
# Compositor choices
#
# Weston on KMS
PACKAGECONFIG[kms] = "--enable-drm-compositor,--disable-drm-compositor,drm udev virtual/mesa mtdev"
# Weston on Wayland (nested Weston)
PACKAGECONFIG[wayland] = "--enable-wayland-compositor,--disable-wayland-compositor,virtual/mesa"
# Weston on X11
PACKAGECONFIG[x11] = "--enable-x11-compositor,--disable-x11-compositor,virtual/libx11 libxcb libxcb libxcursor cairo"
# Headless Weston
PACKAGECONFIG[headless] = "--enable-headless-compositor,--disable-headless-compositor"
# Weston on framebuffer
PACKAGECONFIG[fbdev] = "--enable-fbdev-compositor,--disable-fbdev-compositor,udev mtdev"
# weston-launch
PACKAGECONFIG[launch] = "--enable-weston-launch,--disable-weston-launch,drm"
# VA-API desktop recorder
PACKAGECONFIG[vaapi] = "--enable-vaapi-recorder,--disable-vaapi-recorder,libva"
# Weston with EGL support
PACKAGECONFIG[egl] = "--enable-egl --enable-simple-egl-clients,--disable-egl --disable-simple-egl-clients,virtual/egl"
# Weston with cairo glesv2 support
PACKAGECONFIG[cairo-glesv2] = "--with-cairo-glesv2,--with-cairo=image,cairo"
# Weston with lcms support
PACKAGECONFIG[lcms] = "--enable-lcms,--disable-lcms,lcms"
# Weston with webp support
PACKAGECONFIG[webp] = "--with-webp,--without-webp,libwebp"
# Weston with systemd-login support
PACKAGECONFIG[systemd] = "--enable-systemd-login,--disable-systemd-login,systemd dbus"
# Weston with Xwayland support (requires X11 and Wayland)
PACKAGECONFIG[xwayland] = "--enable-xwayland,--disable-xwayland"
# colord CMS support
PACKAGECONFIG[colord] = "--enable-colord,--disable-colord,colord"
# Clients support
PACKAGECONFIG[clients] = "--enable-clients --enable-simple-clients --enable-demo-clients-install,--disable-clients --disable-simple-clients"
# Weston with PAM support
PACKAGECONFIG[pam] = "--with-pam,--without-pam,libpam"
# Weston with i.MX G2D renderer
PACKAGECONFIG[imxg2d] = "--enable-imxg2d,--disable-imxg2d,virtual/libg2d"
# Weston with OpenGL support
PACKAGECONFIG[opengl] = "--enable-opengl,--disable-opengl"
# Weston with imxgpu hardware
PACKAGECONFIG[imxgpu] = "--enable-imxgpu,--disable-imxgpu"

do_install_append() {
    # Weston doesn't need the .la files to load modules, so wipe them
    rm -f ${D}/${libdir}/libweston-${WESTON_MAJOR_VERSION}/*.la

    # If X11, ship a desktop file to launch it
    if [ "${@bb.utils.filter('DISTRO_FEATURES', 'x11', d)}" ]; then
        install -d ${D}${datadir}/applications
        install ${WORKDIR}/weston.desktop ${D}${datadir}/applications

        install -d ${D}${datadir}/icons/hicolor/48x48/apps
        install ${WORKDIR}/weston.png ${D}${datadir}/icons/hicolor/48x48/apps
    fi

    if [ "${@bb.utils.contains('PACKAGECONFIG', 'xwayland', 'yes', 'no', d)}" = "yes" ]; then
        install -Dm 644 ${WORKDIR}/xwayland.weston-start ${D}${datadir}/weston-start/xwayland
    fi

    if [ "${@bb.utils.filter('BBFILE_COLLECTIONS', 'ivi', d)}" ]; then
        WESTON_INI_SRC=${B}/ivi-shell/weston.ini
    else
        WESTON_INI_SRC=${B}/weston.ini
    fi
    WESTON_INI_DEST_DIR=${D}${sysconfdir}/xdg/weston
    if [ -z "${@bb.utils.filter('BBFILE_COLLECTIONS', 'aglprofilegraphical', d)}" ]; then
        install -d ${WESTON_INI_DEST_DIR}
        install -m 0644 ${WESTON_INI_SRC} ${WESTON_INI_DEST_DIR}
    fi
}

PACKAGES += "${@bb.utils.contains('PACKAGECONFIG', 'xwayland', '${PN}-xwayland', '', d)} \
             libweston-${WESTON_MAJOR_VERSION} ${PN}-examples"

FILES_${PN} = "${bindir}/weston ${bindir}/weston-terminal ${bindir}/weston-info ${bindir}/weston-launch ${bindir}/wcap-decode ${libexecdir} ${libdir}/${BPN}/*.so ${datadir}"
FILES_${PN} += "${sysconfdir}/xdg/weston"

FILES_libweston-${WESTON_MAJOR_VERSION} = "${libdir}/lib*${SOLIBS} ${libdir}/libweston-${WESTON_MAJOR_VERSION}/*.so"
SUMMARY_libweston-${WESTON_MAJOR_VERSION} = "Helper library for implementing 'wayland window managers'."

FILES_${PN}-examples = "${bindir}/*"

FILES_${PN}-xwayland = "${libdir}/libweston-${WESTON_MAJOR_VERSION}/xwayland.so"
RDEPENDS_${PN}-xwayland += "xserver-xorg-xwayland"

RDEPENDS_${PN} += "xkeyboard-config"
RRECOMMENDS_${PN} = "liberation-fonts"
RRECOMMENDS_${PN}-dev += "wayland-protocols"

USERADD_PACKAGES = "${PN}"
GROUPADD_PARAM_${PN} = "--system weston-launch"

PACKAGE_ARCH = "${MACHINE_SOCARCH}"
