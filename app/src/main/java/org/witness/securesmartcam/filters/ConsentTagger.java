package org.witness.securesmartcam.filters;

import java.util.Properties;

import org.witness.sscphase1.ObscuraApp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;

public class ConsentTagger implements RegionProcesser
{
	Properties mProps;
	private Bitmap mPreview;
	
	public ConsentTagger ()
	{
		mProps = new Properties ();
		mProps.put("regionSubject", "");
		mProps.put("informedConsent", "false");
		mProps.put("persistObscureType", "false");
		mProps.put("obfuscationType", this.getClass().getName());	
	}
	
	@Override
	public void processRegion (RectF rect, Canvas canvas,  Bitmap bitmap) 
	{
		// return properties and data as a map
		mProps.put("initialCoordinates", "[" + rect.top + "," + rect.left + "]");
		mProps.put("regionWidth", Float.toString(Math.abs(rect.left - rect.right)));
		mProps.put("regionHeight", Float.toString(Math.abs(rect.top - rect.bottom)));
		mPreview = Bitmap.createBitmap(
				bitmap, 
				(int) rect.left, 
				(int) rect.top,
				(int) Math.min(bitmap.getWidth(),(Math.abs(rect.left - rect.right))), 
				(int) Math.min(bitmap.getHeight(), (Math.abs(rect.top - rect.bottom)))
			);
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
		return mPreview;
	}
}
