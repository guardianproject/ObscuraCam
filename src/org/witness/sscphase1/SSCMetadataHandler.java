package org.witness.sscphase1;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

import com.google.gson.Gson;

public class SSCMetadataHandler extends SQLiteOpenHelper {
	private static final String DB_PATH = "/data/data/org.witness.sscphase1/databases/";
	private static final String DB_NAME = "camera_obscura.db";
	private SQLiteDatabase db;
	private final Context c;
	
	public Uri uriString;
	public String uriPath;
	public int index;
	private String sscImageSerial;
	
	public ExifInterface ei;
	
	private String ownerName;
	private int defaultExifPreference;
	private int ownershipType;
	private int securityLevel;
	private String publicKey;
	private int sociallySharable;
	
	private static final String CAMERAOBSCURA = "camera_obscura";
	private static final String KNOWNSUBJECTS = "known_subjects";
	private static final int CLONE_EXIF = 1;
	private static final int RANDOMIZE_EXIF = 2;
	private static final int WIPE_EXIF = 3;
	
	private String[] exifAttributes = {
			ExifInterface.TAG_DATETIME,
			ExifInterface.TAG_FLASH,
			ExifInterface.TAG_FOCAL_LENGTH,
			ExifInterface.TAG_GPS_DATESTAMP,
			ExifInterface.TAG_GPS_LATITUDE,
			ExifInterface.TAG_GPS_LATITUDE_REF,
			ExifInterface.TAG_GPS_LONGITUDE,
			ExifInterface.TAG_GPS_LONGITUDE_REF,
			ExifInterface.TAG_GPS_PROCESSING_METHOD,
			ExifInterface.TAG_GPS_TIMESTAMP,
			ExifInterface.TAG_IMAGE_LENGTH,
			ExifInterface.TAG_IMAGE_WIDTH,
			ExifInterface.TAG_MAKE,
			ExifInterface.TAG_MODEL,
			ExifInterface.TAG_ORIENTATION,
			ExifInterface.TAG_WHITE_BALANCE
	};
	private String[] exifTableValues = {"tagDateTime",
			"tagFlash","tagFocalLength","tagGPSDateStamp",
			"tagGPSLatitude","tagGPSLatitudeRef","tagGPSLongitude",
			"tagLongitudeRef","tagGPSProcessingMethod","tagGPSTimeStamp",
			"tagImageLength","tagImageWidth","tagMake","tagModel",
			"tagOrientation","tagWhiteBalance"};
	
	private String[] dataTable = {"tagDateTime TEXT",
			"tagFlash TEXT","tagFocalLength TEXT","tagGPSDateStamp TEXT",
			"tagGPSLatitude TEXT","tagGPSLatitudeRef TEXT","tagGPSLongitude TEXT",
			"tagLongitudeRef TEXT","tagGPSProcessingMethod TEXT","tagGPSTimeStamp TEXT",
			"tagImageLength TEXT","tagImageWidth TEXT","tagMake TEXT","tagModel TEXT",
			"tagOrientation TEXT","tagWhiteBalance TEXT","bluetoothNeighbors TEXT","accelerometerAxisAmount INTEGER",
			"accelerometerAxisInitialX TEXT","accelerometerAxisInitialY TEXT","accelerometerAxisInitialZ TEXT",
			"videoDuration INTEGER","numVideoPaths INTEGER","associatedImageRegions TEXT"};
	private String[] associatedImageRegionTable = {"associatedImageRegion TEXT"};
	private String[] imageRegionTable = {"coordinates TEXT","subjectSerial TEXT","informedConsentGiven INTEGER","consentTimeCodeserial TEXT",
			"obfuscationType INTEGER","obfuscationTimeblock TEXT","associatedKeys TEXT","serial TEXT"};
	private String[] subjectTable = {"associatedImages TEXT"};
	
	private static final String SSC = "[Camera Obscura : SSCMetadataHandler] ****************************";

