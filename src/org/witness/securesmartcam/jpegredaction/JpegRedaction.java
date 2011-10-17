package org.witness.securesmartcam.jpegredaction;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.witness.securesmartcam.ImageRegion;
import org.witness.securesmartcam.filters.CrowdPixelizeObscure;
import org.witness.securesmartcam.filters.PaintSquareObscure;
import org.witness.securesmartcam.filters.PixelizeObscure;
import org.witness.securesmartcam.filters.RegionProcesser;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;

public class JpegRedaction implements RegionProcesser {
	
    private native void redactRegion(String src, String target, int left, int right, int top, int bottom, String method);
    private native void redactRegions(String src, String target, String regions);
    Properties mProps;

    static {
        System.loadLibrary("JpegRedaction");
    }

    private File mInFile;
    private File mOutFile;
    private String mMethod;
    
    private final static String METHOD_COPYSTRIP = "c";
    private final static String METHOD_SOLID = "s";
    private final static String METHOD_PIXELLATE = "p";
    private final static String METHOD_OVERLAY = "o";
    private final static String METHOD_INVERSE_PIXELLATE = "i";
    	 
    public JpegRedaction (RegionProcesser iMethod, File inFile, File outFile)
    {
    	setFiles (inFile, outFile);
    	setMethod (iMethod);
    	mProps = new Properties();
    }
    
    public JpegRedaction (File inFile, File outFile)
    {
    	setFiles (inFile, outFile);
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
    	else
    	{
    		mMethod = METHOD_INVERSE_PIXELLATE;
    	}
    	    	
    }
    
	@Override
	public void processRegion(Rect rect, Canvas canvas, Bitmap bitmap) {

		 String strInFile = mInFile.getAbsolutePath();
		 String strOutFile = mOutFile.getAbsolutePath();
	     redactRegion(strInFile, strOutFile, rect.left, rect.right, rect.top, rect.bottom, mMethod);
	     
	     // return properties and data as a map
	     mProps.put("initialCoordinates", "[" + rect.top + "," + rect.left + "]");
	     mProps.put("regionWidth", Integer.toString(Math.abs(rect.left - rect.right)));
		 mProps.put("regionHeight", Integer.toString(Math.abs(rect.top - rect.bottom)));
		
	}
	
	public void obscureRegions(String regions) {

		 String strInFile = mInFile.getAbsolutePath();
		 String strOutFile = mOutFile.getAbsolutePath();
	     redactRegions(strInFile, strOutFile, regions);
		
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