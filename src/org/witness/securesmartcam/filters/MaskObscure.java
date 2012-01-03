/*
 * This ObscureMethod paints the "anon.jpg" over the region
 */

package org.witness.securesmartcam.filters;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.witness.sscphase1.ObscuraApp;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

public class MaskObscure implements RegionProcesser {

	Bitmap _bitmap, unredactedBmp;
	boolean unredactedBmpSet;
	Paint _painter;
	Context _context;
	
	Properties mProps;
	
	public MaskObscure(Context context, Paint painter) {
		
		_painter = painter;
		_context = context;

		mProps = new Properties ();
		mProps.put("path", "mask.png");
		mProps.put("obfuscationType", this.getClass().getName());
		
		mProps.put("timestampOnGeneration", new Date().getTime());
		unredactedBmpSet = false;
	}
	
	public void processRegion(RectF rect, Canvas canvas,  Bitmap bitmap) {
	
		_bitmap = bitmap;
		
		if(!unredactedBmpSet) {
			unredactedBmp = Bitmap.createBitmap(
					bitmap, 
					(int) rect.left, 
					(int) rect.top,
					(int) Math.min(bitmap.getWidth(),(Math.abs(rect.left - rect.right))), 
					(int) Math.min(bitmap.getHeight(), (Math.abs(rect.top - rect.bottom)))
				);
			unredactedBmpSet = true;
			Log.d(ObscuraApp.TAG, "this is where the bitmap is set.");
		} else
			Log.d(ObscuraApp.TAG, "nope, original bmp already set.");
		
		try
		{
		  Bitmap mask = loadBitmap(_context,mProps.getProperty("path"));
		  canvas.drawBitmap(mask, null, rect, _painter);
		}
		catch (IOException e)
		{
			Log.e("anon",e.toString(),e);
		}
		
		// return properties and data as a map
		mProps.put("initialCoordinates", "[" + rect.top + "," + rect.left + "]");
		mProps.put("regionWidth", Float.toString(Math.abs(rect.left - rect.right)));
		mProps.put("regionHeight", Float.toString(Math.abs(rect.top - rect.bottom)));		
			

	}
	
	
	public static Bitmap loadBitmap(Context context, String filename) throws IOException
	{
	    AssetManager assets = context.getResources().getAssets();
	    InputStream buf = new BufferedInputStream((assets.open(filename)));
	    Bitmap bitmap = BitmapFactory.decodeStream(buf);
	    return bitmap;
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
		return unredactedBmp;
	}

	@Override
	public void updateBitmap() {
		unredactedBmpSet = false;
		
	}
}



