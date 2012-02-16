//
//  org_witness_informa_utils_ImageConstructor.cpp
//  
//
//  Created by Harlo Holmes on 2/7/12.
//  
//

#include <stdlib.h>
#include <stdio.h>
#include <android/log.h>

#include <string>
#include <time.h>
#include "jpeg.h"
#include "debug_flag.h"
#include "redaction.h"
#include "parse_functions.h"
#include "org_witness_informa_utils_ImageConstructor.h"

JNIEXPORT int JNICALL
Java_org_witness_informa_utils_ImageConstructor_setRegion
(JNIEnv *env, jobject obj, jstring jstrOriginalImageFilename, jstring jstrInformaImageFilename, int left, int right, int top, int bottom, jstring jstrRedactionMethod, jcharArray jcharResultBuffer) {
    
    jpeg_redaction::Jpeg original;
    char* redactionBlock;
    const char* originalImageFilename;
    const char* informaImageFilename;
    const char* redactionMethod;
    
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
        
        redactionBlock = new char[redactionPack.size() + 1];
        copy(redactionPack.begin(), redactionPack.end(), redactionBlock);
        
        jchar* jResultBuffer = (jchar*)calloc(sizeof(jchar), redactionPack.size() + 1);
        for(int i = 0; i < (redactionPack.size() + 1); i++) {
            jResultBuffer[i] = (jchar) redactionBlock[i];
        }
        free(redactionBlock);
        
        env->SetCharArrayRegion(jcharResultBuffer, 0, (redactionPack.size() + 1), jResultBuffer);
        free(jResultBuffer);
        
        original.DecodeImage(&redaction, NULL);
        __android_log_write(ANDROID_LOG_DEBUG, TAG,"region redacted!");

        original.Save(informaImageFilename);
        __android_log_write(ANDROID_LOG_DEBUG, TAG,"image saved!");
        (env)->ReleaseStringUTFChars(jstrOriginalImageFilename, originalImageFilename);
        (env)->ReleaseStringUTFChars(jstrInformaImageFilename, informaImageFilename);
        (env)->ReleaseStringUTFChars(jstrRedactionMethod, redactionMethod);
        
    } catch (const char *error) {
        __android_log_write(ANDROID_LOG_ERROR, TAG, error);
        exit(0);
    }
    
    __android_log_write(ANDROID_LOG_DEBUG, TAG,"Finished!");
    return 1;
    
}

JNIEXPORT int JNICALL
Java_org_witness_informa_utils_ImageConstructor_constructImage
(JNIEnv *env, jobject obj, jstring jstrOriginalImageFilename, jstring jstrInformaImageFilename, jstring jstrMetadataObjectString, int metadataLength) {

    jpeg_redaction::Jpeg original;
    jpeg_redaction::Jpeg check;
    const char* originalImageFilename;
    const char* informaImageFilename;
    const char* metadataObjectString;
    std::vector<unsigned char> metadata;
    
    const char* TAG = "********************** INFORMA_JNI **********************";
    __android_log_write(ANDROID_LOG_DEBUG, TAG, "3. setting metadata");
    
    try {
                
        originalImageFilename = (env)->GetStringUTFChars(jstrOriginalImageFilename, NULL);
        informaImageFilename = (env)->GetStringUTFChars(jstrInformaImageFilename, NULL);
        metadataObjectString = (env)->GetStringUTFChars(jstrMetadataObjectString, NULL);
        
        __android_log_write(ANDROID_LOG_DEBUG, TAG, "we have metadata:\n");
        __android_log_write(ANDROID_LOG_DEBUG, TAG, metadataObjectString);
        
        // copy object into metadata vector
        metadata.resize(metadataLength + 1);
        memcpy(&metadata[0], &metadataObjectString, metadataLength + 1);
        
        // set metadata into original
        __android_log_write(ANDROID_LOG_DEBUG, TAG, "loading original image:\n");
        __android_log_write(ANDROID_LOG_DEBUG, TAG, originalImageFilename);
        
        bool success = original.LoadFromFile(originalImageFilename, true);
        if(!success) {
            (env)->ReleaseStringUTFChars(jstrOriginalImageFilename, originalImageFilename);
            (env)->ReleaseStringUTFChars(jstrInformaImageFilename, informaImageFilename);
            (env)->ReleaseStringUTFChars(jstrMetadataObjectString, metadataObjectString);

            exit(1);
        }
        
        original.SetObscuraMetaData(metadata.size(), &metadata.front());
        original.Save(informaImageFilename);
        
        
        (env)->ReleaseStringUTFChars(jstrOriginalImageFilename, originalImageFilename);
        (env)->ReleaseStringUTFChars(jstrInformaImageFilename, informaImageFilename);
        (env)->ReleaseStringUTFChars(jstrMetadataObjectString, metadataObjectString);
        
        
        //jpeg_redaction::Redaction::Region region(l,r,t,b);
        
    } catch (const char *error) {
        __android_log_write(ANDROID_LOG_ERROR, TAG,error);
    }
    
    __android_log_write(ANDROID_LOG_DEBUG, TAG,"Finished!");
    return metadataLength;
}

