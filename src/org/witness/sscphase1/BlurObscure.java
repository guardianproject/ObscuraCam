package org.witness.sscphase1;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class BlurObscure extends Activity implements ObscureMethod {

	Bitmap originalBmp;
	
	public BlurObscure(Bitmap _originalBmp) {
		originalBmp = _originalBmp;
	}
	
	public void obscureRect(Rect rect, Canvas canvas) {
	
		makeItBlue (rect);
		
		//doScaleBlur (rect, canvas);
	}
	
	private void makeItBlue (Rect rect)
	{
		
		for (int x = rect.left; x < rect.right - 1; x++) {
			for (int y = rect.top; y < rect.bottom - 1; y++) {
				
				int r = getRed(x,y);
				int g = getBlue(x,y);
				int b = getBlue(x,y);
				
				originalBmp.setPixel(x, y, Color.rgb(Color.red(r), Color.green(g), Color.blue(b)));
			}
		}
	}
	
	private void doScaleBlur (Rect rect, Canvas canvas)
	{
		  Bitmap facebmp = Bitmap.createBitmap(originalBmp,rect.left,rect.top,rect.width(),rect.height());

	        int blurFactor = 10;
	        
	        facebmp = Bitmap.createScaledBitmap(facebmp, facebmp.getWidth()/blurFactor, facebmp.getHeight()/blurFactor, true);
	        facebmp = Bitmap.createScaledBitmap(facebmp, facebmp.getWidth()*blurFactor, facebmp.getHeight()*blurFactor, true);

	    	Paint obscuredPaint = new Paint();     	

	        canvas.drawBitmap(facebmp, rect, rect, obscuredPaint);
	}
	
	private int getRed(int x, int y) {
		return 
			(Color.red(originalBmp.getPixel(x-1, y-1)) +
			Color.red(originalBmp.getPixel(x, y-1)) +
			Color.red(originalBmp.getPixel(x+1, y-1)) +
			Color.red(originalBmp.getPixel(x-1, y)) +
			Color.red(originalBmp.getPixel(x, y)) +
			Color.red(originalBmp.getPixel(x+1, y)) +
			Color.red(originalBmp.getPixel(x-1, y+1)) +
			Color.red(originalBmp.getPixel(x, y+1)) +
			Color.red(originalBmp.getPixel(x+1, y+1)))/9;
	}
	
	private int getGreen(int x, int y) {
		return 
			(Color.green(originalBmp.getPixel(x-1, y-1)) +
			Color.green(originalBmp.getPixel(x, y-1)) +
			Color.green(originalBmp.getPixel(x+1, y-1)) +
			Color.green(originalBmp.getPixel(x-1, y)) +
			Color.green(originalBmp.getPixel(x, y)) +
			Color.green(originalBmp.getPixel(x+1, y)) +
			Color.green(originalBmp.getPixel(x-1, y+1)) +
			Color.green(originalBmp.getPixel(x, y+1)) +
			Color.green(originalBmp.getPixel(x+1, y+1)))/9;		
	}
	
	private int getBlue(int x, int y) {
		return 
			(Color.blue(originalBmp.getPixel(x-1, y-1)) +
			Color.blue(originalBmp.getPixel(x, y-1)) +
			Color.blue(originalBmp.getPixel(x+1, y-1)) +
			Color.blue(originalBmp.getPixel(x-1, y)) +
			Color.blue(originalBmp.getPixel(x, y)) +
			Color.blue(originalBmp.getPixel(x+1, y)) +
			Color.blue(originalBmp.getPixel(x-1, y+1)) +
			Color.blue(originalBmp.getPixel(x, y+1)) +
			Color.blue(originalBmp.getPixel(x+1, y+1)))/9;		
	}
}



