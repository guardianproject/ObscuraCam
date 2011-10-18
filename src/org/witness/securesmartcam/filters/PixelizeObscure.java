/*
 * This ObscureMethod pixelizes the region
 */

package org.witness.securesmartcam.filters;


import java.util.Properties;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

public class PixelizeObscure implements RegionProcesser {

	Bitmap originalBmp;
	
	private final static int PIXEL_BLOCK = 10;
	
	Properties mProps;
	
	public PixelizeObscure ()
	{
		mProps = new Properties ();
		mProps.put("size", "10");		
	}
	
	public void processRegion(RectF rect, Canvas canvas, Bitmap bitmap) {
	
		originalBmp = bitmap;
		
		int pixelSize = (int)(rect.right-rect.left)/PIXEL_BLOCK;
		
		if (pixelSize <= 0) //1 is the smallest it can be
			pixelSize = 1;
		
		pixelate(rect, pixelSize);
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
		return null;
	}
	
	public void setProperties(Properties props)
	{
		
	}
}



