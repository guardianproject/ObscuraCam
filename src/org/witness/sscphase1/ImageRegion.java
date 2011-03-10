package org.witness.sscphase1;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.widget.Button;

public class ImageRegion extends Button {

	float startX;
	float startY;
	float endX;
	float endY;
	
	int imageWidth;
	int imageHeight;
	
	int index;
	
	public static final String SSC = "[Camera Obscura : ImageRegion] **************************** ";
			
	public ImageRegion(
			Context context, 
			int _scaledStartX, int _scaledStartY, 
			int _scaledEndX, int _scaledEndY, 
			int _scaledImageWidth, int _scaledImageHeight, 
			int _imageWidth, int _imageHeight, 
			int _backgroundColor,
			int _index)
	{
		super(context);
		
		/*
		original 300
		current 100
		scaled x 20
		real x 60
		original/current * scaled = real
		
		scaled = real * current/original
		*/
		
		startX = (float)_imageWidth/(float)_scaledImageWidth * (float)_scaledStartX;
		startY = (float)_imageHeight/(float)_scaledImageHeight * (float)_scaledStartY;
		endX = (float)_imageWidth/(float)_scaledImageWidth * (float)_scaledEndX;
		endY = (float)_imageHeight/(float)_scaledImageHeight * (float)_scaledEndY;
				
		imageWidth = _imageWidth;
		imageHeight = _imageHeight;
		index = _index;
		
		setBackgroundColor(_backgroundColor);
	}
	
	public Rect getScaledRect(int _scaledImageWidth, int _scaledImageHeight) {
		
		float scaledStartX = (float)startX * (float)_scaledImageWidth/(float)imageWidth;
		float scaledStartY = (float)startY * (float)_scaledImageHeight/(float)imageHeight;
		float scaledEndX = (float)endX * (float)_scaledImageWidth/(float)imageWidth;
		float scaledEndY = (float)endY * (float)_scaledImageHeight/(float)imageHeight;

		return new Rect((int)scaledStartX, (int)scaledStartY, (int)scaledEndX, (int)scaledEndY);
	}
	
	public String attachTags() {
	   	/*
    	 * this method adds the returned coordinates to our array of ROIs
    	 * and creates a JSON String for identifying it permanently
    	 */
		float[] tagCoords = {startX,startY,endX,endY};
    	String newTagCoordsDescription = "{\"id\":" + index + ",\"coords\":[";
    	for(int x=0;x<4;x++) {
    		newTagCoordsDescription += Float.toString(tagCoords[x]) + ",";
    	}
    	newTagCoordsDescription = newTagCoordsDescription.substring(0,newTagCoordsDescription.length() - 1) + "]}";
    	return newTagCoordsDescription;
	}
}
