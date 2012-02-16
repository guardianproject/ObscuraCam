package org.witness.securesmartcam.jpegredaction;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Properties;

import org.witness.informa.utils.InformaConstants;
import org.witness.informa.utils.InformaConstants.Keys;
import org.witness.informa.utils.secure.MediaHasher;
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
import android.util.Base64;
import android.util.Log;

public class JpegRedaction implements RegionProcesser {
	
    private native byte[] setRegion(String src, String target, int left, int right, int top, int bottom, String method);
    Properties mProps;

    static {
        System.loadLibrary("JpegRedaction");
    }

    private String mInFile, mOutFile;
    private String mMethod = null;
    public boolean hasRedacted = false;
    	 
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
			 
			 byte[] redactionPack = setRegion(mInFile, mOutFile, left, right, top, bottom, mMethod);
			 try {
				 mProps.setProperty(Keys.ImageRegion.UNREDACTED_DATA, Base64.encodeToString(redactionPack, Base64.DEFAULT));
				 mProps.setProperty(Keys.ImageRegion.UNREDACTED_HASH, MediaHasher.hash(redactionPack, "MD5"));
				 hasRedacted = true;

			 } catch(NoSuchAlgorithmException e) {
				 Log.d(InformaConstants.TAG, e.toString());
			 } catch (IOException e) {
				 Log.d(InformaConstants.TAG, e.toString());
			}
		}
		
	}
	
	public Properties getProperties()
	{
		return mProps;
	}
	
	public void setProperties(Properties props)
	{
		
		mProps = props;
		Log.d(InformaConstants.TAG, mProps.toString());
	}
	@Override
	public Bitmap getBitmap() {
		// TODO Auto-generated method stub
		return null;
	}
}