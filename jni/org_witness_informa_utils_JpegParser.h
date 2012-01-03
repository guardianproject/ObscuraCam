#include <jni.h>

#ifndef _Included_org_witness_informa_utils_JpegParser
#define _Included_org_witness_informa_utils_JpegParser
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:	org_witness_informa_utils_JpegParser
 * Method:	parseit
 * Signature:	()V
 */
JNIEXPORT int JNICALL Java_org_witness_informa_utils_JpegParser_generateNewJpeg
	(JNIEnv *, jobject, jstring, jstring, jstring, jint);


#ifdef __cplusplus
}
#endif
#endif