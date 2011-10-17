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
	}
 	
	public void processRegion(Rect rect, Canvas canvas,  Bitmap bitmap) {
		canvas.drawRect(rect, paint);	
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
