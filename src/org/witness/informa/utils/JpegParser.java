package org.witness.informa.utils;

import org.json.JSONObject;

public class JpegParser {
	
	private native int generateNewJpeg(String filename, String metadata, String newFilename, int metadatadLength);
	
	String informaImage;
	int success = 1;
	JSONObject _metadata;
	
	static {
		System.loadLibrary("JpegRedaction");
	}
	
	public JpegParser(String filename, JSONObject metadata) {
		_metadata = metadata;
		informaImage = filename.split(".jpg")[0] + "_informa.jpg";
		success = generateNewJpeg(filename, _metadata.toString(), informaImage, _metadata.toString().length());
		
	}
	
	public String getInformaFileName() {
		if(success != 1)
			return informaImage;
		else
			return null;
	}
	
	public JSONObject getInformaMetadata() {
		return _metadata;
	}
}
