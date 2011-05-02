#include <stdlib.h>
#include <stdio.h>
#include <string>
#include <android/log.h>

#include "jpeg.h"
#include "redaction.h"

#include <jni.h>
#include "org_witness_securesmartcam_jpegredaction_JpegRedaction.h"

JNIEXPORT void JNICALL
Java_org_witness_securesmartcam_jpegredaction_JpegRedaction_redactit(JNIEnv *env, jobject obj) {

	__android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","Running");

	try {
		printf("hello world");

		jpeg_redaction::Jpeg j2("/sdcard/windows.jpg", true);
		jpeg_redaction::Redaction::Rect rect(50, 600, 50, 600);
		jpeg_redaction::Redaction redaction;
		redaction.AddRegion(rect);
		j2.ParseImage(redaction, "/sdcard/rawgrey.pgm");
		j2.Save("/sdcard/testoutput.jpg");

	} catch (const char *error) {
		__android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","ERROR");
	}

	__android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","Finished");
}

