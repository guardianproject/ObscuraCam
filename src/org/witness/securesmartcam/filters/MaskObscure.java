/*
 * This ObscureMethod paints the "anon.jpg" over the region
 */

package org.witness.securesmartcam.filters;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

public class MaskObscure implements RegionProcesser {

	Bitmap _bitmap;
	Paint _painter;
	Context _context;
	
	Properties mProps;
	
	public MaskObscure(Context context, Paint painter) {
		
		_painter = painter;
		_context = context;

		mProps = new Properties ();
		mProps.put("path", "mask.png");
		
	}
	
	public void processRegion(RectF rect, Canvas canvas,  Bitmap bitmap) {
	
		_bitmap = bitmap;		
		
		try
		{
		  Bitmap mask = loadBitmap(_context,mProps.getProperty("path"));
		  canvas.drawBitmap(mask, null, rect, _painter);
		}
		catch (IOException e)
		{
			Log.e("anon",e.toString(),e);
		}
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
		return null;
	}
	
	public void setProperties(Properties props)
	{
		
	}
}



