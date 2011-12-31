package org.witness.informa.utils;

import org.json.JSONObject;

public class JpegParser {
	
	private native void generateNewJpeg(String filename, String metadata, String newFilename, int metadatadLength);
	
	static {
		System.loadLibrary("JpegRedaction");
	}
	
	public JpegParser(String filename, JSONObject metadata) {
		String newFilename = filename.split(".jpg")[0] + "_informa.jpg";
		generateNewJpeg(filename, metadata.toString(), newFilename, metadata.toString().length());
		
	}
}
