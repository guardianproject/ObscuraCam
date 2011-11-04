/*
 * This ObscureMethod paints a solid blue rectangle over the region
 */

package org.witness.securesmartcam.filters;


import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class SolidObscure implements RegionProcesser {

	Paint paint;
	Properties mProps;
	
	public SolidObscure() {
		paint = new Paint();
        paint.setColor(Color.BLACK);
        
        mProps = new Properties();
        mProps.put("obfuscationType", this.getClass().getName());
	}
 	
	public void processRegion(RectF rect, Canvas canvas,  Bitmap bitma) {

		canvas.drawRect(rect, paint);
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
