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
        metadata.resize(metadataLength);
        for(int i=0;i<metadataLength;i++) {
            metadata[i] = metadataObjectString[i];
        }
        
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
        
        __android_log_write(ANDROID_LOG_DEBUG, TAG, "saving metadata...");
        original.SetObscuraMetaData(metadata.size(), &metadata.front());
        original.Save(informaImageFilename);
        
        (env)->ReleaseStringUTFChars(jstrOriginalImageFilename, originalImageFilename);
        (env)->ReleaseStringUTFChars(jstrInformaImageFilename, informaImageFilename);
        (env)->ReleaseStringUTFChars(jstrMetadataObjectString, metadataObjectString);
        
    } catch (const char *error) {
        __android_log_write(ANDROID_LOG_ERROR, TAG,error);
    }
    
    __android_log_write(ANDROID_LOG_DEBUG, TAG,"Finished!");
    return metadataLength;
}

