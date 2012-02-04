package org.witness.informa;

import info.guardianproject.database.sqlcipher.SQLiteDatabase;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.Blob;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.witness.informa.utils.InformaConstants;
import org.witness.informa.utils.InformaConstants.CaptureEvents;
import org.witness.informa.utils.InformaConstants.Keys;
import org.witness.informa.utils.InformaConstants.Keys.Service;
import org.witness.informa.utils.io.DatabaseHelper;
import org.witness.informa.utils.secure.Apg;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;

public class Informa {
	public Intent intent;
	public Genealogy genealogy;
	public Data data;
	
	private SQLiteDatabase db;
	private DatabaseHelper dh;
	private Apg apg;
	private Image[] images;
	
	private Context c;
	private Activity a;
	
	private class InformaZipper extends JSONObject {
		Field[] fields;
		
		public InformaZipper() {
			fields = this.getClass().getDeclaredFields();
		}
		
		public JSONObject zip() throws IllegalArgumentException, IllegalAccessException, JSONException {
			for(Field f : fields) {
				f.setAccessible(true);
				Object value = f.get(this);
				
				if(!(value instanceof Informa)) {
					if(value instanceof InformaZipper)
						this.put(f.getName(), ((InformaZipper) value).zip());
					else if(value instanceof Set) {
						Iterator<?> i = ((Set<?>) value).iterator();
						JSONArray j = new JSONArray();
						while(i.hasNext())
							j.put(((InformaZipper) i.next()).zip());
						this.put(f.getName(), j);
					} else
						this.put(f.getName(), value);
				}
			}
			
			return this;
		}
	}
	
	public class Intent extends InformaZipper {
		Owner owner;
		String intendedDestination;
		int securityLevel;
		
		public Intent(String intendedDestination) {
			this.owner = new Owner();
			this.securityLevel = (Integer) getDBValue(Keys.Tables.SETUP, new String[] {Keys.Owner.DEFAULT_SECURITY_LEVEL}, BaseColumns._ID, 1, Integer.class);
			this.intendedDestination = intendedDestination;
		}
	}
	
	public class Genealogy extends InformaZipper {
		String localMediaPath;
		long dateCreated, dateAcquired;
		
		public Genealogy(String localMediaPath, long dateCreated) {
			this.localMediaPath = localMediaPath;
			this.dateCreated = dateCreated;
			this.dateAcquired = dateCreated; // TODO: set us up the date acquired via imageeditor...
		}
	}
	
