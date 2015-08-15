LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := JpegRedaction
LOCAL_CFLAGS := -fexceptions
LOCAL_LDFLAGS := -Wl,--build-id
LOCAL_SRC_FILES := \
	/home/n8fr8/dev/repos/ObscuraCam3/app/src/main/jni/debug_flag.cpp \
	/home/n8fr8/dev/repos/ObscuraCam3/app/src/main/jni/org_witness_securesmartcam_jpegredaction_JpegRedaction.cpp \
	/home/n8fr8/dev/repos/ObscuraCam3/app/src/main/jni/Makefile.common \
	/home/n8fr8/dev/repos/ObscuraCam3/app/src/main/jni/LICENSE \
	/home/n8fr8/dev/repos/ObscuraCam3/app/src/main/jni/notes.txt \
	/home/n8fr8/dev/repos/ObscuraCam3/app/src/main/jni/jpeg_decoder.cpp \
	/home/n8fr8/dev/repos/ObscuraCam3/app/src/main/jni/Android.mk \
	/home/n8fr8/dev/repos/ObscuraCam3/app/src/main/jni/jpeg_marker.cpp \
	/home/n8fr8/dev/repos/ObscuraCam3/app/src/main/jni/Application.mk \
	/home/n8fr8/dev/repos/ObscuraCam3/app/src/main/jni/iptc.cpp \
	/home/n8fr8/dev/repos/ObscuraCam3/app/src/main/jni/tiff_ifd.cpp \
	/home/n8fr8/dev/repos/ObscuraCam3/app/src/main/jni/tiff_tag.cpp \
	/home/n8fr8/dev/repos/ObscuraCam3/app/src/main/jni/Makefile \
	/home/n8fr8/dev/repos/ObscuraCam3/app/src/main/jni/byte_swapping.cpp \
	/home/n8fr8/dev/repos/ObscuraCam3/app/src/main/jni/jpeg.cpp \
	/home/n8fr8/dev/repos/ObscuraCam3/app/src/main/jni/README.md \

LOCAL_C_INCLUDES += /home/n8fr8/dev/repos/ObscuraCam3/app/src/main/jni
LOCAL_C_INCLUDES += /home/n8fr8/dev/repos/ObscuraCam3/app/src/debug/jni

include $(BUILD_SHARED_LIBRARY)
