/*
 * This ObscureMethod pixelizes the region
 */

package org.witness.securesmartcam.filters;

import java.util.Date;
import java.util.Properties;

import org.witness.sscphase1.ObscuraApp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;

public class PixelizeObscure implements RegionProcesser {

	Bitmap originalBmp, unredactedBmp;
	boolean unredactedBmpSet;
	
	private final static int PIXEL_BLOCK = 10;
	
	Properties mProps;
	
	public PixelizeObscure ()
	{
		mProps = new Properties ();
		mProps.put("size", "10");
		mProps.put("obfuscationType", this.getClass().getName());
		
		mProps.put("timestampOnGeneration", new Date().getTime());
		unredactedBmpSet = false;
	}
	
	public void processRegion(RectF rect, Canvas canvas, Bitmap bitmap) {
	
		originalBmp = bitmap;
		
		if(!unredactedBmpSet) {
			unredactedBmp = Bitmap.createBitmap(
					bitmap, 
					(int) rect.left, 
					(int) rect.top,
					(int) Math.min(bitmap.getWidth(),(Math.abs(rect.left - rect.right))), 
					(int) Math.min(bitmap.getHeight(), (Math.abs(rect.top - rect.bottom)))
				);
			
			unredactedBmpSet = true;
			Log.d(ObscuraApp.TAG, "this is where the bitmap is set.");
		} else
			Log.d(ObscuraApp.TAG, "nope, original bmp already set.");
		
		int pixelSize = (int)(rect.right-rect.left)/PIXEL_BLOCK;
		
		if (pixelSize <= 0) //1 is the smallest it can be
			pixelSize = 1;
		
		pixelate(rect, pixelSize);
		
		// return properties and data as a map
		mProps.put("initialCoordinates", "[" + rect.top + "," + rect.left + "]");
		mProps.put("regionWidth", Float.toString(Math.abs(rect.left - rect.right)));
		mProps.put("regionHeight", Float.toString(Math.abs(rect.top - rect.bottom)));
		
	}
	
	private void pixelate(RectF rect, int pixelSize)
	{
		
		if (rect.left <= 0) {
			rect.left = 0;
		} else if (rect.right >= originalBmp.getWidth()-1) {
			rect.right = originalBmp.getWidth() - 1;
		}
		
		if (rect.top <= 0) {
			rect.top = 0;
		} else if (rect.bottom >= originalBmp.getHeight()) {
			rect.bottom = originalBmp.getHeight() - 1;
		}
		
		
		int pixels[];
		int newPixel;
		int wPixelSize = pixelSize;
		int hPixelSize = pixelSize;
		
		for (int x = (int)rect.left; x < rect.right; x+=pixelSize) {
			for (int y = (int)rect.top; y < rect.bottom; y+=pixelSize) {

				wPixelSize = pixelSize;
				hPixelSize = pixelSize;
				
				try
				{ 
					pixels = new int[pixelSize*pixelSize];
					newPixel = originalBmp.getPixel(x, y);
					for (int i = 0; i < pixels.length; i++)
						pixels[i] = newPixel;
					
					if (x+pixelSize>rect.right)
					{
						wPixelSize = (int)rect.right - x;
					}
					
					if (y+pixelSize> rect.bottom)
					{
						hPixelSize = (int)rect.bottom - y;
					}
					
					originalBmp.setPixels(pixels, 0, pixelSize, x, y, wPixelSize, hPixelSize);
				}
				catch (IllegalArgumentException iae)
				{
					iae.printStackTrace();
				}
			}
		}
	}
	
	public Properties getProperties()
	{
		return mProps;
	}
	
	public void setProperties(Properties props)
	{
		mProps = props;
	}
	
	public void updateBitmap() {
		unredactedBmpSet = false;
	}

	@Override
	public Bitmap getBitmap() {
		return unredactedBmp;
	}
}