	public SSCMetadataHandler(Context context) {
		super(context, DB_NAME, null, 1);
		this.c = context;
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
		/* this method creates a new media entry in the user's main table with the following:
		 * 
		 * media store URI
		 * owner name/id [taken from preferences?]
		 * owner type [individual or organization]
		 * public key of owner
		 * default sharing preference [taken from preferences]
		 * origin of media: taken by camera, imported from gallery
		 * timestamp
		 * make/model of device that captured the media (often the phone itself, but read the exif data)
		 * default security [taken from preferences]
		 */
		
		/*
		 *  this method requires the mediaType to be passed (image or video)
		 *  although we're only dealing with images now, I think we'll
		 *  have to branch off behaviors when we deal with video in the future
		 */
		// TODO: interface with preferences to get taken-for-granted data
		this.defaultExifPreference = CLONE_EXIF;
		this.ownerName = "OWNERNAME";
		this.ownershipType = 1;
		this.securityLevel = 3;
		this.sociallySharable = 1;
		this.publicKey = "XXX";
		this.uriString = uriString;
		
		switch(imageSource) {
		// the uriPath will be different, depending on the source of the image...
		case 1:
			this.uriPath = uriString.getPath();
			break;
		case 2:
			this.uriPath = getFileNameFromUri(uriString);
			break;
		}
		
		this.sscImageSerial = makeHash(ownerName + System.currentTimeMillis());

		// insert initial info into database, creating a new record, then return its index.
		// TODO: Link this data up with the preferences stuff...
		String[] tableNames = {"localMediaPath",
				"ownerName",
				"ownershipType",
				"securityLevel",
				"publicKey",
				"sociallySharable",
				"serial"};
		String[] tableValues = {uriString.toString(),
				ownerName,
				Integer.toString(ownershipType),
				Integer.toString(securityLevel),
				publicKey,
				Integer.toString(sociallySharable),
				sscImageSerial};
		this.index = insertIntoDatabase(CAMERAOBSCURA,tableNames,tableValues);
		
		// spawns a new table containing corresponding available data (EXIF, BT, ACC, Geo, and reference to image regions);
		createTable(sscImageSerial,dataTable);
		insertIntoDatabase(sscImageSerial, exifTableValues, writeExif(defaultExifPreference));
		modifyRecord(sscImageSerial,"associatedImageRegions",makeHash(sscImageSerial + "_IR"),"_id",Integer.toString(0));
		
		// spawns a new table for the associated image regions, and updates the entry in the database pointing to it
		String ir = makeHash(sscImageSerial + "_IR");
		createTable(ir,imageRegionTable);
	}

	public int getImageResourceCursor() {
		return index;
	}
	
	public String[] writeExif(int intent) {
		/*
		 *  this method will make the specified modifications to the exif data
		 *  
		 *  this method takes a parameter: intent to determine whether the exif data will be:
		 *  1. genuinely passed on from the media,
		 *  2. recorded from user-input values,
		 *  3. or wiped entirely (values set to null)
		 */
		String[] newExifValues = new String[exifAttributes.length];
		try {
			ei = new ExifInterface(uriPath);
		} catch (IOException e) {}
		
		int c = 0;
		switch(intent) {
		case CLONE_EXIF:
			newExifValues = readExif(ei);
			break;
		case RANDOMIZE_EXIF:
			break;
		case WIPE_EXIF:
			for(String nef : exifAttributes) {
				newExifValues[c] = "0";
				ei.setAttribute(nef,newExifValues[c]);
				c++;
			}
		}
		return newExifValues;
	}
	
	public String[] readExif(ExifInterface ei) {
		/*
		 * just a method for reading what's currently attached to the media and returning it.
		 */
		String[] exifData = new String[exifAttributes.length];
		int c = 0;
		for(String ea : exifAttributes) {
			exifData[c] = ei.getAttribute(ea);
			c++;
		}
		return exifData;
	}
	
	public void registerSensorData() {
		/*
		 * this method will write the associated sensory data to the same table containing the exif data
		 */
		
	}
	
	public void registerKeys() {
		
	}
	
