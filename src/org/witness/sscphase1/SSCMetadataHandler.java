package org.witness.sscphase1;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.ExifInterface;
import android.net.Uri;
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
				Log.v(SSC,"OK DB created!");
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
		boolean result = false;
		db = SQLiteDatabase.openDatabase(DB_PATH + DB_NAME, null, SQLiteDatabase.OPEN_READWRITE);
		if(db != null) {
			result = true;
		}
		return result;
	}
	
	public int insertIntoDatabase(String tableName, String targetColumn, String values) throws SQLException {
		String theQuery = "INSERT INTO " + tableName;
		if(targetColumn != null) {
			theQuery += targetColumn;
		}
		theQuery += " VALUES (" + values + ")";
		db.execSQL(theQuery);
		
		// sloppy way of returning last row affected
		int i = -1;
		Cursor dbCount = db.rawQuery("SELECT * FROM " + tableName,null);
		if(dbCount != null) {
			dbCount.moveToLast();
			i = dbCount.getPosition();
			Log.v(SSC,"DB COUNT = " + i);
		}
		dbCount.close();
		return i;
	}
	
	public Cursor readFromDatabase(String tableName,String refKey, String refVal) {
		Cursor dbResponse = null;
		dbResponse = db.rawQuery("SELECT * FROM " + tableName + " WHERE " + refKey + " = " + refVal,null);
		return dbResponse;
	}
	
	public void initiateMedia(Uri uriString, int mediaType) throws IOException {
		/*
		 *  this method requires the mediaType to be passed (image or video)
		 *  although we're only dealing with images now, I think we'll
		 *  have to branch off behaviors when we deal with video in the fuure
		 */
		this.uriString = uriString;
		this.uriPath = uriString.getPath();
		
		// insert initial info into database, creating a new record, then return its index.
		index = insertIntoDatabase("camera_obscura","(g_localMediaPath)","\"" + uriString.toString() + "\"");
		
		switch(mediaType) {
		case 1:
			// image
			try {
				ExifInterface ei = new ExifInterface(uriPath);
				Log.v(SSC,"SHOWING: " + exifDump(ei));
			} catch (IOException e) {
				Log.d(SSC,"ioexception : " + e);
			}
			break;
		case 2:
			break;
		}
	}
	
	public String exifDump(ExifInterface ei) {
		// in testing, these values are all null.  why?
		String response = "EXIF:\n";
		String[] exifTags = c.getResources().getStringArray(R.array.ExifTags);
		String[] updateValues = new String[exifTags.length];
		for(int x=0;x<exifTags.length;x++) {
			response += ("tag: " + exifTags[x] + ", value: " + ei.getAttribute(exifTags[x]) + "\n");
			updateValues[x] = ei.getAttribute(exifTags[x]);
		}
		updateMetadata(CLONE_EXIF_DATA,updateValues,index);
		return response;
	}
	
	public void exifWipe(ExifInterface ei) {
		// should Toast to user that this cannot be undone!
		String[] exifTags = c.getResources().getStringArray(R.array.ExifTags);
		for(int x=0;x<exifTags.length;x++) {
			ei.setAttribute(exifTags[x],null);
		}
		updateMetadata(WIPE_EXIF_DATA,null,index);
	}
	
	public void updateMetadata(int target, String[] value, int index) {
		/* special batch updating is coded in the negatives
		 * otherwise, the integer passed is the column number
		 * and naturally pertains to the "main" camera_obscura table on the database.
		 */
		String theQuery;
		String tableName, key;
		String[] exifTags = c.getResources().getStringArray(R.array.ExifTagsDB);
		int x;
		switch(target) {
		case WIPE_EXIF_DATA:
			tableName = "camera_obscura";
			key = "";
			for(x=0;x<exifTags.length;x++) {
				key += (exifTags[x] + " = null,"); 
			}
			key = key.substring(0,key.length() - 1);
			theQuery = "UPDATE " + tableName + " SET " + key + " WHERE _id = " + index; 
			break;
		case CLONE_EXIF_DATA:
			tableName = "camera_obscura";
			key = "";
			for(x=0;x<exifTags.length;x++) {
				key += (exifTags[x] + " = " + value[x] + ",");
			}
			key = key.substring(0,key.length() -1);
			theQuery = "UPDATE " + tableName + " SET " + key + " WHERE _id = " + index;
			break;
		default:
			tableName = "camera_obscura";
			key = "";
			Cursor dbNames = db.rawQuery("PRAGMA table_info(" + tableName + ")",null);
			if(dbNames != null) {
				key = dbNames.getColumnName(target);
			}
			theQuery = "UPDATE " + tableName + " SET " + key + " = " + value[0] + " WHERE _id = " + index;
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
