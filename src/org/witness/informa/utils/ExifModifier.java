package org.witness.informa.utils;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.witness.sscphase1.ObscuraApp;

import android.media.ExifInterface;
import android.util.Log;

public class ExifModifier {
	ExifInterface _ei;
	File _jpeg;
		
	public ExifModifier(String jpeg) {
		try {
			_ei = new ExifInterface(jpeg);
			_jpeg = new File(jpeg);
			
		} catch(IOException e) {
			Log.d(ObscuraApp.TAG,"nope: " + e);
		}
	}
	
	public void addHash(String hash) {
		Log.d(ObscuraApp.TAG,"old model tag: " + _ei.getAttribute(ExifInterface.TAG_MODEL));
		_ei.setAttribute(ExifInterface.TAG_MODEL, hash);
		
		
	}
	
	
	public JSONObject getExifTags() {
		return null;
	}
	
	public JSONObject getExifTags(String key) throws JSONException {
		
		JSONObject json = new JSONObject();
		json.put(key, _ei.getAttribute(key));
		return json;
	}
	
	public JSONObject getMakernotes() {
		return null;
	}
	
	public JSONObject getMakernotes(String key) {
		return null;
	}
	
	public boolean zipExifData() {
		try {
			_ei.saveAttributes();
			Log.d(ObscuraApp.TAG,"new model tag: " + _ei.getAttribute(ExifInterface.TAG_MODEL));
			return true;
		} catch(IOException e) {
			Log.d(ObscuraApp.TAG,"exif fail: " + e);
			return false;
		}
	}

	public void addOffset(long length) {
		_ei.setAttribute("InformaCam_SOI", "" + Long.toString(length));
		
	}
}
