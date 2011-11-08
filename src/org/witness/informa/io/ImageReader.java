package org.witness.informa.io;

import java.io.File;
import java.util.ArrayList;

import org.json.JSONObject;

public class ImageReader {
	boolean _isValid = false;
	ArrayList<JSONObject> _imageRegions;
	String _globalParams;
	
	public ImageReader(File image) {
		// TODO: extract watermark
		
		// TODO: decode it
		
		// TODO: check it against exif model tag
		
		/*
		 *  TODO: if it's a match, unpack the data at the end of the jpeg
		 *  
		 *  _globalParams = whatever the intent & geneaology data contains
		 *  _imageRegions = whatever image regions encoded therein
		 */
		
	}
	
	public String getParams() {
		return _globalParams;
	}
	
	public ArrayList<JSONObject> getImageRegions() {
		return _imageRegions;
	}
	
	public boolean isValid() {
		return _isValid;
	}
}
