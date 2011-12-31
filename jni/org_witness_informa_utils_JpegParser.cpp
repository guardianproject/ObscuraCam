#include <stdlib.h>
#include <stdio.h>
#include <string>
#include <vector>
#include <android/log.h>

#include "jpeg.h"
#include "redaction.h"
#include "bit_shifts.h"
#include "byte_swapping.h"
#include "debug_flag.h"
#include "iptc.h"
#include "jpeg_decoder.h"
#include "jpeg_dht.h"
#include "jpeg_marker.h"
#include "makernote.h"
#include "obscura_metadata.h"
#include "photoshop_3block.h"
#include "tiff_ifd.h"
#include "tiff_tag.h"

#include <jni.h>
#include "org_witness_informa_utils_JpegParser.h"

JNIEXPORT void JNICALL
Java_org_witness_informa_utils_JpegParser_generateNewJpeg(JNIEnv *env, jobject obj, jstring jstrSrcFilename, jstring jstrMetadata, jstring jstrNewFilename, jint jintMetadataLength) {
	__android_log_write(ANDROID_LOG_VERBOSE, "JPEG_PARSER", "Generating new jpeg");
	try {
		jpeg_redaction::Jpeg newJpeg, doubleCheck;
		const char* strSrcFilename;
		const char* mdChar;
		std::string strNewFilename;

		strSrcFilename = (env)->GetStringUTFChars(jstrSrcFilename, NULL);
		mdChar = (env)->GetStringUTFChars(jstrMetadata, NULL);
		strNewFilename = (env)->GetStringUTFChars(jstrNewFilename, NULL);
		
		newJpeg.LoadFromFile(strSrcFilename, true);
		__android_log_write(ANDROID_LOG_VERBOSE, "JPEG_PARSER", "loaded up jpeg");
		__android_log_write(ANDROID_LOG_VERBOSE, "JPEG_PARSER", strSrcFilename);
		__android_log_write(ANDROID_LOG_VERBOSE, "JPEG_PARSER", "have metadata: ");
		__android_log_write(ANDROID_LOG_VERBOSE, "JPEG_PARSER", mdChar);
		
		// declare vector
		std::vector<unsigned char> mdVector;
		
		// resize it to length of mdChar
		mdVector.resize(jintMetadataLength);
		
		// for each char in mdChar, copy to mVector...
		for(int i=0; i< mdVector.size(); i++) {
			mdVector[i] = mdChar[i];
		}
		
		
		// save as metadata
		newJpeg.SetObscuraMetaData(mdVector.size(), &mdVector.front());
		newJpeg.Save(strNewFilename.c_str());
		
		// double-check
		__android_log_write(ANDROID_LOG_DEBUG, "JPEG_PARSER", "double check:");
		bool success = doubleCheck.LoadFromFile(strNewFilename.c_str(), true);
		if(!success) {
			__android_log_write(ANDROID_LOG_ERROR, "JPEG_PARSER", "doublecheck failed.");
			exit(1);
		}
		
		
		
		
		
	} catch (const char *error) {
		__android_log_write(ANDROID_LOG_ERROR, "JPEG_PARSER", "ERROR HERE.");
	}
}

