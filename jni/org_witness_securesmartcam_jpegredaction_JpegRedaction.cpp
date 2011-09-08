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
    const char *source_filename = "/sdcard/windows.jpg";
    const char *dest_filename = "/sdcard/jrl_test_output.jpg";

    printf("hello world");
    jpeg_redaction::Jpeg jpeg_decoder;
    jpeg_decoder.LoadFromFile(source_filename, true);
    __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","Loaded");
    jpeg_redaction::Redaction::Region region(50, 600, 50, 600);
    jpeg_redaction::Redaction redaction;
    redaction.AddRegion(region);
    jpeg_decoder.DecodeImage(&redaction, NULL);
    jpeg_decoder.Save(dest_filename);

  } catch (const char *error) {
    __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","ERROR");
  }

  __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","Finished");
}

