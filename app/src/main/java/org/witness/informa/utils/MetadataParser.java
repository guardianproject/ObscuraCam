package org.witness.informa.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.witness.sscphase1.ObscuraApp;
import org.witness.sscphase1.R;

import android.content.Context;
import android.util.Log;

public class MetadataParser {
	ArrayList<ImageRegion> _imageRegions;
	File _image;
	Map<String, Object> _geneaology, _intent;
	ImageDescription imageDescription;
	protected static Context _c;
	
	String[] geneaologyKeys = {
			"localMediaPath",
			"origin",
			"dateAcquired",
			"processedBy"
	};
	
	String[] intentKeys = {
			"ownership",
			"ownershipType"
	};

	public MetadataParser(String date, File image, Context c) {
		_imageRegions = new ArrayList<ImageRegion>();
		_image = image;
		_c = c;
		
		_geneaology = new HashMap<String, Object>();
		_geneaology.put("dateAcquired", date);
		
		_intent = new HashMap<String, Object>();
	}
	
	public void addRegion(Properties regionData) {
		_imageRegions.add(new ImageRegion(regionData));
		
	}
	
	public ImageDescription flushMetadata() {
		ImageDescription imageDescription = new ImageDescription(_geneaology, _intent, _imageRegions);
		addInformaExifData(imageDescription.getHumanReadableValue());
		return imageDescription;
	}
	
	public void addInformaExifData(String exifData) {
		ExifModifier em = new ExifModifier(_image.getPath());
		em.addMakernotes(exifData);
		em.zipExifData();
	}
	
	public static class ImageDescription {
		String humanReadable;
		JSONObject machineReadable;
		
		public ImageDescription(Map<String, Object> geneaology, Map<String, Object> intent, ArrayList<ImageRegion> imageRegions) {
			humanReadable = "undefined data";
			try {
				machineReadable = new JSONObject();
				
				for(Map.Entry<String, Object> entry : geneaology.entrySet())
					machineReadable.put(entry.getKey(), entry.getValue());
				
				for(Map.Entry<String, Object> entry : intent.entrySet())
					machineReadable.put(entry.getKey(), entry.getValue());
						
				JSONArray allRegions = new JSONArray();
				for(ImageRegion i : imageRegions) {
					JSONObject ir = new JSONObject();
					for(Map.Entry<String, Object> entry : i._data.entrySet())
						ir.put(entry.getKey(), entry.getValue());
					
					for(Map.Entry<String, Object> entry : i._consent.entrySet())
						ir.put(entry.getKey(), entry.getValue());
							
					allRegions.put(ir);
				}
				
				machineReadable.put("imageRegions", allRegions);
				
				
				StringBuffer sb = new StringBuffer();
				sb.append(
						_c.getString(R.string.mdHumanReadable_intro) + 
						" (" + _c.getString(R.string.mdHumanReadable_onDate) + " " + machineReadable.getString("dateAcquired") + ".) " +
						_c.getString(R.string.mdHumanReadable_numTags) + " " +  ((JSONArray) machineReadable.get("imageRegions")).length() + " " +
						_c.getString(R.string.mdHumanReadable_consentIntro)
				);
				
				JSONArray ir = (JSONArray) machineReadable.getJSONArray("imageRegions");
				int subjectCount = 0;
				for(int i = 0; i < ir.length(); i++) {
					JSONObject region = ir.getJSONObject(i);
					if(region.has("regionSubject")) {
						sb.append(
								(subjectCount + 1) + ". " + _c.getString(R.string.mdHumanReadable_psudonym) + " " + 
								region.getString("regionSubject") +
								" (" + _c.getString(R.string.mdHumanReadable_coordinates) + " " + region.getString("initialCoordinates") + ".) " +
								_c.getString(R.string.mdHumanReadable_consentGiven) + " " + region.getString("informedConsent") + ". "
								//_c.getString(R.string.mdHumanReadable_persistObfuscation) + " " + region.getString("persistObscureType") + "."
						);
					}
					subjectCount++;
				}
				
				if(subjectCount == 0) {
					sb.append("No subjects identified in this photo!");
				}
				
				
				
				humanReadable = sb.toString();
			} catch(JSONException e) {
				Log.d(ObscuraApp.TAG,"Fuck you, harlo:  " + e);
			}
		}
		
		public String getHumanReadableValue() {
			return humanReadable;
		}
		
		public JSONObject getMachineReadableValue() {
			return machineReadable;
		}
		
	}
	
	public static class ImageRegion {
		Map<String,Object> _consent, _data;
		
		String[] consentKeys = {
				"regionSubject",
				"informedConsent",
				"persistObscureType"
		};
		
		String[] dataKeys = {
				"obfuscationType",
				"initialCoordinates",
				"regionWidth",
				"regionHeight"
		};
		
		public ImageRegion(Properties regionData) {
			_consent = new HashMap<String, Object>();
			_data = new HashMap<String, Object>();
			
			for(String key : dataKeys) {
				if(regionData.containsKey(key))
					_data.put(key, regionData.get(key));
			}
			
			for(String key : consentKeys) {
				if(regionData.containsKey(key))
					_consent.put(key, regionData.get(key));
			}
		}
	}
	
		
}
