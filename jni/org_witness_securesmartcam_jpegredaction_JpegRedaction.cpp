#include <stdlib.h>
#include <stdio.h>
#include <string>
#include <android/log.h>

#include "jpeg.h"
#include "redaction.h"

#include <jni.h>
#include "org_witness_securesmartcam_jpegredaction_JpegRedaction.h"

JNIEXPORT void JNICALL
Java_org_witness_securesmartcam_jpegredaction_JpegRedaction_redactRegion(JNIEnv *env, jobject obj, jstring jstrSrcFilename, jstring jstrDestFilename, int left, int right, int top, int bottom, jstring jStrRedactionMethod) {
  __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","Running");
  try {

    jpeg_redaction::Jpeg jpeg_decoder;
    const char* strSrcFilename;
    const char* strDestFilename;
    const char* strRedactionMethod;
    
    strSrcFilename = (env)->GetStringUTFChars(jstrSrcFilename , NULL);
    strDestFilename = (env)->GetStringUTFChars(jstrDestFilename , NULL);
    strRedactionMethod = (env)->GetStringUTFChars(jStrRedactionMethod , NULL);
    
    jpeg_decoder.LoadFromFile(strSrcFilename, true);
    __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","Loaded");
    __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION",strSrcFilename);
    
    jpeg_redaction::Redaction::Region region(left, right, top, bottom);
    region.SetRedactionMethod(strRedactionMethod);
    jpeg_redaction::Redaction redaction;
    redaction.AddRegion(region);
    __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","added redact region");
    
    jpeg_decoder.DecodeImage(&redaction,NULL);
    __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","redacted");
    
    jpeg_decoder.Save(strDestFilename);
    __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","saved");

	(env)->ReleaseStringUTFChars(jstrSrcFilename , strSrcFilename); // release jstring
	(env)->ReleaseStringUTFChars(jstrDestFilename , strDestFilename); // release jstring
    

  } catch (const char *error) {
    __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","ERROR");
  }

  __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","Finished");
}

JNIEXPORT void JNICALL
Java_org_witness_securesmartcam_jpegredaction_JpegRedaction_redactRegions(JNIEnv *env, jobject obj, jstring jstrSrcFilename, jstring jstrDestFilename, jstring jStrRedactionCmd) {
  __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","Running");
  try {

    jpeg_redaction::Jpeg jpeg_decoder;
    const char* strSrcFilename;
    const char* strDestFilename;
    const char* strRedactionCmd;
    
    strSrcFilename = (env)->GetStringUTFChars(jstrSrcFilename , NULL);
    strDestFilename = (env)->GetStringUTFChars(jstrDestFilename , NULL);
    strRedactionCmd = (env)->GetStringUTFChars(jStrRedactionCmd , NULL);
    
    jpeg_decoder.LoadFromFile(strSrcFilename, true);
    __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","Loaded");
    __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION",strSrcFilename);
    
    jpeg_redaction::Redaction redaction;
    redaction.AddRegions(strRedactionCmd);
    __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","added redact regions");
    
    jpeg_decoder.DecodeImage(&redaction,NULL);
    __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","redacted");
    
    jpeg_decoder.Save(strDestFilename);
    __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","saved");

	(env)->ReleaseStringUTFChars(jstrSrcFilename , strSrcFilename); // release jstring
	(env)->ReleaseStringUTFChars(jstrDestFilename , strDestFilename); // release jstring
    

  } catch (const char *error) {
    __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","ERROR");
  }

  __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","Finished");
}

