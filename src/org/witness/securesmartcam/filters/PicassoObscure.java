/*
 * This ObscureMethod pixelizes the region
 */

package org.witness.securesmartcam.filters;


import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

public class PicassoObscure implements RegionProcesser {

	Bitmap originalBmp;
	Properties mProps;
	
	public PicassoObscure ()
	{
		mProps = new Properties ();
		mProps.put("size", "10");
		mProps.put("obfuscationType", this.getClass().getName());
	}
	
	public void processRegion(RectF rect, Canvas canvas, Bitmap bitmap) {
	
		originalBmp = bitmap;
		
	
		
		//pixelate(rect, pixelSize);
		
		//originalBmp.setPixels(pixels, 0, pixelSize, x, y, wPixelSize, hPixelSize);
		
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



