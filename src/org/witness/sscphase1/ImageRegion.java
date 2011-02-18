package org.witness.sscphase1;

import android.graphics.Rect;

public class ImageRegion {

	float startX;
	float startY;
	float endX;
	float endY;
	
	int imageWidth;
	int imageHeight;
	
		
	public ImageRegion(int _scaledStartX, int _scaledStartY, int _scaledEndX, int _scaledEndY, int _scaledImageWidth, int _scaledImageHeight, int _imageWidth, int _imageHeight) {
		
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
	}
	
	public Rect getScaledRect(int _scaledImageWidth, int _scaledImageHeight) {
		
		float scaledStartX = (float)startX * (float)_scaledImageWidth/(float)imageWidth;
		float scaledStartY = (float)startY * (float)_scaledImageHeight/(float)imageHeight;
		float scaledEndX = (float)endX * (float)_scaledImageWidth/(float)imageWidth;
		float scaledEndY = (float)endY * (float)_scaledImageHeight/(float)imageHeight;

		return new Rect((int)scaledStartX, (int)scaledStartY, (int)scaledEndX, (int)scaledEndY);
	}
}
