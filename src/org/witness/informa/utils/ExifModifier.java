package org.witness.informa.utils;

import java.io.File;
import java.io.IOException;

import org.json.JSONObject;
import org.witness.sscphase1.ObscuraApp;

import android.media.ExifInterface;
import android.util.Log;

public class ExifModifier {
	ExifInterface _ei;
	File _jpeg;
	
	
	public void ExifModifier(String jpeg) {
		try {
			_ei = new ExifInterface(jpeg);
			_jpeg = new File(jpeg);
			
			// get all the exif tags of jpeg and hold them in memory as a JSON object
			
		} catch(IOException e) {
			Log.d(ObscuraApp.TAG,"nope: " + e);
		}
	}
	
	public void addMakernotes() {
		
	}
	
	public JSONObject getExifTags() {
		return null;
	}
	
	public JSONObject getExifTags(String key) {
		return null;
	}
	
	public JSONObject getMakernotes() {
		return null;
	}
	
	public JSONObject getMakernotes(String key) {
		return null;
	}
	
	public boolean zipExifData() {
		return false;
	}
}
