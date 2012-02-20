package org.witness.informa.utils;

import info.guardianproject.database.sqlcipher.SQLiteDatabase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.witness.informa.utils.InformaConstants.Keys;
import org.witness.informa.utils.InformaConstants.Keys.Image;
import org.witness.informa.utils.InformaConstants.Keys.ImageRegion;
import org.witness.informa.utils.InformaConstants.Keys.Informa;
import org.witness.informa.utils.InformaConstants.Keys.Tables;
import org.witness.informa.utils.io.DatabaseHelper;
import org.witness.informa.utils.secure.Apg;
import org.witness.informa.utils.secure.MediaHasher;
import org.witness.securesmartcam.filters.CrowdPixelizeObscure;
import org.witness.securesmartcam.filters.InformaTagger;
import org.witness.securesmartcam.filters.PixelizeObscure;
import org.witness.securesmartcam.filters.SolidObscure;
import org.witness.securesmartcam.utils.ObscuraConstants;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

public class ImageConstructor {
	private native int constructImage(
			String originalImageFilename, 
			String informaImageFilename, 
			String metadataObjectString, 
			int metadataLength);
	private native byte[] redactRegion(
			String originalImageFilename,
			String informaImageFilename,
			int left,
			int right,
			int top,
			int bottom,
			String redactionCommand);
	
	private JSONArray imageRegions;
	private ArrayList<ContentValues> unredactedRegions;
	JSONObject metadataObject;
	
	Context c;
	
	private SharedPreferences _sp;
	
	static {
		System.loadLibrary("JpegRedaction");
	}
	
	
	public ImageConstructor(Context c, final String informaImageFilename, String metadataObjectString) throws JSONException, NoSuchAlgorithmException, IOException {
		this.c = c;
		this.unredactedRegions = new ArrayList<ContentValues>();
		File clone = new File(InformaConstants.DUMP_FOLDER, ObscuraConstants.TMP_FILE_NAME);
		String[] baseParts = informaImageFilename.split("/");
		String base = baseParts[baseParts.length - 1].split("_")[0] + ".jpg";
		
		metadataObject = (JSONObject) new JSONTokener(metadataObjectString).nextValue();
		this.imageRegions = (JSONArray) (metadataObject.getJSONObject(Keys.Informa.DATA)).getJSONArray(Keys.Data.IMAGE_REGIONS);
		
		ContentValues cv = new ContentValues();
		cv.put(Image.UNREDACTED_IMAGE_HASH, MediaHasher.hash(fileToBytes(clone), "SHA-1"));
		cv.put(Image.CONTAINMENT_ARRAY, "NOT INCLUDED IN THIS VERSION");
		
		for(int i=0; i<imageRegions.length(); i++) {
			JSONObject ir = imageRegions.getJSONObject(i);
			
			String redactionMethod = ir.getString(Keys.ImageRegion.FILTER);
			if(!redactionMethod.equals(InformaTagger.class.getName())) {
				
				JSONObject dimensions = ir.getJSONObject(Keys.ImageRegion.DIMENSIONS);
				JSONObject coords = ir.getJSONObject(Keys.ImageRegion.COORDINATES);
				
				int top = (int) (coords.getInt(Keys.ImageRegion.TOP));
				int left = (int) (coords.getInt(Keys.ImageRegion.LEFT));
				int right = (int) (left + dimensions.getInt(Keys.ImageRegion.WIDTH));
				int bottom = (int) (top + dimensions.getInt(Keys.ImageRegion.HEIGHT));
				
				String redactionCode = "";
				if(redactionMethod.equals(PixelizeObscure.class.getName()))
					redactionCode = ObscuraConstants.Filters.PIXELIZE;
				else if(redactionMethod.equals(SolidObscure.class.getName()))
					redactionCode = ObscuraConstants.Filters.SOLID;
				else if(redactionMethod.equals(CrowdPixelizeObscure.class.getName()))
					redactionCode = ObscuraConstants.Filters.CROWD_PIXELIZE;
				
				byte[] redactionPack = redactRegion(clone.getAbsolutePath(), clone.getAbsolutePath(), left, right, top, bottom, redactionCode);
				
				// insert hash and data length into metadata package
				ir.put(Keys.ImageRegion.UNREDACTED_HASH, MediaHasher.hash(redactionPack, "SHA-1"));
				ir.put(Keys.ImageRegion.UNREDACTED_DATA, redactionPack.length);
				
				//zip data
				ContentValues rcv = new ContentValues();
				rcv.put(ImageRegion.UNREDACTED_HASH, MediaHasher.hash(redactionPack, "SHA-1"));
				rcv.put(ImageRegion.DATA, gzipImageRegionData(redactionPack));
				rcv.put(ImageRegion.BASE, base);
				unredactedRegions.add(rcv);
			}
		}		
				
		// insert metadata
		if(constructImage(clone.getAbsolutePath(), informaImageFilename, metadataObject.toString(), metadataObject.toString().length()) > 0) {
			
			// package zipped image region bytes
			_sp = PreferenceManager.getDefaultSharedPreferences(c);
			DatabaseHelper dh = new DatabaseHelper(c);
			SQLiteDatabase db = dh.getWritableDatabase(_sp.getString(Keys.Settings.HAS_DB_PASSWORD, ""));
			
			cv.put(Image.METADATA, metadataObject.toString());
			cv.put(Image.REDACTED_IMAGE_HASH, MediaHasher.hash(fileToBytes(new File(informaImageFilename)), "SHA-1"));
			cv.put(Image.LOCATION_OF_ORIGINAL, ((JSONObject) metadataObject.getJSONObject(Informa.GENEALOGY)).getString(Keys.Image.LOCAL_MEDIA_PATH));
			cv.put(Image.LOCATION_OF_OBSCURED_VERSION, informaImageFilename);
			cv.put(Keys.Intent.Destination.EMAIL, ((JSONObject) metadataObject.getJSONObject(Informa.INTENT)).getString(Keys.Intent.INTENDED_DESTINATION));
			
			dh.setTable(db, Tables.IMAGES);
			db.insert(dh.getTable(), null, cv);
			
			dh.setTable(db, Tables.IMAGE_REGIONS);
			for(ContentValues rcv : unredactedRegions)
				db.insert(dh.getTable(), null, rcv);
			
			db.close();
			dh.close();
				
			// delete unencrypted
			clone.delete();
			
			
			c.sendBroadcast(
					new Intent()
					.setAction(InformaConstants.Keys.Service.FINISH_ACTIVITY));
		}
		
	}
	
