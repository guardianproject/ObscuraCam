/*
 * This ObscureMethod blurs the contents of the region
 */

package org.witness.sscphase1;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class BlurObscure extends Activity implements ObscureMethod {

	Bitmap originalBmp;

	private final static int BLUR_OFFSET = 10;
	
	public BlurObscure(Bitmap _originalBmp) {
		originalBmp = _originalBmp;
	}
	
	public void obscureRect(Rect rect, Canvas canvas) {
		makeItBlur(rect);
	}
	
	private void makeItBlur(Rect rect)
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
	}
}



