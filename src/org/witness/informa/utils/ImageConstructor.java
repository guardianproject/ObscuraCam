package org.witness.informa.utils;

import android.util.Log;

public class ImageConstructor {
	private native int constructImage(String originalImageFilename, String informaImageFilename, String metadataObjectString, int metadataLength);
	
	static {
		System.loadLibrary("JpegRedaction");
	}
	
	public ImageConstructor(String originalImageFilename, String informaImageFilename, String metadataObjectString) {
		Log.d(InformaConstants.TAG, "md from Java: " + metadataObjectString);
		int x = constructImage(originalImageFilename, informaImageFilename, metadataObjectString, metadataObjectString.length());
		Log.d(InformaConstants.TAG, "mdLength from JNI: " + x);
	}
	
}
