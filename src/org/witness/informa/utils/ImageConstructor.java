package org.witness.informa.utils;

import info.guardianproject.database.sqlcipher.SQLiteDatabase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.witness.informa.utils.InformaConstants.Keys;
import org.witness.informa.utils.InformaConstants.Keys.Image;
import org.witness.informa.utils.InformaConstants.Keys.ImageRegion;
import org.witness.informa.utils.InformaConstants.Keys.Informa;
import org.witness.informa.utils.InformaConstants.Keys.Settings;
import org.witness.informa.utils.InformaConstants.Keys.Tables;
import org.witness.informa.utils.InformaConstants.OriginalImageHandling;
import org.witness.informa.utils.io.DatabaseHelper;
import org.witness.informa.utils.secure.Apg;
import org.witness.informa.utils.secure.MediaHasher;
import org.witness.securesmartcam.filters.CrowdPixelizeObscure;
import org.witness.securesmartcam.filters.InformaTagger;
import org.witness.securesmartcam.filters.PixelizeObscure;
import org.witness.securesmartcam.filters.SolidObscure;
import org.witness.securesmartcam.utils.ObscuraConstants;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
	ArrayList<File> imageRegionFiles;
	JSONObject metadataObject;
	File clone;
	String base, unredactedHash, redactedHash;
	String containmentArray = InformaConstants.NOT_INCLUDED;
	
	FileOutputStream fos;
	ZipOutputStream zos;
	
	Context c;
	private DatabaseHelper dh;
	private SQLiteDatabase db;
	private SharedPreferences _sp;
	
	static {
		System.loadLibrary("JpegRedaction");
	}
	
	
	public ImageConstructor(Context c, String metadataObjectString, String baseName) throws JSONException, NoSuchAlgorithmException, IOException {
		this.c = c;
		this.unredactedRegions = new ArrayList<ContentValues>();
		clone = new File(InformaConstants.DUMP_FOLDER, ObscuraConstants.TMP_FILE_NAME);
		
		_sp = PreferenceManager.getDefaultSharedPreferences(c);
		dh = new DatabaseHelper(c);
		db = dh.getWritableDatabase(_sp.getString(Keys.Settings.HAS_DB_PASSWORD, ""));
		
		unredactedHash = MediaHasher.hash(fileToBytes(clone), "SHA-1");
		
		base = baseName.split("_")[0] + ".jpg";
		
		// handle original based on settings
		handleOriginalImage();
		
		// do redaction
		imageRegionFiles = new ArrayList<File>();
		metadataObject = (JSONObject) new JSONTokener(metadataObjectString).nextValue();
		this.imageRegions = (JSONArray) (metadataObject.getJSONObject(Keys.Informa.DATA)).getJSONArray(Keys.Data.IMAGE_REGIONS);
		
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
				
				// create a file for the image region				
				imageRegionFiles.add(bytesToFile(redactionPack, "ir" + i + ".informaRegion"));
				
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
		
		dh.setTable(db, Tables.IMAGE_REGIONS);
		for(ContentValues rcv : unredactedRegions)
			db.insert(dh.getTable(), null, rcv);
		
		redactedHash = MediaHasher.hash(fileToBytes(clone), "SHA-1");
	}

	public void doCleanup() {
		File informaDir = new File(InformaConstants.DUMP_FOLDER);
    	FileFilter cleanup = new FileFilter() {
    		String[] extensions = new String[] {"jpg","informaregion"};
			@Override
			public boolean accept(File file) {
				
				for(String e : extensions)
					if(file.getName().toLowerCase().endsWith(e))
						return true;
						
				return false;
			}
    		
    	};
    	
    	for(File f : informaDir.listFiles(cleanup))
    		f.delete();
    			
		db.close();
		dh.close();
		
		clone.delete();
	}
	
	public int createVersionForTrustedDestination(String informaImageFilename, String intendedDestination) throws JSONException, NoSuchAlgorithmException, IOException {
		int result = 0;
		
		// replace the metadata's intended destination
		metadataObject.getJSONObject(Keys.Informa.INTENT).put(Keys.Intent.INTENDED_DESTINATION, intendedDestination);
		
		// insert metadata
		if(constructImage(clone.getAbsolutePath(), informaImageFilename, metadataObject.toString(), metadataObject.toString().length()) > 0) {
			ContentValues cv = new ContentValues();
			// package zipped image region bytes
			cv.put(Image.METADATA, metadataObject.toString());
			cv.put(Image.REDACTED_IMAGE_HASH, redactedHash);
			cv.put(Image.LOCATION_OF_ORIGINAL, ((JSONObject) metadataObject.getJSONObject(Informa.GENEALOGY)).getString(Keys.Image.LOCAL_MEDIA_PATH));
			cv.put(Image.LOCATION_OF_OBSCURED_VERSION, informaImageFilename);
			cv.put(Keys.Intent.Destination.EMAIL, ((JSONObject) metadataObject.getJSONObject(Informa.INTENT)).getString(Keys.Intent.INTENDED_DESTINATION));
			cv.put(Image.CONTAINMENT_ARRAY, containmentArray);
			cv.put(Image.UNREDACTED_IMAGE_HASH, unredactedHash);
			
			
			dh.setTable(db, Tables.IMAGES);
			db.insert(dh.getTable(), null, cv);
			
			// zip file for trusted destination
			File informa = new File(informaImageFilename.substring(0, informaImageFilename.length() - 4) + ".informa");
			fos = new FileOutputStream(informa);
			zos = new ZipOutputStream(fos);
			
			addToZip(new File(informaImageFilename));
			
			for(File f : imageRegionFiles) {
				addToZip(f);
			}
			
			zos.close();
			fos.close();
			
			// TODO: DO ENCRYPT!
			result = 1;
		}
		
		return result;
	}
	
	private void handleOriginalImage() {		
		switch(Integer.parseInt(_sp.getString(Keys.Settings.DEFAULT_IMAGE_HANDLING,""))) {
		case OriginalImageHandling.ENCRYPT_ORIGINAL:
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						encryptOriginal();
					} catch (IOException e) {}
				}
			}).start();
			break;
		case OriginalImageHandling.LEAVE_ORIGINAL_ALONE:
			new Thread(new Runnable() {
				@Override
				public void run() {
					copyOriginalToSDCard();
				}
			}).start();
			break;
		}
	}
	
	private void copyOriginalToSDCard() {
		Log.d(InformaConstants.TAG, "copying original to sd card...");
	}
	
	private void encryptOriginal() throws IOException {
		JSONArray containment = new JSONArray();
		long clength = clone.length();
		int parts = (int) Math.ceil(clength/InformaConstants.BLOB_MAX) + 1;
		
		dh.setTable(db, Tables.ENCRYPTED_IMAGES);
		
		byte[] b = fileToBytes(clone);
		int bytesLeft = b.length;
		int offset = 0;
		for(int i=0; i<parts; i++) {
			byte[] cpy = new byte[Math.min(bytesLeft, InformaConstants.BLOB_MAX)];
			System.arraycopy(b, offset, cpy, 0, cpy.length);
			offset += cpy.length;
			bytesLeft -= cpy.length;
			
			ContentValues cv = new ContentValues();
			cv.put(Keys.ImageRegion.BASE, base);
			cv.put(Keys.ImageRegion.DATA, cpy);
			containment.put(db.insert(dh.getTable(), null, cv));
		}
		
		containmentArray = containment.toString();
		
	}
	
	private void addToZip(File file) throws IOException {
		byte[] buffer = new byte[(int) file.length()];
		
		FileInputStream fis = new FileInputStream(file);
		zos.putNextEntry(new ZipEntry(file.getAbsolutePath()));
		
		int bytesRead = 0;
		while((bytesRead = fis.read(buffer)) > 0)
			zos.write(buffer, 0, bytesRead);
		zos.closeEntry();
		fis.close();
	}
	
	private byte[] gzipImageRegionData(byte[] data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gos = new GZIPOutputStream(baos);
		gos.write(Base64.encode(data, Base64.DEFAULT));
		gos.close();
		baos.close();
		return baos.toByteArray();
	}
	
	private File bytesToFile(byte[] data, String filename) throws IOException {
		File byteFile = new File(InformaConstants.DUMP_FOLDER, filename);
		FileOutputStream fos = new FileOutputStream(byteFile);
		fos.write(data);
		fos.close();
		return byteFile;
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