	public void registerSubject() {
		/*
		 * this method will write data regarding a subject to a join table that can be associated with the image
		 * 
		 * if the corresponding subject join table does not exist, it will create it and name
		 * it according to our taxonomy
		 * 
		 * this method populates the subject join table with the following:
		 * 
		 * Subject Name
		 * Consent
		 * Consent timecode (null if image)
		 * 
		 * this method will also spawn a new join table named after this subject (a hash of the name, i would say)
		 * or append to it, in the case that the table already exists with
		 * the ID of corresponding image (imageRegion?)
		 * 
		 */
	}
	
	public void registerImageRegion(ImageRegion imageRegion) {
		// TODO: first, check to see if this image region exists.
		// insert into _ir database with known values
		String[] tableNames = {"coordinates","serial"};
		float[] coordinates = {
				imageRegion.startX,
				imageRegion.startY,
				imageRegion.endX,
				imageRegion.endY};
		String[] tableValues = {gsonPack(coordinates),imageRegion.toString()};
		insertIntoDatabase(makeHash(sscImageSerial + "_IR"),tableNames,tableValues);
	}
	
	public int insertIntoDatabase(String tableName, String[] targetColumns, String[] values) throws SQLException {
		String theQuery = "INSERT INTO " + tableName + " (";
		if(targetColumns != null) {
			StringBuffer sb = new StringBuffer();
			for(String tc : targetColumns) {
				sb.append(",");
				sb.append(tc);
			}
			theQuery += (sb.toString().substring(1) + ") ");
		}
		StringBuffer sb = new StringBuffer();
		for(String val : values) {
			sb.append(",");
			sb.append("\'" + val + "\'");
		}
		theQuery += "VALUES (" + sb.toString().substring(1) + ")";
		Log.d(SSC,theQuery);
		db.execSQL(theQuery);
		
		// return last row affected
		int i = -1;
		Cursor dbCount = db.rawQuery("SELECT * FROM " + tableName,null);
		if(dbCount != null) {
			dbCount.moveToLast();
			i = dbCount.getPosition();
			dbCount.close();
		}
		return i;
	}
	
	// SINGULAR
	private void modifyRecord(String tableName, String targetColumn,
			String value, String matchColumn, String matchValue) {
		String theQuery = "UPDATE " + tableName + 
							" SET " + targetColumn + 
							" = \'" + value + "\' WHERE " +
							matchColumn + " = " + matchValue; 
		Log.d(SSC,theQuery);
		db.execSQL(theQuery);
	}
	
	// PLURAL (overloaded)
	public void modifyRecord(String tableName, String[] targetColumns, String[] values, String matchColumn, String matchValue) {
		String theQuery = "UPDATE " + tableName + " SET ";
		StringBuffer sb = new StringBuffer();
		for(int x=0; x<targetColumns.length; x++) {
			sb.append(",");
			sb.append(targetColumns[x] + " = \'" + values[x] + "\'");
		}
		theQuery += sb.toString().substring(1) + " WHERE " + matchColumn + " = " + matchValue; 
		Log.d(SSC,theQuery);
		db.execSQL(theQuery);
	}
	
	public void createTable(String tableName, String[] columns) {
		String theQuery = "CREATE TABLE " + tableName + " (_id INTEGER PRIMARY KEY,";
		StringBuffer sb = new StringBuffer();
		for(String col : columns) {
			sb.append(",");
			sb.append(col);
		}
		theQuery += sb.toString().substring(1) + ")";
		Log.d(SSC,theQuery);
		db.execSQL(theQuery);
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
	
	private String gsonPack(float[] values) {
		Gson gson = new Gson();
		return gson.toJson(values);
	}
	
	private String makeHash(String s) {
		String theHash = null;
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			Log.d(SSC,"NO SUCH ALGO: " + e);
		}
		md.update(s.getBytes());
		byte b[] = md.digest();
		StringBuffer sb = new StringBuffer();
		for(int x=0;x<b.length;x++) {
			sb.append(Integer.toHexString(0xFF & b[x]));
		}
		theHash = "_" + sb.toString();
		Log.d(SSC,s + " becomes " + theHash);
		return theHash; 
	}
}