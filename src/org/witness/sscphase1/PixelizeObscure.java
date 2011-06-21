/*
 * This ObscureMethod pixelizes the region
 */

package org.witness.sscphase1;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public class PixelizeObscure extends Activity implements ObscureMethod {

	Bitmap originalBmp;
	
	private final static int PIXEL_BLOCK = 10;
	
	public PixelizeObscure(Bitmap _originalBmp) {
		originalBmp = _originalBmp;
	}
	
	public void obscureRect(Rect rect, Canvas canvas) {
	
		int pixelSize = (rect.right-rect.left)/PIXEL_BLOCK;
		
		if (pixelSize <= 0) //1 is the smallest it can be
			pixelSize = 1;
		
		pixelate(rect, pixelSize);
	}
	
	private void pixelate(Rect rect, int pixelSize)
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
		
		for (int x = rect.left; x < rect.right - 1; x+=pixelSize) {
			for (int y = rect.top; y < rect.bottom - 1; y+=pixelSize) {
				
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
	
}



