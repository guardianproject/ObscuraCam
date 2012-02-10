package org.witness.informa.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.witness.informa.utils.InformaConstants.Keys;
import org.witness.informa.utils.secure.Apg;

import android.os.Handler;
import android.util.Log;

public class ImageConstructor {
	private native int constructImage(
			String originalImageFilename, 
			String informaImageFilename, 
			String metadataObjectString, 
			int metadataLength);
	
	JSONArray imageRegions;
	Apg apg;
	
	static {
		System.loadLibrary("JpegRedaction");
	}
	
	
	public ImageConstructor(final String informaImageFilename, String metadataObjectString) throws JSONException {
		// tokenize metadata
		JSONObject metadataObject;
		metadataObject = (JSONObject) new JSONTokener(metadataObjectString).nextValue();
		
		// get who it's intended for
		
		// set metadata via jpegredaction lib
		constructImage(InformaConstants.DUMP_FOLDER + "itmp.jpg", informaImageFilename, metadataObjectString, metadataObjectString.length());
		
		// encrypt
		
		// delete unencrypted
		
	}
	
}
