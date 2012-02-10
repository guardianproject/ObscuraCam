package org.witness.securesmartcam.jpegredaction;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

import org.witness.informa.utils.InformaConstants;
import org.witness.informa.utils.InformaConstants.Keys;
import org.witness.securesmartcam.ImageRegion;
import org.witness.securesmartcam.filters.CrowdPixelizeObscure;
import org.witness.securesmartcam.filters.InformaTagger;
import org.witness.securesmartcam.filters.PixelizeObscure;
import org.witness.securesmartcam.filters.RegionProcesser;
import org.witness.securesmartcam.filters.SolidObscure;
import org.witness.securesmartcam.utils.ObscuraConstants;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;

public class JpegRedaction implements RegionProcesser {
	
    private native int setRegion(String src, String target, int left, int right, int top, int bottom, String method, char[] redactionBuffer);
    Properties mProps;

    static {
        System.loadLibrary("JpegRedaction");
    }

    private String mInFile, mOutFile;
    private String mMethod = null;
    
    private char[] redactionPack = new char[] {};
    	 
    public JpegRedaction (RegionProcesser iMethod, String inFile, String outFile)
    {
    	setFiles (inFile, outFile);
    	
    	if (iMethod != null)
    		setMethod (iMethod);
    	
    	mProps = new Properties();
    }
    
    
    public void setFiles (String inFile, String outFile)
    {
    	mInFile = inFile;
    	mOutFile = outFile;
    }

    public void setMethod (RegionProcesser rProc)
    {
    	if (rProc instanceof CrowdPixelizeObscure)
    	{
    		mMethod = ObscuraConstants.Filters.CROWD_PIXELIZE;
    	}
    	else if (rProc instanceof PixelizeObscure)
    	{
    		mMethod = ObscuraConstants.Filters.PIXELIZE;
    	}
    	else if (rProc instanceof SolidObscure)
    	{
    		mMethod = ObscuraConstants.Filters.SOLID;
    	}
    	else if (rProc instanceof InformaTagger)
    	{
    		mMethod = ObscuraConstants.Filters.INFORMA_TAGGER;
    	}
    }
    
	@Override
	public void processRegion(RectF rect, Canvas canvas, Bitmap bitmap) {
		
		if (mMethod != null)
		{
	
			 int left = Math.max(0, (int)rect.left);
			 int right = Math.min(canvas.getWidth(),(int)rect.right);
			 int top = Math.max(0, (int)rect.top);
			 int bottom = Math.max(canvas.getHeight(), (int)rect.bottom);
			 
			 if (setRegion(mInFile, mOutFile, left, right, top, bottom, mMethod, redactionPack) == 1)
				 mProps.setProperty(Keys.ImageRegion.UNREDACTED_HASH, convertRedactionPack());
				 
		}
		
	}
	
	public String convertRedactionPack() {
		String byteString = "";
		/*
		 *  TODO:
		 *  1) turn char[] into byte[]
		 *  2) base64 encode bytes
		 */
		
		return byteString;
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