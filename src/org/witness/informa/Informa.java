package org.witness.informa;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.witness.sscphase1.ObscuraApp;

import flexjson.JSONSerializer;

import android.database.Cursor;
import android.provider.BaseColumns;
import android.util.Log;
import android.content.ContentValues;
import android.content.Context;
import info.guardianproject.database.sqlcipher.SQLiteDatabase;
import info.guardianproject.database.sqlcipher.SQLiteOpenHelper;

public class Informa {
	public Intent intent;
	public Geneaology geneaology;
	public Data data;
	
	private InformaDatabaseHelper idh;
	private SQLiteDatabase db;
	private String pw = "lalala123"; // we will fix this later...
	private Context _c;
	
	private static final String INFORMA_IMAGES = "informaImages";
	private static final String INFORMA_CONTACTS = "informaContacts";
	private static final String IRDB = "irdb";
	private static final String UNREDACTED = "unredacted";
	private static final String REDACTED = "redacted";
	private static final String PSEUDONYM = "pseudonym";
	
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
					"CREATE TABLE " + INFORMA_IMAGES + " (" + BaseColumns._ID + " integer primary key autoincrement, informa blob not null)",
					"CREATE TABLE " + INFORMA_CONTACTS + " (" + BaseColumns._ID + " integer primary key autoincrement, pseudonym text not null)",
					"CREATE TABLE " + IRDB + " (" + BaseColumns._ID + " integer primary key autoincrement, unredacted blob not null, redacted blob not null)"
				};
			}
		};
		
		public abstract String[] build();
	}
	
	public class Intent {
		Owner owner;
		String intendedDestination;
		int securityLevel;
		
		public Intent() {
			this.owner = new Owner();
			this.securityLevel = 1; // TODO: get answer from db
			this.intendedDestination = "PUBLIC-KEY-OF-SPONSORING-ORGANIZATION"; // TODO: get this from db
		}
	}
	
	public class Geneaology {
		String localMediaPath;
		long dateCreated, dateAcquired;
		
		public Geneaology(String localMediaPath, long dateCreated) {
			this.localMediaPath = localMediaPath;
			this.dateCreated = dateCreated;
		}
	}
	
	public class Data {
		int sourceType;
		String imageHash;
		Device device;
		
		Set<CaptureTimestamp> captureTimestamp;
		Set<Location> location;
		Set<Corroboration> corroboration;
		Set<ImageRegion> imageRegions;
		
		public Data(int sourceType, Device device) {
			this.sourceType = sourceType;
			this.device = device;
			
			this.imageRegions = new HashSet<ImageRegion>();
			this.location = new HashSet<Location>();
			this.corroboration = new HashSet<Corroboration>();
			this.captureTimestamp = new HashSet<CaptureTimestamp>();
		}
	}
	
	public class Location {
		int locationType;
		JSONObject locationData;
		
		public Location(int locationType, JSONObject locationData) {
			this.locationType = locationType;
			this.locationData = locationData;
		}
	}
	
	public class CaptureTimestamp {
		int timestampType;
		long timestamp;
		
		public CaptureTimestamp(int timestampType, long timestamp) {
			this.timestampType = timestampType;
			this.timestamp = timestamp;
			
		}
	}
	
	public class Owner {
		String ownerKey;
		int ownershipType;
		
		public Owner() {
			this.ownerKey = getPGPKey();
			this.ownershipType = getOwnershipType();
		}
		
		public String getPGPKey() {
			return "MY-PGP-KEY-GOES-HERE"; // TODO: hook into APG to return key
		}
		
		public int getOwnershipType() {
			return 25; // TODO: call db for this answer
		}
	}
	
	public class Device {
		String deviceKey, imei;
		Corroboration bluetoothInformation;
		
		public Device(JSONObject devicePackage) throws JSONException {
			int isSelf = -1;
			if(devicePackage.has("deviceIMEI"))
				this.imei = devicePackage.getString("deviceIMEI");
			else {
				this.imei = null;
				isSelf = 1;
			}
			
			this.bluetoothInformation = new Corroboration(
					devicePackage.getString("deviceBTAddress"), 
					devicePackage.getString("deviceBTName"),
					isSelf
			);
		}
	}
	
	public class Subject {
		String subjectName;
		boolean informedConsentGiven;
		int[] consentGiven;
		
		public Subject(String subjectName, boolean informedConsentGiven, int[] consentGiven) {
			this.subjectName = subjectName;
			this.informedConsentGiven = informedConsentGiven;
			this.consentGiven = consentGiven;
		}
	}
	
	public class Corroboration {
		String deviceBTAddress, deviceBTName;
		int selfOrNeighbor;	// -1 is self, 1 is neighbor
		
		public Corroboration(String deviceBTAddress, String deviceBTName, int selfOrNeighbor) {
			this.deviceBTAddress = deviceBTAddress;
			this.deviceBTName = deviceBTName;
			this.selfOrNeighbor = selfOrNeighbor;
		}
	}
	
	public class ImageRegion {
		CaptureTimestamp captureTimestamp;
		Location location;
		
		public String obfuscationType;
		String unredactedHash, processedHash;
		JSONObject regionDimensions, regionCoordinates;
		
		Subject subject;
				
		public ImageRegion(JSONObject ir) throws JSONException {
			// unpack the passed image region and fill in the blanks...
			this.captureTimestamp = new CaptureTimestamp(
					ObscuraApp.CAPTURE_EVENTS.RegionGenerated, ir.getLong("timestampOnGeneration"));
			this.location = new Location(
					ObscuraApp.LOCATION_TYPES.LocationOnGeneration, ir.getJSONObject("locationOnGeneration"));
			this.obfuscationType = ir.getString("obfuscationType");
			
			this.regionDimensions = new JSONObject();
			this.regionDimensions.put("width", Float.parseFloat(ir.getString("regionWidth")));
			this.regionDimensions.put("height", Float.parseFloat(ir.getString("regionHeight")));
			
			
			String[] rCoords = ir.getString("initialCoordinates").substring(1, ir.getString("initialCoordinates").length() - 1).split(",");
			this.regionCoordinates = new JSONObject();
			this.regionCoordinates.put("top", Float.parseFloat(rCoords[0]));
			this.regionCoordinates.put("left", Float.parseFloat(rCoords[1]));
			
			
			if(ir.has("regionSubject")) {
				this.subject = new Subject(
					ir.getString("regionSubject"),
					Boolean.parseBoolean(ir.getString("informedConsent")),
					new int[] {101});	// TODO: formalize the consent types
			} else {
				this.subject = null;
			}
			
			this.unredactedHash = this.processedHash = "blah-blah";
			// TODO: use jpegRedaction and APG to get unredactedHash and processedHash
		}
	}
	
	public Informa(Context c, JSONObject imageData, JSONArray regionData, JSONArray capturedEvents) throws Exception {
		_c = c;
		//idh = new InformaDatabaseHelper(_c);
		
		//SQLiteDatabase.loadLibs(_c);
		//db = idh.getWritableDatabase(pw);
		
		init(imageData, regionData, capturedEvents);
		renderAsJson();
		//renderToDatabase();
	}
	
	private void init(JSONObject imageData, JSONArray regionData, JSONArray capturedEvents) throws JSONException {
		intent = new Intent();
		geneaology = new Geneaology(
				imageData.getString("localMediaPath"),
				new Date().getTime());
		data = new Data(
				imageData.getInt("sourceType"),
				new Device((JSONObject) ((JSONObject) capturedEvents.get(0)).get("phone")));
		

		//geneaology.dateCreated = capturedEvents.getLong(2);
		Log.d(ObscuraApp.TAG, "unifying region data...");
		// associate the timestamp with the stored values here
		
		
		// grep storedRegionData for matching TS and attendant values
		for(int y=0;y< capturedEvents.length(); y++) {
			JSONObject rd = (JSONObject) capturedEvents.get(y);
			JSONObject geo = rd.getJSONObject("geo");
			JSONObject phone = rd.getJSONObject("phone");
			JSONObject acc = rd.getJSONObject("acc");
			
			switch(rd.getInt("captureEvent")) {
			case ObscuraApp.CAPTURE_EVENTS.MediaCaptured:
				
				JSONObject lomc = new JSONObject();
				lomc.put("gpsCoords", geo.getString("gpsCoords"));
				lomc.put("cellId", phone.getString("cellId"));
				lomc.put("lightMeter", acc.getJSONObject("light"));
				lomc.put("accelerometer", acc.getJSONObject("acc"));
				lomc.put("orientation", acc.getJSONObject("orientation"));
				
				data.location.add(new Location(rd.getInt("captureEvent"), lomc));	
				
				break;
			case ObscuraApp.CAPTURE_EVENTS.MediaSaved:
				
				JSONObject loms = new JSONObject();
				loms.put("gpsCoords", geo.getString("gpsCoords"));
				loms.put("cellId", phone.getString("cellId"));
				
				data.location.add(new Location(rd.getInt("captureEvent"), loms));
				
				break;
			case ObscuraApp.CAPTURE_EVENTS.RegionGenerated:
				
				for(int x=0; x< regionData.length(); x++) {
					JSONObject imageRegion = (JSONObject) regionData.get(x);
					long timestampToMatch = Long.parseLong(imageRegion.getString("timestampOnGeneration"));
					
					if(rd.getLong("timestamp") == timestampToMatch) {
						JSONObject locationOnGeneration = new JSONObject();
						locationOnGeneration.put("gpsCoords", geo.getString("gpsCoords"));
						locationOnGeneration.put("cellId", phone.getString("cellId"));
						
						imageRegion.put("locationOnGeneration", locationOnGeneration);
						Log.d(ObscuraApp.TAG, "region: " + imageRegion.toString());			
						data.imageRegions.add(new ImageRegion(imageRegion));			
					}
				}
				
				break;
			}
		}
		
	}
	
	public Object get(Object obj) {
		List<Class<?>> primativeClasses = Arrays.asList(new Class<?>[] {
				int.class,
				int[].class,
				String.class,
				String[].class,
				float.class,
				float[].class,
				long.class,
				long[].class,
				boolean.class,
				boolean[].class,
				Double.class,
				Double[].class				
		});
		
		JSONObject o = new JSONObject();
		Class<?> c = obj.getClass();
		Field[] fields = c.getDeclaredFields();
		
		for(Field f : fields) {
			f.setAccessible(true);
			
			try {
				Object value = f.get(obj);
				if(!primativeClasses.contains(f.getType())) {
					if(f.getType().equals(Set.class)) {
						Iterator i = ((Set) value).iterator();
						JSONArray j = new JSONArray();
						while(i.hasNext()) {
							Object set = i.next();
							j.put(get(set));
						}
						o.put(f.getName(), j);	
					} else if(!f.getType().equals(Informa.class)) {						
						Object b = get(value);
						if(b != null) {
							o.put(f.getName(), b);
						}
						
					}
				} else {
					o.put(f.getName(), value);
				}
			} catch (IllegalArgumentException e) {
				Log.d(ObscuraApp.TAG, e.getMessage());
			} catch (IllegalAccessException e) {
				Log.d(ObscuraApp.TAG, e.getMessage());
			} catch (SecurityException e) {
				Log.d(ObscuraApp.TAG, e.getMessage());
			} catch (JSONException e) {
				Log.d(ObscuraApp.TAG, e.getMessage());
			} catch (NullPointerException e) {
				Log.d(ObscuraApp.TAG, e.getMessage());
				try {
					o.put(f.getName(), "null");
				} catch (JSONException g) {
					Log.d(ObscuraApp.TAG, g.getMessage());
				}
			}
		}
		
		return o;
	}
	
	
	public JSONObject renderAsJson() throws JSONException {
		JSONObject sourceObject = new JSONObject();
		sourceObject.put("intent", (JSONObject) get(intent));
		sourceObject.put("geneaology", (JSONObject) get(geneaology));
		sourceObject.put("data", (JSONObject) get(data));
		
		Log.d(ObscuraApp.TAG, "INFORMA READOUT ****************\n" + sourceObject.toString());
		
		return sourceObject;
	}
	
	public String serializeObject(Object o) {
		return new JSONSerializer().serialize(o);
	}
	
	public long renderToDatabase() throws Exception {		
		ContentValues cv = new ContentValues();
		cv.put("hash", "yx0-xys");
		cv.put("content", this.renderAsJson().toString());
		cv.put("ts", this.geneaology.dateCreated);
		
		return db.insert(INFORMA_IMAGES, null, cv);
	}
}
