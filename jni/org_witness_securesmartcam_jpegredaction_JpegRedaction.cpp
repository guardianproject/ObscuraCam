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
    
    jpeg_decoder.RemoveAllSensitive();
    
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

JNIEXPORT jbyteArray JNICALL
Java_org_witness_securesmartcam_jpegredaction_JpegRedaction_setRegion
(JNIEnv *env, jobject obj, jstring jstrOriginalImageFilename, jstring jstrInformaImageFilename, int left, int right, int top, int bottom, jstring jstrRedactionMethod) {
    
    jpeg_redaction::Jpeg original;
    char* redactionBlock;
    const char* originalImageFilename;
    const char* informaImageFilename;
    const char* redactionMethod;
    jbyteArray regionBuffer;
    
    const char* TAG = "********************** INFORMA_JNI **********************";
    __android_log_write(ANDROID_LOG_DEBUG, TAG, "2. adding image region");
    
    try {
        originalImageFilename = (env)->GetStringUTFChars(jstrOriginalImageFilename, NULL);
        informaImageFilename = (env)->GetStringUTFChars(jstrInformaImageFilename, NULL);
        redactionMethod = (env)->GetStringUTFChars(jstrRedactionMethod, NULL);
        
        bool success = original.LoadFromFile(originalImageFilename, true);
        if(!success) {
            (env)->ReleaseStringUTFChars(jstrOriginalImageFilename, originalImageFilename);
            (env)->ReleaseStringUTFChars(jstrInformaImageFilename, informaImageFilename);
            (env)->ReleaseStringUTFChars(jstrRedactionMethod, redactionMethod);            
        }
        __android_log_write(ANDROID_LOG_DEBUG, TAG, "[opened file]");
        __android_log_write(ANDROID_LOG_DEBUG, TAG, originalImageFilename);
        
        jpeg_redaction::Redaction::Region region(left, right, top, bottom);
        region.SetRedactionMethod(redactionMethod);
        jpeg_redaction::Redaction redaction;
        redaction.AddRegion(region);
        __android_log_write(ANDROID_LOG_DEBUG, TAG,"added redact region");
        
        std::vector<unsigned char> redactionPack;
        redaction.Pack(&redactionPack);
        jbyte* buff = new jbyte[redactionPack.size() + 1];
        for(int i=0; i<redactionPack.size() + 1; i++) {
            buff[i] = (jbyte) redactionPack[i];
        }
        
        regionBuffer = (env)->NewByteArray(redactionPack.size() + 1);
        (env)->SetByteArrayRegion(regionBuffer, 0, (redactionPack.size() + 1), (jbyte*) buff);
        free(buff);
        __android_log_write(ANDROID_LOG_DEBUG, TAG,"setting redaction buffer");
        
        original.DecodeImage(&redaction, NULL);
        __android_log_write(ANDROID_LOG_DEBUG, TAG,"region redacted!");
        
        original.Save(informaImageFilename);
        __android_log_write(ANDROID_LOG_DEBUG, TAG,"image saved as");
        __android_log_write(ANDROID_LOG_DEBUG, TAG, informaImageFilename);
        
        (env)->ReleaseStringUTFChars(jstrOriginalImageFilename, originalImageFilename);
        (env)->ReleaseStringUTFChars(jstrInformaImageFilename, informaImageFilename);
        (env)->ReleaseStringUTFChars(jstrRedactionMethod, redactionMethod);
        
    } catch (const char *error) {
        __android_log_write(ANDROID_LOG_ERROR, TAG, error);
        exit(0);
    }
    
    __android_log_write(ANDROID_LOG_DEBUG, TAG,"Finished!");
    return regionBuffer;
    
}

