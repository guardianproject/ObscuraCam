package org.witness.securesmartcam.filters;

import java.util.Properties;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

public class ConsentTagger implements RegionProcesser
{
	Properties mProps;
	
	public ConsentTagger ()
	{
		mProps = new Properties ();
		mProps.put("regionSubject", "");
		mProps.put("informedConsent", "false");
		mProps.put("persistObscureType", "false");
		mProps.put("obfuscationType", this.getClass().getName());		
	}
	
	@Override
	public void processRegion (Rect rect, Canvas canvas,  Bitmap bitmap) 
	{
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
