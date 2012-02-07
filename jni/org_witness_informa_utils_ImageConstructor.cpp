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
#include <jansson.h>

JNIEXPORT jstring JNICALL
Java_org_witness_informa_utils_ImageConstructor
(JNIEnv *env, jobject obj, jstring jstrOriginalImageFilename,
 jstring jstrMetadataObjectString, int mdObjectLength) {
    __android_log_write(ANDROID_LOG_DEBUG, "INFORMA_JNI", "Running.");
    
    try {
        jpeg_redaction::Jpeg decoder;
        const char* originalImageFilename;
        const char* metadataObjectString;
        
        json_t* metadata;
        json_error_t jsonError;
        
        originalImageFilename = (env)->GetStringUTFChars(jstrOriginalImageFilename, NULL);
        metadataObjectString = (env)->GetStringUTFChars(jstrMetadataObjectString, NULL);
        metadata = json_object();
        
        metadata = json_loads(metadataObjectString, &jsonError);
        
        __android_log_write(ANDROID_LOG_DEBUG, "INFORMA_JNI", "we have metadata:\n");
        __android_log_write(ANDROID_LOG_DEBUG, "INFORMA_JNI", json_dumps(metadata, 0));
        
        decoder.LoadFromFile(originalImageFilename, true);
        __android_log_write(ANDROID_LOG_DEBUG, "INFORMA_JNI","loading original image");
        
        
        //jpeg_redaction::Redaction::Region region(l,r,t,b);
        
    } catch (const char *error) {
        __android_log_write(ANDROID_LOG_ERROR, "INFORMA_JNI",error);
    }
    
    __android_log_write(ANDROID_LOG_DEBUG, "INFORMA_JNI","Finished!");
}

