package org.witness.securesmartcam.filters;

import java.util.Properties;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public interface RegionProcesser {
	
	public void processRegion(Rect rect, Canvas canvas, Bitmap originalBmp);
	
	public Properties getProperties();
	
	public void setProperties(Properties props);
	
}
