/*
 * This ObscureMethod pixelizes the region
 */

package org.witness.sscphase1;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Rect;

public class CrowdBlurObscure extends Activity implements ObscureMethod {

	Bitmap originalBmp;
	
	private final static int PIXEL_BLOCK = 10;
	
	public CrowdBlurObscure(Bitmap _originalBmp) {
		originalBmp = _originalBmp;
	}
	
	public void obscureRect(Rect rect, Canvas canvas) {
	
		int pixelSize = 5;
		/*
		int pixelSize = (rect.right-rect.left)/PIXEL_BLOCK;
		
		if (pixelSize <= 0) //1 is the smallest it can be
			pixelSize = 1;
		*/
		
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
		
		//Path path = new Path();
		//path.addCircle(rect.exactCenterX(), rect.exactCenterY(), rect.width()/2, Direction.CW);
		
		int px, py;
		
		for (int x = 0; x < originalBmp.getWidth() - 1; x++) {
			for (int y = 0; y < originalBmp.getHeight() - 1; y++) {
				
				if (rect.contains(x, y))
					continue;
				
				px = (x/pixelSize)*pixelSize;
				py = (y/pixelSize)*pixelSize;

				try
				{ 
					originalBmp.setPixel(x, y, originalBmp.getPixel(px,py));
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




