/*
 * This ObscureMethod pixelizes the region
 */

package org.witness.securesmartcam.filters;


import java.util.Properties;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

public class CrowdPixelizeObscure implements RegionProcesser {

	Bitmap originalBmp;
	
	public static int PIXEL_BLOCK = 50;
	
	public void processRegion(RectF rect, Canvas canvas,  Bitmap bitmap) {
	
		originalBmp = bitmap;
		
		int pixelSize = originalBmp.getWidth()/PIXEL_BLOCK;
		
		if (pixelSize <= 0) //1 is the smallest it can be
			pixelSize = 1;
	
		pixelate(rect, pixelSize);
	}
	
	private void pixelate(RectF rect, int pixelSize)
	{
		if (rect.left <= 0) {
			rect.left = 1;
		} else if (rect.right >= originalBmp.getWidth()-1) {
			rect.right = originalBmp.getWidth() - 1;
		}
		
		if (rect.top <= 0) {
			rect.top = 1;
		} else if (rect.bottom >= originalBmp.getHeight()) {
			rect.bottom = originalBmp.getHeight();
		}
		
		int px, py;
		
		for (int x = 0; x < originalBmp.getWidth() - 1; x+=pixelSize) {
			for (int y = 0; y < originalBmp.getHeight() - 1; y+=pixelSize) {
				
				if (rect.contains(x, y))
					continue;
				
				px = (x/pixelSize)*pixelSize;
				py = (y/pixelSize)*pixelSize;

				try
				{ 
					//originalBmp.setPixel(x, y, originalBmp.getPixel(px,py));
					
					int pixels[] = new int[pixelSize*pixelSize];
					int newPixel = originalBmp.getPixel(px, py);
					for (int i = 0; i < pixels.length; i++)
						pixels[i] = newPixel;
					
					originalBmp.setPixels(pixels, 0, pixelSize, px, py, pixelSize, pixelSize);
				}
				catch (IllegalArgumentException iae)
				{
					//something is wrong with our pixel math
					break; //stop the filter
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




