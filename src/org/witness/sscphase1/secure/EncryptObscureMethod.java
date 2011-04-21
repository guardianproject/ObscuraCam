package org.witness.sscphase1.secure;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.witness.sscphase1.ObscureMethod;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Base64;
import android.util.Log;

public class EncryptObscureMethod implements ObscureMethod
{
	private Apg _apg;
	private Activity _context;
	private Bitmap _fullBitmap;
	
	public EncryptObscureMethod (Activity context, Bitmap fullBitmap, long signKey, long[] encryptKeys)
	{
		_context = context;
		_apg = Apg.getInstance();
		_fullBitmap = fullBitmap;
		
		if (!_apg.isAvailable(context))
		{
			//should prompt to install, open the market etc
		}
		
	//	_apg.setSignatureKeyId(signKey);
	//	_apg.setEncryptionKeys(encryptKeys);
		
	}
	
	public void obscureRect(Rect rect, Canvas canvas) {
		
		//will encrypted using selected keys		
	
        Bitmap facebmp = Bitmap.createBitmap(_fullBitmap,rect.left,rect.top,rect.width(),rect.height());

        int height = facebmp.getHeight();
        int width = facebmp.getWidth();

        int[] pixels = new int[height * width];

        facebmp.getPixels(pixels, 0, width, 1, 1, width - 1, height - 1);
        
        
        /*
        int alpha=argb>>24;
        int red=(argb-alpha)>>16;
        int green=(argb-(alpha+red))>>8;
        int blue=(argb-(alpha+red+green));
       */
        
//        canvas.drawBitmap(facebmp, rect, rect, );
        
       // _fullBitmap.setPixel(x, y, Color.rgb(Color.red(r), Color.green(g), Color.blue(b)));
        
        try
        {
        	byte[] pixelBytes = intsToBytes(pixels);
        	
        	Log.d("EncryptObscure","got pixel bytes. length=" + pixelBytes.length);
        	
        	String pixelString = Base64.encodeToString(pixelBytes, Base64.DEFAULT);
        	
        //	_apg.encrypt(_context, "application/data", pixelBytes);
        	_apg.encrypt(_context, pixelString);
        
        }
        catch (IOException ioe)
        {
        	Log.e("Encrypt","error encrypted region",ioe);
        }
	}
	
	byte[] intsToBytes(int[] values) throws IOException
	{
	   ByteArrayOutputStream baos = new ByteArrayOutputStream();
	   DataOutputStream dos = new DataOutputStream(baos);
	   for(int i=0; i < values.length; ++i)
	   {
	        dos.writeInt(values[i]);
	   }

	   return baos.toByteArray();
	}

}
