package org.witness.informa;

import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.witness.sscphase1.ObscuraApp;

import android.database.Cursor;
import android.provider.BaseColumns;
import android.util.Log;
import android.content.Context;
import info.guardianproject.database.sqlcipher.SQLiteDatabase;
import info.guardianproject.database.sqlcipher.SQLiteOpenHelper;

public class Informa {
	Intent intent;
	Geneaology geneaology;
	Data data;
	
	private InformaDatabaseHelper idh;
	private SQLiteDatabase db;
	private String pw = "lalala123"; // we will fix this later...
	Context _c;
	
	private class InformaDatabaseHelper extends SQLiteOpenHelper {
		private static final String DATABASE_NAME = "informa.db";
		private static final int DATABASE_VERSION = 1;
		
		public InformaDatabaseHelper(Context c) {
			super(c, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			for(String query : QueryBuilders.INIT.build())
				db.execSQL(query);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
	}
	
	public enum QueryBuilders {
		INIT() {
			@Override
			public String[] build() {
				return new String[] {
					"CREATE TABLE InformaImages(" + BaseColumns._ID + " integer primary key autoincrement, informa blob not null)",
					"CREATE TABLE InformaContacts(" + BaseColumns._ID + " integer primary key autoincrement, pseudonym text not null)",
					"CREATE TABLE IRDB(" + BaseColumns._ID + " integer primary key autoincrement, unredacted blob not null, redacted blob not null)"
				};
			}
		};
		
		public abstract String[] build();
	}
	
	private class Intent {
		Owner owner;
		int securityLevel;
		
		private Intent() {
			owner = new Owner();
			securityLevel = 1; // TODO: get answer from db
		}
	}
	
	private class Geneaology {
		String localMediaPath, intendedDestination;
		long dateCreated, dateAcquired;
		
		private Geneaology() {
			intendedDestination = "PUBLIC-KEY-OF-SPONSORING-ORGANIZATION"; // TODO: get this from db
		}
	}
	
	private class Data {
		int sourceType;
		
		String imageHash;
		Vector<CaptureTimestamp> captureTimestamp;
		Device device;
		
		Vector<Location> location;
		Vector<Corroboration> corroboration;
		
		ImageRegions imageRegions;
		
		private Data() {
			imageRegions = new ImageRegions();
		}
	}
	
	private class Location {
		
		public Location(int locTypes, JSONObject locationBundle) {
			
			
		}
	}
	
	private class CaptureTimestamp {
		
		public CaptureTimestamp(int timestampTypes, long timestamp) {
			
			
		}
	}
	
	private class Owner {
		String ownerKey;
		int ownershipType;
		
		public Owner() {
			ownerKey = getPGPKey();
			ownershipType = getOwnershipType();
		}
		
		private String getPGPKey() {
			return "MY-PGP-KEY-GOES-HERE"; // TODO: hook into APG to return key
		}
		
		private int getOwnershipType() {
			return 25; // TODO: call db for this answer
		}
	}
	
	private class Device {
		String deviceKey, imei;
		Corroboration bluetoothInformation;
		
		public Device(JSONObject devicePackage) throws JSONException {
			imei = devicePackage.getString("deviceIMEI");
			bluetoothInformation = new Corroboration(
					devicePackage.getString("deviceBTAddress"), 
					devicePackage.getString("deviceBTName"),
					-1
			);
		}
	}
	
	private class Subject {
		String subjectName;
		boolean informedConsentGiven;
		int[] consentGiven;
	}
	
	private class Corroboration {
		String _deviceBTAddress, _deviceBTName;
		int _selfOrNeighbor;
		
		public Corroboration(String deviceBTAddress, String deviceBTName, int selfOrNeighbor) {
			_deviceBTAddress = deviceBTAddress;
			_deviceBTName = deviceBTName;
			_selfOrNeighbor = selfOrNeighbor;
		}
	}
	
	private class ImageRegion {
		CaptureTimestamp captureTimestamp;
		Location location;
		
		String obfuscationType, unredactedHash, processedHash;
		float[] regionDimensions, regionCoordinates;
		
		Subject subject;
				
		public ImageRegion(JSONObject ir) {
			// unpack the passed image region and fill in the blanks...
			
		}
	}
	
	private class ImageRegions extends ArrayList<ImageRegion> {
		private static final long serialVersionUID = 1L;		
	}
	
	public Informa(Context c, JSONObject imageData, JSONArray regionData, JSONArray capturedEvents) throws JSONException {
		_c = c;
		idh = new InformaDatabaseHelper(_c);
		
		SQLiteDatabase.loadLibs(_c);
		db = idh.getWritableDatabase(pw);
		
		init(imageData, regionData, capturedEvents);
	}
	
	private void init(JSONObject imageData, JSONArray regionData, JSONArray capturedEvents) throws JSONException {
		intent = new Intent();
		geneaology = new Geneaology();
		data = new Data();
		
		// input the values that get passed in...
		geneaology.localMediaPath = (String) imageData.get("localMediaPath");
		geneaology.dateAcquired = new Date().getTime();
		//geneaology.dateCreated = capturedEvents.getLong(2);
		
		
		data.sourceType = (Integer) imageData.get("sourceType");
		data.device = new Device((JSONObject) ((JSONObject) capturedEvents.get(0)).get("phone"));
		
		for(int x=0; x< regionData.length(); x++) {
			// associate the timestamp with the stored values here
			JSONObject imageRegion = (JSONObject) regionData.get(x);
			long timestampToMatch = Long.parseLong(imageRegion.getString("timestampOnGeneration"));
						
			// grep storedRegionData for matching TS and attendant values
			for(int y=0;y< capturedEvents.length(); y++) {
				JSONObject rd = (JSONObject) capturedEvents.get(y);
				if(rd.getLong("timestamp") == timestampToMatch) {
					JSONObject geo = rd.getJSONObject("geo");
					JSONObject phone = rd.getJSONObject("phone");
					
					JSONObject locationOnGeneration = new JSONObject();
					locationOnGeneration.put("gpsCoords", geo.getString("gpsCoords"));
					locationOnGeneration.put("cellId", phone.getString("cellId"));
					
					rd.put("locationOnGeneration", locationOnGeneration);
								
					data.imageRegions.add(new ImageRegion(rd));			
					
				}
								
			}
		}
		
	}
	
	public JSONObject renderAsJson() {
		JSONObject render = new JSONObject();
		
		return render;
	}
	
	public Cursor renderToDatabase() {
		Cursor cursor = null;
		
		return cursor;
	}
}
