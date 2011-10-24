package org.witness.securesmartcam.filters;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

public class TintObscure implements RegionProcesser {

	Bitmap originalBmp;
	Properties mProps;
	
	public TintObscure(Bitmap _originalBmp) {
		originalBmp = _originalBmp;
		mProps = new Properties();
		mProps.put("obfuscationType", this.getClass().getName());
	}
	
	private void tint(int deg, int picw, int pich, Bitmap mBitmap) {
		   int[] pix = new int[picw * pich];
		   mBitmap.getPixels(pix, 0, picw, 0, 0, picw, pich);

		   int RY, GY, BY, RYY, GYY, BYY, R, G, B, Y;
		   double angle = (3.14159d * (double)deg) / 180.0d;
		   int S = (int)(256.0d * Math.sin(angle));
		   int C = (int)(256.0d * Math.cos(angle));

		   for (int y = 0; y < pich; y++)
		   for (int x = 0; x < picw; x++)
		      {
		      int index = y * picw + x;
		      int r = (pix[index] >> 16) & 0xff;
		      int g = (pix[index] >> 8) & 0xff;
		      int b = pix[index] & 0xff;
		      RY = ( 70 * r - 59 * g - 11 * b) / 100;
		      GY = (-30 * r + 41 * g - 11 * b) / 100;
		      BY = (-30 * r - 59 * g + 89 * b) / 100;
		      Y  = ( 30 * r + 59 * g + 11 * b) / 100;
		      RYY = (S * BY + C * RY) / 256;
		      BYY = (C * BY - S * RY) / 256;
		      GYY = (-51 * RYY - 19 * BYY) / 100;
		      R = Y + RYY;
		      R = (R < 0) ? 0 : ((R > 255) ? 255 : R);
		      G = Y + GYY;
		      G = (G < 0) ? 0 : ((G > 255) ? 255 : G);
		      B = Y + BYY;
		      B = (B < 0) ? 0 : ((B > 255) ? 255 : B);
		      pix[index] = 0xff000000 | (R << 16) | (G << 8) | B;
		      }

		   originalBmp.setPixels(pix, 0, picw, 0, 0, picw, pich);

		   pix = null;
		}

	@Override
	public void processRegion(RectF rect, Canvas canvas, Bitmap bitmap) {
		// return properties and data as a map
		mProps.put("initialCoordinates", "[" + rect.top + "," + rect.left + "]");
		mProps.put("regionWidth", Float.toString(Math.abs(rect.left - rect.right)));
		mProps.put("regionHeight", Float.toString(Math.abs(rect.top - rect.bottom)));
	}
	
	public Properties getProperties()
	{
		return mProps;
	}
	
	public void setProperties(Properties props)
	{
		mProps = props;
	}

	@Override
	public Bitmap getBitmap() {
		// TODO Auto-generated method stub
		return null;
	}

}
