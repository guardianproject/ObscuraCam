package org.witness.informa.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.witness.informa.utils.InformaConstants.Keys;
import org.witness.securesmartcam.filters.CrowdPixelizeObscure;
import org.witness.securesmartcam.filters.InformaTagger;
import org.witness.securesmartcam.filters.PixelizeObscure;
import org.witness.securesmartcam.filters.SolidObscure;
import org.witness.securesmartcam.utils.ObscuraConstants.Filters;

import android.os.Handler;
import android.util.Log;

public class ImageConstructor {
	private native int constructImage(
			String originalImageFilename, 
			String informaImageFilename, 
			String metadataObjectString, 
			int metadataLength);
	private native char[] setRegion(
			String originalImageFilename, 
			String informaImageFilename, 
			int left, 
			int right, 
			int top, 
			int bottom, 
			String redactionMethod, 
			char[] resultBuffer);
	
	JSONArray imageRegions;
	
	static {
		System.loadLibrary("JpegRedaction");
	}
	
	public ImageConstructor(final String originalImageFilename, final String informaImageFilename, String metadataObjectString) {
		JSONObject metadataObject;
		Handler h;
		
		try {
			// 1. tokenize metadata
			metadataObject = (JSONObject) new JSONTokener(metadataObjectString).nextValue();
			imageRegions = (JSONArray) 
					((JSONObject) metadataObject.getJSONObject(Keys.Informa.DATA))
					.getJSONArray(Keys.Data.IMAGE_REGIONS);
			for(int i=0;i<imageRegions.length();i++) {
				final int ir = i;
				Runnable r = new Runnable() {
					@Override
					public void run() {
						Log.d(InformaConstants.TAG, "getting " + ir);
						try {
							JSONObject imageRegion = imageRegions.getJSONObject(ir);
							String obfuscationClass = imageRegion.getString(Keys.ImageRegion.FILTER);
							float regionHeight = 
									((JSONObject) imageRegion.getJSONObject(Keys.ImageRegion.DIMENSIONS)).getInt(Keys.ImageRegion.HEIGHT);
							float regionWidth =
									((JSONObject) imageRegion.getJSONObject(Keys.ImageRegion.DIMENSIONS)).getInt(Keys.ImageRegion.WIDTH);
							float regionTop =
									((JSONObject) imageRegion.getJSONObject(Keys.ImageRegion.COORDINATES)).getInt(Keys.ImageRegion.TOP);
							float regionLeft =
									((JSONObject) imageRegion.getJSONObject(Keys.ImageRegion.COORDINATES)).getInt(Keys.ImageRegion.LEFT);
							
							Log.d(InformaConstants.TAG, "setting: " + regionLeft + "," + (regionLeft + regionWidth) + "," + regionTop + "," + (regionTop + regionHeight));
							
							String redactionMethod = "";
							if(PixelizeObscure.class.getName().equals(obfuscationClass))
								redactionMethod = Filters.PIXELIZE;
							else if(CrowdPixelizeObscure.class.getName().equals(obfuscationClass))
								redactionMethod = Filters.CROWD_PIXELIZE;
							else if(SolidObscure.class.getName().equals(obfuscationClass))
								redactionMethod = Filters.SOLID;
							else if(InformaTagger.class.getName().equals(obfuscationClass))
								redactionMethod = Filters.INFORMA_TAGGER;
							
							char[] unredactedRegion = new char[]{};
							Log.d(InformaConstants.TAG, "char was len " + unredactedRegion.length);
							setRegion(
									originalImageFilename, 
									informaImageFilename, 
									(int) regionLeft, 
									(int) (regionLeft + regionWidth), 
									(int) regionTop, 
									(int) (regionTop + regionHeight),
									redactionMethod, 
									unredactedRegion);
							
							// 3. set chars into metadata block
							Log.d(InformaConstants.TAG, "char is now len " + unredactedRegion.length);
						} catch(JSONException e) {
							Log.d(InformaConstants.TAG, "fuck you: " + e);
					
						} 
					}
					
				};
				new Thread(r).start();
			}
				// 2. for each image region, run redaction method
			
			Log.d(InformaConstants.TAG, "md from Java: " + imageRegions.toString());
		} catch(JSONException e) {
			Log.d(InformaConstants.TAG, "fuck you: " + e);
		}
		
		
		
		// $. set metadata
		
		//int x = constructImage(originalImageFilename, informaImageFilename, metadataObjectString, metadataObjectString.length());
		//Log.d(InformaConstants.TAG, "mdLength from JNI: " + x);
		
		
	}
	
}
