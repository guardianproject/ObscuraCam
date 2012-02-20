//
//  org_witness_informa_utils_ImageConstructor.cpp
//  
//
//  Created by Harlo Holmes on 2/7/12.
//  
//

#include <stdlib.h>
#include <stdio.h>
#include <string>
#include <android/log.h>

#include "jpeg.h"
#include "debug_flag.h"
#include "redaction.h"

#include <jni.h>
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

JNIEXPORT jbyteArray JNICALL
Java_org_witness_informa_utils_ImageConstructor_redactRegion(JNIEnv *env, jobject obj, jstring jstrSrcFilename, jstring jstrDestFilename, int left, int right, int top, int bottom, jstring jStrRedactionCommand) {
    
    const char* TAG = "*************INFORMA_JNI***************";
    const char* PROC = "JPEGREDACTION";
    
    __android_log_write(ANDROID_LOG_ERROR, PROC, "Running");
    unsigned int newPackSize = 0;
    jbyteArray returnedPack;
    
    try {
        
        jpeg_redaction::Jpeg jpeg_decoder;
        const char* strSrcFilename;
        const char* strDestFilename;
        const char* strRedactionCommand;
        
        strSrcFilename = (env)->GetStringUTFChars(jstrSrcFilename , NULL);
        strDestFilename = (env)->GetStringUTFChars(jstrDestFilename , NULL);
        strRedactionCommand = (env)->GetStringUTFChars(jStrRedactionCommand , NULL);
        
        jpeg_decoder.LoadFromFile(strSrcFilename, true);
        __android_log_write(ANDROID_LOG_ERROR, PROC, "Loaded");
        __android_log_write(ANDROID_LOG_ERROR, PROC, strSrcFilename);
                
        // do the redaction on this region
        jpeg_redaction::Redaction::Region region(left, right, top, bottom);
        region.SetRedactionMethod(strRedactionCommand);
        jpeg_redaction::Redaction redaction;
        redaction.AddRegion(region);
        __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","added redact region");
        
        jpeg_decoder.DecodeImage(&redaction,NULL);
        __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","redacted");
        
        // get this region's redaction pack
        std::vector<unsigned char> redactionPack;
        redaction.Pack(&redactionPack);
        newPackSize = redactionPack.size();
        
        returnedPack = (jbyteArray) env->NewByteArray(newPackSize);
        jbyte buffer[newPackSize];
        for(int i=0; i<redactionPack.size(); i++) {
            buffer[i] = redactionPack.at(i);
        }
        env->SetByteArrayRegion(returnedPack, 0, newPackSize, buffer);       
        __android_log_write(ANDROID_LOG_DEBUG, TAG, "added one new redaction pack");        
                        
        jpeg_decoder.Save(strDestFilename);
        __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","saved");
        
        (env)->ReleaseStringUTFChars(jstrSrcFilename , strSrcFilename); // release jstring
        (env)->ReleaseStringUTFChars(jstrDestFilename , strDestFilename); // release jstring
        
        
    } catch (const char *error) {
        __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","ERROR");
        __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION", error);
    }
    
    __android_log_write(ANDROID_LOG_ERROR,"JPEGREDACTION","Finished");
    return returnedPack;
}

