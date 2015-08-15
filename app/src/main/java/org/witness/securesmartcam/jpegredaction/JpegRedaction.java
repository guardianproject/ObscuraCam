package org.witness.securesmartcam.jpegredaction;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.witness.securesmartcam.ImageRegion;
import org.witness.securesmartcam.filters.CrowdPixelizeObscure;
import org.witness.securesmartcam.filters.PixelizeObscure;
import org.witness.securesmartcam.filters.RegionProcesser;
import org.witness.securesmartcam.filters.SolidObscure;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;

public class JpegRedaction implements RegionProcesser {
	
    private native void redactRegion(String src, String target, int left, int right, int top, int bottom, String method);
    private native void redactRegions(String src, String target, String regions);
    Properties mProps;

    static {
        System.loadLibrary("JpegRedaction");
    }

    private File mInFile;
    private File mOutFile;
    private String mMethod = null;
    
    private final static String METHOD_COPYSTRIP = "c";
    private final static String METHOD_SOLID = "s";
    private final static String METHOD_PIXELLATE = "p";
    private final static String METHOD_OVERLAY = "o";
    private final static String METHOD_INVERSE_PIXELLATE = "i";
    	 
    public JpegRedaction (RegionProcesser iMethod, File inFile, File outFile)
    {
    	setFiles (inFile, outFile);
    	
    	if (iMethod != null)
    		setMethod (iMethod);
    	
    	mProps = new Properties();
    	mProps.put("obfuscationType", this.getClass().getName());
    }
    
    public JpegRedaction ()
    {
    	this (null, null, null);
    }
    
    
    public JpegRedaction (File inFile, File outFile)
    {
    	this (null, inFile, outFile);
    }
    
    public void setFiles (File inFile, File outFile)
    {
    	mInFile = inFile;
    	mOutFile = outFile;
    }

    public void setMethod (RegionProcesser rProc)
    {
    	if (rProc instanceof CrowdPixelizeObscure)
    	{
    		mMethod = METHOD_INVERSE_PIXELLATE;
    	}
    	else if (rProc instanceof PixelizeObscure)
    	{
    		mMethod = METHOD_PIXELLATE;
    	}
    	else if (rProc instanceof SolidObscure)
    	{
    		mMethod = METHOD_SOLID;
    	}
    }
    
	@Override
	public void processRegion(RectF rect, Canvas canvas, Bitmap bitmap) {

		if (mMethod != null)
		{
			 String strInFile = mInFile.getAbsolutePath();
			 String strOutFile = mOutFile.getAbsolutePath();
	
			 int left = Math.max(0, (int)rect.left);
			 int right = Math.min(canvas.getWidth(),(int)rect.right);
			 int top = Math.max(0, (int)rect.top);
			 int bottom = Math.max(canvas.getHeight(), (int)rect.bottom);
			 
			 redactRegion(strInFile, strOutFile, left, right, top, bottom, mMethod);
		     
		     // return properties and data as a map
		     mProps.put("initialCoordinates", "[" + rect.top + "," + rect.left + "]");
		     mProps.put("regionWidth", Float.toString(Math.abs(rect.left - rect.right)));
			 mProps.put("regionHeight", Float.toString(Math.abs(rect.top - rect.bottom)));
		}
		
	}
	
	public void processRegions(ArrayList<ImageRegion> iRegions, float inSampleSize, Canvas canvas) {

		 String strInFile = mInFile.getAbsolutePath();
		 String strOutFile = mOutFile.getAbsolutePath();
		 
		 //l,r,t,b[:method];
		 StringBuffer sbRegions = new StringBuffer();
		 int rWidth = (int)( canvas.getWidth()*inSampleSize);
		 int rHeight = (int)(canvas.getHeight()*inSampleSize);
		 
		for (ImageRegion currentRegion : iRegions)
	    {
			RegionProcesser rProc = currentRegion.getRegionProcessor();
			
            RectF rect = new RectF(currentRegion.getBounds());
            rect.left *= inSampleSize;
            rect.top *= inSampleSize;
            rect.right *= inSampleSize;
            rect.bottom *= inSampleSize;

			 int left = Math.max(0, (int)rect.left);
			 int right = Math.min(rWidth,(int)rect.right);
			 int top = Math.max(0, (int)rect.top);
			 int bottom = Math.min(rHeight, (int)rect.bottom);
            
            sbRegions.append(left);
            sbRegions.append(',');

            sbRegions.append(right);
            sbRegions.append(',');

            sbRegions.append(top);
            sbRegions.append(',');

            sbRegions.append(bottom);
            sbRegions.append(':');

            if (rProc instanceof CrowdPixelizeObscure)
        	{
        		mMethod = METHOD_INVERSE_PIXELLATE;
        	}
        	else if (rProc instanceof PixelizeObscure)
        	{
        		mMethod = METHOD_PIXELLATE;
        	}
        	else if (rProc instanceof SolidObscure)
        	{
        		mMethod = METHOD_SOLID;
        	}
            
            sbRegions.append(mMethod);
            sbRegions.append(';');
            
	    
	    }

		Log.d("jpegredaction:size", rWidth + "x" + rHeight);
		Log.d("jpegredaction:regions", sbRegions.toString());
		
	    redactRegions(strInFile, strOutFile, sbRegions.toString());
		
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
		return null;
	}
}