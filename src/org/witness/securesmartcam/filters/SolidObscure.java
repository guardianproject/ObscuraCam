/*
 * This ObscureMethod paints a solid blue rectangle over the region
 */

package org.witness.securesmartcam.filters;


import java.util.Date;
import java.util.Properties;

import org.witness.informa.utils.InformaConstants.Keys.ImageRegion;
import org.witness.securesmartcam.utils.ObscuraConstants;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

public class SolidObscure implements RegionProcesser {

	Paint paint;
	Properties mProps;
	
	Bitmap originalBmp, unredactedBmp;
	boolean unredactedBmpSet;
	
	public SolidObscure() {
		paint = new Paint();
        paint.setColor(Color.BLACK);
        
        mProps = new Properties();
		mProps.put(ImageRegion.FILTER, this.getClass().getName());
		mProps.put(ImageRegion.TIMESTAMP, System.currentTimeMillis());
        unredactedBmpSet = false;
	}
 	
	public void processRegion(RectF rect, Canvas canvas,  Bitmap bitmap) {
		// capture the original bitmpap;
		if(!unredactedBmpSet) {
			unredactedBmp = Bitmap.createBitmap(
					bitmap, 
					(int) rect.left, 
					(int) rect.top,
					(int) Math.min(bitmap.getWidth(),(Math.abs(rect.left - rect.right))), 
					(int) Math.min(bitmap.getHeight(), (Math.abs(rect.top - rect.bottom)))
				);
			unredactedBmpSet = true;
			Log.d(ObscuraConstants.TAG, "this is where the bitmap is set.");
		} else
			Log.d(ObscuraConstants.TAG, "nope, original bmp already set.");
		
		canvas.drawRect(rect, paint);
		// return properties and data as a map
		mProps.put(ImageRegion.COORDINATES, "[" + rect.top + "," + rect.left + "]");
		mProps.put(ImageRegion.WIDTH, Integer.toString((int) Math.abs(rect.left - rect.right)));
		mProps.put(ImageRegion.HEIGHT, Integer.toString((int) Math.abs(rect.top - rect.bottom)));
	}

	public Properties getProperties()
	{
		return mProps;
	}
	
	public void setProperties(Properties props)
	{
		mProps = props;
	}
	
	public void updateBitmap() {
		unredactedBmpSet = false;
	}

	@Override
	public Bitmap getBitmap() {
		return unredactedBmp;
	}
}
