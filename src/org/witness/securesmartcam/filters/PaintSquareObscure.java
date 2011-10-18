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
import android.graphics.Rect;

public class PaintSquareObscure implements RegionProcesser {

	Paint paint;
	Properties mProps;
	
	public PaintSquareObscure() {
		paint = new Paint();
        paint.setColor(Color.BLACK);
        
        mProps = new Properties();
        mProps.put("obfuscationType", this.getClass().getName());
	}
 	
	public void processRegion(Rect rect, Canvas canvas,  Bitmap bitmap) {
		canvas.drawRect(rect, paint);
		// return properties and data as a map
		mProps.put("initialCoordinates", "[" + rect.top + "," + rect.left + "]");
		mProps.put("regionWidth", Integer.toString(Math.abs(rect.left - rect.right)));
		mProps.put("regionHeight", Integer.toString(Math.abs(rect.top - rect.bottom)));
	}

	public Properties getProperties()
	{
		return mProps;
	}
	
	public void setProperties(Properties props)
	{
		mProps = props;
	}
}
