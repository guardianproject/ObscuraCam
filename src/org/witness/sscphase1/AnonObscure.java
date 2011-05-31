/*
 * This ObscureMethod paints the "anon.jpg" over the region
 */

package org.witness.sscphase1;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

public class AnonObscure extends Activity implements ObscureMethod {

	Bitmap _bitmap;
	Paint _painter;
	Context _context;
	
	public AnonObscure(Context context, Bitmap bitmap, Paint painter) {
		_bitmap = bitmap;
		_painter = painter;
		_context = context;
	}
	
	public void obscureRect(Rect rect, Canvas canvas) {
	
		try
		{
		  Bitmap mask = loadBitmap(_context,"anon.png");
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
	    // Drawable d = new BitmapDrawable(bitmap);
	    return bitmap;
	}
}



