package org.witness.sscphase1;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class PixelizeObscure extends Activity implements ObscureMethod {

	Bitmap originalBmp;
	
	public PixelizeObscure(Bitmap _originalBmp) {
		originalBmp = _originalBmp;
	}
	
	public void obscureRect(Rect rect, Canvas canvas) {
	
		int pixelSize = (rect.right-rect.left)/10;
		
		// Why does this shift the color to blue??
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
		
		for (int x = rect.left; x < rect.right - 1; x++) {
			for (int y = rect.top; y < rect.bottom - 1; y++) {
				
				px = (x/pixelSize)*pixelSize;
				py = (y/pixelSize)*pixelSize;
				
				originalBmp.setPixel(x, y, originalBmp.getPixel(px,py));

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



