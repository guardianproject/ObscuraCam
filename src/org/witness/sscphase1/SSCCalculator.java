package org.witness.sscphase1;

import org.json.JSONArray;
import org.json.JSONObject;

public class SSCCalculator {
	boolean calc;
	final static String SSC = "[Camera Obscura : SSCCalculator] **************************** ";

	
	public SSCCalculator() {
		// it inits but I don't foresee it needing anything...
	}
	
	public int[] getTagDimensions(float[] coords) {
		// this method receives the top-left and bottom right coordinates of a tag
		// and returns their length and width
		int[] dimensions = new int[2];
		dimensions[0] = (int) Math.abs(coords[0] - coords[2]);
		dimensions[1] = (int) Math.abs(coords[1] - coords[3]);
		return dimensions;
	}
	
	public int jsonGetTagId(String json) throws Exception {
		JSONObject j = jsonGetAll(json);
		return j.getInt("id");
	}
	
	public float[] jsonGetTagCoords(String json) throws Exception {
		JSONObject j = jsonGetAll(json);
		float[] coords = null;
		JSONArray jCoords = j.getJSONArray("coords");
		for(int x=0;x<jCoords.length();x++) {
			coords[x] = (float) jCoords.getInt(x);
		}
		return coords;
	}
	
	public String jsonGetTagCoordsAsString(String json) throws Exception {
		String coords = "{";
		JSONObject j = new JSONObject(json);
		JSONArray jCoords = j.getJSONArray("coords");
		for(int x=0;x<jCoords.length();x++) {
			coords += ((float) jCoords.getInt(x) + ",");
		}
		coords = coords.substring(0,coords.length() -1) + "}";
		return coords;
	}
	
	public float[] jsonDeflate(int key, String json) throws Exception {
		JSONObject j = new JSONObject(json);
		float[] response = null;
		switch(key) {
		case 1:
			// coords
			JSONArray jCoords = j.getJSONArray("coords");
			response = new float[4];
			for(int x=0;x<jCoords.length();x++) {
				response[x] = (float) jCoords.getInt(x);
			}
		}
		return response;
	}
	
	public JSONObject jsonGetAll(String json) throws Exception {
		JSONObject j = new JSONObject(json);
		return j;
	}
}
