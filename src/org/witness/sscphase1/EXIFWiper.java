package org.witness.sscphase1;

import java.io.IOException;

import android.media.ExifInterface;
import android.util.Log;

public class EXIFWiper {
	
	public static final String LOGTAG = "EXIFWIPER";
		
	String imageFilePath;
	ExifInterface ei;
	
	public EXIFWiper(String _imageFilePath) throws IOException {
		imageFilePath = _imageFilePath;
		Log.v(LOGTAG,"Image Path:" + imageFilePath);
		ei = new ExifInterface(_imageFilePath); 
	}

	public void wipeIt() throws IOException {
		// Go through list and zero everything out.
		// Can we get list??  
		//ei.setAttribute("ImageDescription","Something New");
		
		// Is this all of them??
		ei.setAttribute(ExifInterface.TAG_DATETIME,"");
		ei.setAttribute(ExifInterface.TAG_FLASH, "");
		ei.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "");
		ei.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "");
		ei.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "");
		ei.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "");
		ei.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, "");
		ei.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, "");
		ei.setAttribute(ExifInterface.TAG_MAKE, "");
		ei.setAttribute(ExifInterface.TAG_MODEL, "");
		ei.setAttribute(ExifInterface.TAG_ORIENTATION, "");
		ei.setAttribute(ExifInterface.TAG_WHITE_BALANCE, "");
		//ei.setAttribute(ExifInterface, "");
		
		ei.saveAttributes();
	}
	
	
}
