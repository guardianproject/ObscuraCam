/*
 * This ObscureMethod paints a solid blue rectangle over the region
 */

package org.witness.securesmartcam.filters;


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
	
	Bitmap originalBmp;
	
	public SolidObscure() {
		paint = new Paint();
        paint.setColor(Color.BLACK);
        
        mProps = new Properties();
		mProps.put(ImageRegion.FILTER, this.getClass().getName());
		mProps.put(ImageRegion.TIMESTAMP, System.currentTimeMillis());
	}
 	
	public void processRegion(RectF rect, Canvas canvas,  Bitmap bitmap) {
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

	@Override
	public Bitmap getBitmap() {
		return null;
	}
}
