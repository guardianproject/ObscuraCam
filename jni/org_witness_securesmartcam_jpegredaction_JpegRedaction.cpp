#include <stdlib.h>
#include <stdio.h>
#include <string>
#include <android/log.h>

#include "jpeg.h"
#include "redaction.h"

#include <jni.h>
#include "org_witness_securesmartcam_jpegredaction_JpegRedaction.h"

JNIEXPORT void JNICALL
Java_org_witness_securesmartcam_jpegredaction_JpegRedaction_redactit(JNIEnv *env, jobject obj, jstring j_src_path, jstring j_dest_path, jstring j_regions) {
  __android_log_write(ANDROID_LOG_INFO,"JPEGREDACTION","Running");
  try {
    //    const char *source_filename = "/sdcard/windows.jpg";
    const char *dest_filename = env->GetStringUTFChars(j_dest_path, NULL);
    const char *source_filename = env->GetStringUTFChars(j_src_path, NULL);
    const char *regions = env->GetStringUTFChars(j_regions, NULL);

    printf("hello world");
    jpeg_redaction::Jpeg jpeg_decoder;
    jpeg_decoder.LoadFromFile(source_filename, true);
    std::string message = "Loaded: ";
    message += source_filename;
    __android_log_write(ANDROID_LOG_INFO,"JPEGREDACTION", message.c_str());
    jpeg_redaction::Redaction redaction;
    redaction.AddRegions(regions);
    jpeg_decoder.DecodeImage(&redaction, NULL);
    jpeg_decoder.RemoveAllSensitive();
    jpeg_decoder.Save(dest_filename);

    message = "Saved: ";
    message += dest_filename;
    __android_log_write(ANDROID_LOG_INFO,"JPEGREDACTION", message.c_str());
  } catch (const char *error) {
    __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","Caught ERROR");
  }

  __android_log_write(ANDROID_LOG_INFO,"JPEGREDACTION","Finished");
}

