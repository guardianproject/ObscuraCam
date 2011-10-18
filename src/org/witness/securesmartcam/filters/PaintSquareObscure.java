/*
 * This ObscureMethod paints a solid blue rectangle over the region
 */

package org.witness.securesmartcam.filters;


import java.util.Properties;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class PaintSquareObscure implements RegionProcesser {

	Paint paint;
	
	public PaintSquareObscure() {
		paint = new Paint();
        paint.setColor(Color.BLACK);
	}
 	
	public void processRegion(RectF rect, Canvas canvas,  Bitmap bitma) {
		canvas.drawRect(rect, paint);
	}

	public Properties getProperties()
	{
		return null;
	}
	
	public void setProperties(Properties props)
	{
		
	}
}
