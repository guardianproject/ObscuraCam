package org.witness.sscphase1;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

public class BlurObscure extends Activity implements ObscureMethod {

	Bitmap originalBmp;
	
	public BlurObscure(Bitmap _originalBmp) {
		originalBmp = _originalBmp;
	}
	
	public void obscureRect(Rect rect, Canvas canvas) {
		
		for (int x = 1; x < originalBmp.getWidth() - 1; x++) {
			for (int y = 1; y < originalBmp.getHeight() - 1; y++) {
				int r = getRed(x,y);
				int g = getBlue(x,y);
				int b = getBlue(x,y);
				originalBmp.setPixel(x, y, Color.rgb(Color.red(r), Color.green(g), Color.blue(b)));
			}
		}
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