	public class Data extends InformaZipper {
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
			this.imageRegions = new HashSet<ImageRegion>();
		}
	}
	
	public class Device extends InformaZipper {
		String imei;
		Corroboration bluetoothInfo;
		
		public Device(JSONObject devicePackage) throws JSONException {
			int isSelf = InformaConstants.Device.IS_SELF;
			if(devicePackage.has(Keys.Device.IMEI))
				this.imei = devicePackage.getString(Keys.Device.IMEI);
			else {
				this.imei = null;
				isSelf = InformaConstants.Device.IS_NEIGHBOR;
			}
			
			this.bluetoothInfo = new Corroboration(
					devicePackage.getString(Keys.Device.BLUETOOTH_DEVICE_ADDRESS),
					devicePackage.getString(Keys.Device.BLUETOOTH_DEVICE_NAME),
					isSelf);
		}
	}
	
	public class CaptureTimestamp extends InformaZipper {
		int timestampType;
		long timestamp;
		
		public CaptureTimestamp(int timestampType, long timestamp) {
			this.timestampType = timestampType;
			this.timestamp = timestamp;
		}
	}
	
	public class Location extends InformaZipper {
		int locationType;
		JSONObject locationData;
		
		public Location(int locationType, JSONObject locationData) {
			this.locationType = locationType;
			this.locationData = locationData;
		}
	}
	
	public class Corroboration extends InformaZipper {
		String deviceBTAddress, deviceBTName;
		int selfOrNeighbor;
		
		public Corroboration(String deviceBTAddress, String deviceBTName, int selfOrNeighbor) {
			this.deviceBTAddress = deviceBTAddress;
			this.deviceBTName = deviceBTName;
			this.selfOrNeighbor = selfOrNeighbor;
		}
	}
	
	public class Subject extends InformaZipper {
		String subjectName;
		boolean informedConsentGiven;
		int[] consentGiven;
		
		public Subject(String subjectName, boolean informedConsentGiven, int[] consentGiven) {
			this.subjectName = subjectName;
			this.informedConsentGiven = informedConsentGiven;
			this.consentGiven = consentGiven;
		}
	}
	
	public class ImageRegion extends InformaZipper {
		CaptureTimestamp captureTimestamp;
		Location location;
		
		public String obfuscationType, unredactedRegion;
		JSONObject regionDimensions, regionCoordinates;
		
		Subject subject;
		
		public ImageRegion(JSONObject ir) throws JSONException {
			this.captureTimestamp = new CaptureTimestamp(CaptureEvents.REGION_GENERATED, ir.getLong(Keys.ImageRegion.TIMESTAMP));
			this.location = new Location(InformaConstants.LocationTypes.ON_REGION_GENERATED, ir.getJSONObject(Keys.Location.COORDINATES));
			this.obfuscationType = ir.getString(Keys.ImageRegion.FILTER);
			
			if(ir.has(Keys.ImageRegion.UNREDACTED_HASH))
				this.unredactedRegion = ir.getString(Keys.ImageRegion.UNREDACTED_HASH);
			else
				this.unredactedRegion = null;
			
			this.regionDimensions = new JSONObject();
			this.regionDimensions.put(Keys.ImageRegion.WIDTH, Float.parseFloat(ir.getString(Keys.ImageRegion.WIDTH)));
			this.regionDimensions.put(Keys.ImageRegion.HEIGHT, Float.parseFloat(ir.getString(Keys.ImageRegion.HEIGHT)));
			
			String[] rCoords = ir.getString(Keys.ImageRegion.COORDINATES).substring(1, ir.getString(Keys.ImageRegion.COORDINATES).length() -1).split(",");
			this.regionCoordinates = new JSONObject();
			this.regionCoordinates.put(Keys.ImageRegion.TOP, Integer.parseInt(rCoords[0]));
			this.regionCoordinates.put(Keys.ImageRegion.LEFT, Integer.parseInt(rCoords[1]));
			
			if(ir.has(Keys.ImageRegion.Subject.PSEUDONYM)) {
				this.subject = new Subject(
						ir.getString(Keys.ImageRegion.Subject.PSEUDONYM),
						Boolean.parseBoolean(ir.getString(Keys.ImageRegion.Subject.INFORMED_CONSENT_GIVEN)),
						new int[] {InformaConstants.Consent.GENERAL});
			} else
				this.subject = null;
			
		}
	}
	
	public class Owner extends InformaZipper {
		String sigKeyId;
		int ownershipType;
		
		public Owner() {
			this.sigKeyId = getAPGEmail(apg.getSignatureKeyId());
			this.ownershipType = (Integer) getDBValue(Keys.Tables.SETUP, new String[] {Keys.Owner.OWNERSHIP_TYPE}, BaseColumns._ID, 1, Integer.class);
		}
	}
	
	private class Image extends File {
		private static final long serialVersionUID = 1L;
		private JSONObject json;
		private String encrypedVersionPath, intendedDestination;

		public Image(String path, JSONObject imageData, JSONArray regionData, JSONArray capturedEvents, String intendedDestination) throws JSONException, IllegalArgumentException, IllegalAccessException {
			super(path);
			
			this.intendedDestination = intendedDestination;
			
			intent = new Intent(intendedDestination);
			genealogy = new Genealogy(path, System.currentTimeMillis());
			data = new Data(
					imageData.getInt(Keys.Image.MEDIA_TYPE), 
					new Device((JSONObject) ((JSONObject) capturedEvents.get(0)).get(Keys.Suckers.PHONE)));
			
			for(int c=0; c<capturedEvents.length(); c++) {
				JSONObject rd = (JSONObject) capturedEvents.get(c);
				JSONObject geo = rd.getJSONObject(Keys.Suckers.GEO);
				JSONObject phone = rd.getJSONObject(Keys.Suckers.PHONE);
				JSONObject acc = rd.getJSONObject(Keys.Suckers.ACCELEROMETER);
				
				switch(rd.getInt(Keys.CaptureEvent.TYPE)) {
				case CaptureEvents.MEDIA_CAPTURED:
					JSONObject lomc = new JSONObject();
					lomc.put(Keys.Location.COORDINATES, geo.getString(Keys.Suckers.Geo.GPS_COORDS));
					lomc.put(Keys.Location.CELL_ID, phone.getString(Keys.Suckers.Phone.CELL_ID));
					lomc.put(Keys.Suckers.Accelerometer.LIGHT, acc.getJSONObject(Keys.Suckers.Accelerometer.LIGHT_METER_VALUE));
					lomc.put(Keys.Suckers.Accelerometer.ACC, acc.getJSONObject(Keys.Suckers.Accelerometer.ACC));
					lomc.put(Keys.Suckers.Accelerometer.ORIENTATION, acc.getJSONObject(Keys.Suckers.Accelerometer.ORIENTATION));
					break;
				case CaptureEvents.MEDIA_SAVED:
					JSONObject loms = new JSONObject();
					loms.put(Keys.Location.COORDINATES, geo.getString(Keys.Suckers.Geo.GPS_COORDS));
					loms.put(Keys.Location.CELL_ID, phone.getString(Keys.Suckers.Phone.CELL_ID));
					data.location.add(new Location(rd.getInt(Keys.CaptureEvent.TYPE), loms));
					break;
				case CaptureEvents.REGION_GENERATED:
					for(int x=0; x< regionData.length(); x++) {
						JSONObject imageRegion = (JSONObject) regionData.get(x);
						/*
						 *  TODO: make call to jpeg redaction object to get unredacted strips
						 *  add them to metadata block
						 */
						
						long timestampToMatch = Long.parseLong(imageRegion.getString(Keys.CaptureEvent.MATCH_TIMESTAMP));
						if(rd.getLong(Keys.CaptureEvent.TIMESTAMP) == timestampToMatch) {
							JSONObject log = new JSONObject();
							log.put(Keys.Location.COORDINATES, geo.getString(Keys.Suckers.Geo.GPS_COORDS));
							log.put(Keys.Location.CELL_ID, phone.getString(Keys.Suckers.Phone.CELL_ID));
							imageRegion.put(Keys.ImageRegion.LOCATION, log);
							data.imageRegions.add(new ImageRegion(imageRegion));
						}
					}
					break;
				}
			}
			
			json = new JSONObject();
			json.put(Keys.Informa.INTENT, intent.zip());
			json.put(Keys.Informa.GENEALOGY, genealogy.zip());
			json.put(Keys.Informa.DATA, data.zip());
			Log.d(InformaConstants.READOUT, json.toString());
						
		}
		
		public String getIntendedDestination() {
			return this.intendedDestination;
		}
		
		public String getEncryptedVersionPath() {
			return this.encrypedVersionPath;
		}
		
	}
	
	private Object getDBValue(String table, String[] key, String matchKey, Object matchValue, Class<?> expectedType) {
		dh.setTable(db, table);
		Cursor c = dh.getValue(db, key, matchKey, matchValue);
		//"SELECT " + select + " FROM " + getTable() + " WHERE " + matchKey + " = " + matchValue, null);
		Object value = new Object();
		if(c != null) {
			if(expectedType.equals(Long.class))
				value = c.getLong(0);
			else if(expectedType.equals(String.class))
				value = c.getString(0);
			else if(expectedType.equals(Integer.class))
				value = c.getInt(0);
			else if(expectedType.equals(Blob.class))
				value = c.getBlob(0);
		}
		Log.d(InformaConstants.TAG, value.toString());
		return value;
	}
	
	private String getAPGEmail(long keyId) {
		return apg.getPublicUserId(c, keyId);
	}
	
	public Informa(
			Activity a,
			JSONObject imageData, 
			JSONArray regionData, 
			JSONArray capturedEvents, 
			String[] intendedDestinations) throws IllegalArgumentException, JSONException, IllegalAccessException {
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
		this.a = a;
		c = a.getApplicationContext();
		
		dh = new DatabaseHelper(c);
		db = dh.getWritableDatabase(sp.getString(Keys.Settings.HAS_DB_PASSWORD, ""));
		ContentValues cv = new ContentValues();
		
		apg = Apg.getInstance();
		apg.setSignatureKeyId((Long) getDBValue(Keys.Tables.SETUP, new String[] {Keys.Owner.SIG_KEY_ID}, BaseColumns._ID, 1, String.class));
		
		images = new Image[intendedDestinations.length];
		for(int i = 0; i<intendedDestinations.length; i++) {
			images[i] = new Image(
					imageData.getString(Keys.Image.LOCAL_MEDIA_PATH), 
					imageData, 
					regionData, 
					capturedEvents, 
					intendedDestinations[i]);
		}
		
		
	}
}
