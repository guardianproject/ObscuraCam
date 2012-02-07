//
//  org_witness_informa_utils_ImageConstructor.h
//  
//
//  Created by Harlo Holmes on 2/7/12.
//
#include <jni.h>

#ifndef _Included_org_witness_informa_utils_ImageConstructor
#define _Included_org_witness_informa_utils_ImageConstructor
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:       org_witness_informa_utils_ImageConstructor
 * Method:      constructImage
 * Signature:   ()V
 */
JNIEXPORT jstring JNICALL
    Java_org_witness_informa_utils_ImageConstructor_constructImage
    (JNIEnv *, jobject, jstring, jstring, int);
    
#ifdef __cplusplus
}
#endif
#endif
