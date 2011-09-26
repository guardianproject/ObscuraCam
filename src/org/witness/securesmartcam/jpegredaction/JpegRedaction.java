package org.witness.securesmartcam.jpegredaction;

import java.io.File;

import org.witness.securesmartcam.ImageRegion;
import org.witness.securesmartcam.filters.ObscureMethod;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;

public class JpegRedaction implements ObscureMethod {
	
    private native void redactRegion(String src, String target, int left, int right, int top, int bottom, String method);
    private native void redactRegions(String src, String target, String regions);

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
    	 
    public JpegRedaction (int iMethod, File inFile, File outFile)
    {
    	setFiles (inFile, outFile);
    	setMethod (iMethod);
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

    public void setMethod (int iMethod)
    {
    	switch (iMethod)
    	{
    		case ImageRegion.BG_PIXELIZE:
    			mMethod = METHOD_INVERSE_PIXELLATE;
    		break;
    		
    		case ImageRegion.SOLID:
    			mMethod = METHOD_SOLID;
    		break;
    		
    		case ImageRegion.PIXELIZE:
    			mMethod = METHOD_PIXELLATE;
    		break;
    		
    		default:
    			mMethod = METHOD_SOLID;
    	}
    }
    
	@Override
	public void obscureRect(Rect rect, Canvas canvas) {

		 String strInFile = mInFile.getAbsolutePath();
		 String strOutFile = mOutFile.getAbsolutePath();
	     redactRegion(strInFile, strOutFile, rect.left, rect.right, rect.top, rect.bottom, mMethod);
		
	}
	
	public void obscureRegions(String regions) {

		 String strInFile = mInFile.getAbsolutePath();
		 String strOutFile = mOutFile.getAbsolutePath();
	     redactRegions(strInFile, strOutFile, regions);
		
	}
}