LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LDLIBS := -llog

LOCAL_MODULE    := ImageConstuctor
LOCAL_SRC_FILES := org_witness_informa_utils_ImageConstructor.cpp \
			iptc.cpp jpeg.cpp  tiff_ifd.cpp tiff_tag.cpp jpeg_decoder.cpp \
        		byte_swapping.cpp jpeg_marker.cpp debug_flag.cpp \
        		dump.c hashtable.c 	load.c \
        		strbuffer.c utf.c value.c


include $(BUILD_SHARED_LIBRARY)