	private byte[] gzipImageRegionData(byte[] data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gos = new GZIPOutputStream(baos);
		gos.write(Base64.encode(data, Base64.DEFAULT));
		gos.close();
		baos.close();
		return baos.toByteArray();
	}
	
	private byte[] fileToBytes(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		byte[] fileBytes = new byte[(int) file.length()];
		
		int offset = 0;
		int bytesRead = 0;
		while(offset < fileBytes.length && (bytesRead = fis.read(fileBytes, offset, fileBytes.length - offset)) >= 0)
			offset += bytesRead;
		fis.close();
		return fileBytes;
	}
	
    private void copy (Uri uriSrc, File fileTarget) throws IOException
    {
    	InputStream is = c.getContentResolver().openInputStream(uriSrc);
		
		OutputStream os = new FileOutputStream (fileTarget);
			
		copyStreams (is, os);
    	
    }
    
    private void copy (Uri uriSrc, Uri uriTarget) throws IOException
    {
    	
    	InputStream is = c.getContentResolver().openInputStream(uriSrc);
		
		OutputStream os = c.getContentResolver().openOutputStream(uriTarget);
			
		copyStreams (is, os);

    	
    }
    
    private static void copyStreams(InputStream input, OutputStream output) throws IOException {
        // if both are file streams, use channel IO
        if ((output instanceof FileOutputStream) && (input instanceof FileInputStream)) {
          try {
            FileChannel target = ((FileOutputStream) output).getChannel();
            FileChannel source = ((FileInputStream) input).getChannel();

            source.transferTo(0, Integer.MAX_VALUE, target);

            source.close();
            target.close();

            return;
          } catch (Exception e) { /* failover to byte stream version */
          }
        }

        byte[] buf = new byte[8192];
        while (true) {
          int length = input.read(buf);
          if (length < 0)
            break;
          output.write(buf, 0, length);
        }

        try {
          input.close();
        } catch (IOException ignore) {
        }
        try {
          output.close();
        } catch (IOException ignore) {
        }
      }
	
}
