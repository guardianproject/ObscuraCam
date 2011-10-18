/*
 * This ObscureMethod blurs the contents of the region
 */

package org.witness.securesmartcam.filters;


import java.util.Properties;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class BlurObscure implements RegionProcesser {

	Bitmap originalBmp;

	private final static int BLUR_OFFSET = 10;
	
	public void processRegion(RectF rect, Canvas canvas,  Bitmap bitmap) {
		//makeItBlur(rect);
		
		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		paint.setAlpha(100);
		canvas.drawCircle(rect.centerX(), rect.centerY(), rect.width()/2, paint);
		
	}
	
	/*
	private void makeItBlur(Rect rect)
	{
		
		if (rect.left <= (0 + BLUR_OFFSET*2)) {
			rect.left = (1 + BLUR_OFFSET*2);
		} else if (rect.right >= (originalBmp.getWidth()-1-BLUR_OFFSET*2)) {
			rect.right = (originalBmp.getWidth() - 2 - BLUR_OFFSET*2);
		}
		
		if (rect.top <= (0 + BLUR_OFFSET*2)) {
			rect.top = (1 + BLUR_OFFSET*2);
		} else if (rect.bottom >= (originalBmp.getHeight() - BLUR_OFFSET*2)) {
			rect.bottom = (originalBmp.getHeight() - 2 - BLUR_OFFSET*2);
		}
			
		for (int x = rect.left; x < rect.right - 1; x++) {
			for (int y = rect.top; y < rect.bottom - 1; y++) {
				
				int blurOffset = (int)((Math.random()*BLUR_OFFSET)+BLUR_OFFSET);
				int r = getRed(x,y, blurOffset);
				
				blurOffset = (int)((Math.random()*BLUR_OFFSET)+BLUR_OFFSET);
				int g = getGreen(x,y, blurOffset);
				
				blurOffset = (int)((Math.random()*BLUR_OFFSET)+BLUR_OFFSET);
				int b = getBlue(x,y, blurOffset);
				
				originalBmp.setPixel(x, y, Color.rgb(r,g,b));
			}
		}
	}
	
	
	private int getRed(int x, int y, int blurOffset) {
		return 
			(Color.red(originalBmp.getPixel(x-blurOffset, y-blurOffset)) +
			Color.red(originalBmp.getPixel(x, y-blurOffset)) +
			Color.red(originalBmp.getPixel(x+blurOffset, y-blurOffset)) +
			Color.red(originalBmp.getPixel(x-blurOffset, y)) +
			Color.red(originalBmp.getPixel(x, y)) +
			Color.red(originalBmp.getPixel(x+blurOffset, y)) +
			Color.red(originalBmp.getPixel(x-blurOffset, y+blurOffset)) +
			Color.red(originalBmp.getPixel(x, y+blurOffset)) +
			Color.red(originalBmp.getPixel(x+blurOffset, y+blurOffset)))/9;
	}
	
	private int getGreen(int x, int y, int blurOffset) {
		return 
			(Color.green(originalBmp.getPixel(x-blurOffset, y-blurOffset)) +
			Color.green(originalBmp.getPixel(x, y-blurOffset)) +
			Color.green(originalBmp.getPixel(x+blurOffset, y-blurOffset)) +
			Color.green(originalBmp.getPixel(x-blurOffset, y)) +
			Color.green(originalBmp.getPixel(x, y)) +
			Color.green(originalBmp.getPixel(x+blurOffset, y)) +
			Color.green(originalBmp.getPixel(x-blurOffset, y+blurOffset)) +
			Color.green(originalBmp.getPixel(x, y+blurOffset)) +
			Color.green(originalBmp.getPixel(x+blurOffset, y+blurOffset)))/9;		
	}
	
	private int getBlue(int x, int y, int blurOffset) {
		return 
			(Color.blue(originalBmp.getPixel(x-blurOffset, y-blurOffset)) +
			Color.blue(originalBmp.getPixel(x, y-blurOffset)) +
			Color.blue(originalBmp.getPixel(x+blurOffset, y-blurOffset)) +
			Color.blue(originalBmp.getPixel(x-blurOffset, y)) +
			Color.blue(originalBmp.getPixel(x, y)) +
			Color.blue(originalBmp.getPixel(x+blurOffset, y)) +
			Color.blue(originalBmp.getPixel(x-blurOffset, y+blurOffset)) +
			Color.blue(originalBmp.getPixel(x, y+blurOffset)) +
			Color.blue(originalBmp.getPixel(x+blurOffset, y+blurOffset)))/9;		
	}*/
	
	public Properties getProperties()
	{
		return null;
	}
	
	public void setProperties(Properties props)
	{
		
	}
	
}



