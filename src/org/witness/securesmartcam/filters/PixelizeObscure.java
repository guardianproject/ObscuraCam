/*
 * This ObscureMethod pixelizes the region
 */

package org.witness.securesmartcam.filters;


import java.util.Properties;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public class PixelizeObscure implements RegionProcesser {

	Bitmap originalBmp;
	
	private final static int PIXEL_BLOCK = 10;
	
	
	public void processRegion(Rect rect, Canvas canvas, Bitmap bitmap) {
	
		originalBmp = bitmap;
		
		int pixelSize = (rect.right-rect.left)/PIXEL_BLOCK;
		
		if (pixelSize <= 0) //1 is the smallest it can be
			pixelSize = 1;
		
		pixelate(rect, pixelSize);
	}
	
	private void pixelate(Rect rect, int pixelSize)
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
		
		for (int x = rect.left; x < rect.right; x+=pixelSize) {
			for (int y = rect.top; y < rect.bottom; y+=pixelSize) {

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
						wPixelSize = rect.right - x;
					}
					
					if (y+pixelSize> rect.bottom)
					{
						hPixelSize = rect.bottom - y;
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



