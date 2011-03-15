package org.witness.sscphase1;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class SSCMetadataHandler extends SQLiteOpenHelper {
	private static final String DB_PATH = "/data/data/org.witness.sscphase1/databases/";
	private static final String DB_NAME = "camera_obscura.db";
	private SQLiteDatabase db;
	private final Context c;
	
	public Uri uriString;
	public String uriPath;
	public int index;
	
	public static final int WIPE_EXIF_DATA = -1;
	public static final int CLONE_EXIF_DATA = -2;
	public static final int RANDOMIZE_EXIF_DATA = -3;
	
	private static final String SSC = "[Camera Obscura : SSCMetadataHandler] ****************************";

	public SSCMetadataHandler(Context context) {
		super(context, DB_NAME, null, 1);
		this.c = context;
	}
	
	public void createDatabase() throws IOException {
		boolean dbExists = checkForDatabase();
		if(dbExists) {} else {
			this.getReadableDatabase();
			try {
				copyDataBase();
			} catch(IOException e) {
				Log.d(SSC,"DB COPY Error: " + e);
			}
		}
	}
	
	private boolean checkForDatabase() {
		SQLiteDatabase checkDB = null;
		try {
			String dbPath = DB_PATH + DB_NAME;
			checkDB = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
		} catch(SQLiteException e) {
			Log.d(SSC,"DB CHECK Error: " + e);
		}
		if(checkDB != null) {
			checkDB.close();
		}
		return checkDB != null ? true : false;
	}
	
	private void copyDataBase() throws IOException {
		InputStream is = c.getAssets().open(DB_NAME);
		OutputStream os = new FileOutputStream(DB_PATH + DB_NAME);
		byte[] buffer = new byte[1024];
		int length;
		while((length = is.read(buffer)) > 0) {
			os.write(buffer,0,length);
		}
		os.flush();
		os.close();
		is.close();
	}
	
	public boolean openDataBase() throws SQLException {
		db = SQLiteDatabase.openDatabase(DB_PATH + DB_NAME, null, SQLiteDatabase.OPEN_READWRITE);
		return db != null ? true : false;
	}
	
	public int insertIntoDatabase(String tableName, String targetColumn, String values) throws SQLException {
		String theQuery = "INSERT INTO " + tableName;
		if(targetColumn != null) {
			theQuery += " " + targetColumn;
		}
		theQuery += " VALUES (" + values + ")";
		db.execSQL(theQuery);
		
		// sloppy way of returning last row affected
		int i = -1;
		Cursor dbCount = db.rawQuery("SELECT * FROM " + tableName,null);
		if(dbCount != null) {
			dbCount.moveToLast();
			i = dbCount.getPosition();
			dbCount.close();
			Log.v(SSC,"DB COUNT = " + i);
		}
		return i;
	}
	
	public int getImageResourceCursor() {
		return index;
	}
	
	public Cursor readItemFromDatabase(String tableName,String refKey, String refVal) {
		Cursor dbResponse = null;
		dbResponse = db.rawQuery("SELECT * FROM " + tableName + " WHERE " + refKey + " = " + refVal,null);
		// TODO: and what are we going to do with the returned cursor?  Should this even return cursor?
		return dbResponse;
	}
	
	public ArrayList<String> readBatchFromDatabase(String tableName, String colName, String orderBy) {
		Cursor dbResponse = null;
		ArrayList<String> batch = new ArrayList<String>();
		dbResponse = db.rawQuery("SELECT " + colName + " FROM " + tableName,null);
		if(dbResponse != null) {
			dbResponse.moveToFirst();
			while(dbResponse.isAfterLast() == false) {
				batch.add(dbResponse.getString(1));
				dbResponse.moveToNext();
			}
		}
		dbResponse.close();
		return batch;
	}
	
	public String getFileNameFromUri(Uri uriString) {
		String fileName = null;
		String[] projection = {MediaStore.Images.Media.DATA};
		Cursor msData = c.getContentResolver().query(uriString, projection, null, null, null);
		msData.moveToFirst();
		fileName = msData.getString(msData.getColumnIndex(projection[0]));
		msData.close();
		return fileName;
	}
	
	public void initiateMedia(Uri uriString, int mediaType, int imageSource) throws IOException {
		/*
		 *  this method requires the mediaType to be passed (image or video)
		 *  although we're only dealing with images now, I think we'll
		 *  have to branch off behaviors when we deal with video in the future
		 */
		this.uriString = uriString;

		// insert initial info into database, creating a new record, then return its index.
		this.index = insertIntoDatabase("camera_obscura","(g_localMediaPath)","\"" + uriString.toString() + "\"");
		
		switch(mediaType) {
		case 1:
			// image
			switch(imageSource) {
			// the uriPath will be different, depending on the source of the image...
			case 1:
				this.uriPath = uriString.getPath();
				break;
			case 2:
				this.uriPath = getFileNameFromUri(uriString);
				break;
			}
			
			try {
				ExifInterface ei = new ExifInterface(uriPath);
				exifParser(ei,CLONE_EXIF_DATA);
			} catch (IOException e) {
				Log.d(SSC,"ioexception : " + e);
			}
			break;
		case 2:
			break;
		}
	}
	
	public String exifParser(ExifInterface ei, int mode) {
		String exifValues = "";
		String[] exifTags = c.getResources().getStringArray(R.array.ExifTagsDB);
		switch(mode) {
		case CLONE_EXIF_DATA:
			// get all exif values and input them into the database
			exifValues += exifTags[0] + "='" + ei.getAttribute(ExifInterface.TAG_DATETIME) + "',";
			exifValues += exifTags[1] + "='" + ei.getAttribute(ExifInterface.TAG_FLASH) + "',";
			exifValues += exifTags[2] + "='" + ei.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) + "',";
			exifValues += exifTags[3] + "='" + ei.getAttribute(ExifInterface.TAG_GPS_DATESTAMP) + "',";
			exifValues += exifTags[4] + "='" + ei.getAttribute(ExifInterface.TAG_GPS_LATITUDE) + "',";
			exifValues += exifTags[5] + "='" + ei.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) + "',";
			exifValues += exifTags[6] + "='" + ei.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) + "',";
			exifValues += exifTags[7] + "='" + ei.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF) + "',";
			exifValues += exifTags[8] + "='" + ei.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD) + "',";
			exifValues += exifTags[9] + "='" + ei.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP) + "',";
			exifValues += exifTags[10] + "='" + ei.getAttribute(ExifInterface.TAG_IMAGE_LENGTH) + "',";
			exifValues += exifTags[11] + "='" + ei.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) + "',";
			exifValues += exifTags[12] + "='" + ei.getAttribute(ExifInterface.TAG_MAKE) + "',";
			exifValues += exifTags[13] + "='" + ei.getAttribute(ExifInterface.TAG_MODEL) + "',";
			exifValues += exifTags[14] + "='" + ei.getAttribute(ExifInterface.TAG_ORIENTATION) + "',";
			exifValues += exifTags[15] + "='" + ei.getAttribute(ExifInterface.TAG_WHITE_BALANCE) + "'";
			updateMetadata(CLONE_EXIF_DATA,exifValues,index);
			break;
		case RANDOMIZE_EXIF_DATA:
			// TODO: create a random string of realistic data and put it into the database
			break;
		case WIPE_EXIF_DATA:
			// set all values to null
			for(int x=0;x<exifTags.length;x++) {
				exifValues += exifTags[x] + "=null,";
			}
			exifValues = exifValues.substring(0,-1);
			ei.setAttribute(ExifInterface.TAG_DATETIME,null);
			ei.setAttribute(ExifInterface.TAG_FLASH,null);
			ei.setAttribute(ExifInterface.TAG_FOCAL_LENGTH,null);
			ei.setAttribute(ExifInterface.TAG_GPS_DATESTAMP,null);
			ei.setAttribute(ExifInterface.TAG_GPS_LATITUDE,null);
			ei.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF,null);
			ei.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,null);
			ei.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF,null);
			ei.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD,null);
			ei.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP,null);
			ei.setAttribute(ExifInterface.TAG_IMAGE_LENGTH,null);
			ei.setAttribute(ExifInterface.TAG_IMAGE_WIDTH,null);
			ei.setAttribute(ExifInterface.TAG_MAKE,null);
			ei.setAttribute(ExifInterface.TAG_MODEL,null);
			ei.setAttribute(ExifInterface.TAG_ORIENTATION,null);
			ei.setAttribute(ExifInterface.TAG_WHITE_BALANCE,null);
			updateMetadata(WIPE_EXIF_DATA,exifValues,index);
			break;
		}
		return exifValues;
	}
	
	public void updateMetadata(int target, String value, int index) {
		/* special batch updating is coded in the negatives
		 * otherwise, the integer passed is the column number
		 * and naturally pertains to the "main" camera_obscura table on the database.
		 */
		String theQuery;
		String tableName, key;
		switch(target) {
		case WIPE_EXIF_DATA:
			tableName = "camera_obscura";
			key = value;
			theQuery = "UPDATE " + tableName + " SET " + key + " WHERE _id = " + index; 
			break;
		case CLONE_EXIF_DATA:
			tableName = "camera_obscura";
			key = value;
			theQuery = "UPDATE " + tableName + " SET " + key + " WHERE _id = " + index;
			break;
		case RANDOMIZE_EXIF_DATA:
			tableName = "camera_obscura";
			key = value;
			theQuery = "UPDATE " + tableName + " SET " + key + " WHERE _id = " + index;
			break;
		default:
			tableName = "camera_obscura";
			key = "";
			Cursor dbNames = db.rawQuery("PRAGMA table_info(" + tableName + ")",null);
			if(dbNames != null) {
				key = dbNames.getColumnName(target);
			}
			dbNames.close();
			theQuery = "UPDATE " + tableName + " SET " + key + " = " + value + " WHERE _id = " + index;
			break;
		}
		db.execSQL(theQuery);
	}
	
	@Override
	public synchronized void close() {
		if(db != null) {
			db.close();
		}
		super.close();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

}