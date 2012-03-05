package org.witness.informa;

import info.guardianproject.database.sqlcipher.SQLiteDatabase;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
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
import org.witness.informa.utils.InformaConstants.Keys.Tables;
import org.witness.informa.utils.InformaConstants.Keys.TrustedDestinations;
import org.witness.informa.utils.InformaConstants.MediaTypes;
import org.witness.informa.utils.io.DatabaseHelper;
import org.witness.informa.utils.secure.Apg;
import org.witness.informa.utils.secure.MediaHasher;
import org.witness.securesmartcam.utils.Selections;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.ExifInterface;
import android.os.Build;
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
	
	private Context _c;
	
	private class InformaZipper extends JSONObject {
		Field[] fields;
		
		public InformaZipper() {
			fields = this.getClass().getDeclaredFields();
		}
		
		public JSONObject zip() throws IllegalArgumentException, IllegalAccessException, JSONException {
			for(Field f : fields) {
				f.setAccessible(true);
				Object value = f.get(this);
				
				if(!(value instanceof Informa)  && !(value instanceof Image)) {
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
			this.dateAcquired = dateCreated; 
			// TODO: set us up the date acquired via imageeditor...
		}
	}
	
	public class Data extends InformaZipper {
		int sourceType;
		String imageHash;
		Device device;
		Exif exif;
		
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
	
	public class Exif extends InformaZipper {
		int sdk, orientation, imageLength, imageWidth, whiteBalance, flash, focalLength;
		String make, model, iso, exposureTime, aperture;
		
		public Exif(JSONObject exif) throws JSONException {
			this.sdk = Build.VERSION.SDK_INT;
			if(exif.has(Keys.Exif.MAKE))
				this.make = exif.getString(Keys.Exif.MAKE);
			if(exif.has(Keys.Exif.MODEL))
				this.model = exif.getString(Keys.Exif.MODEL);
			if(exif.has(Keys.Exif.ORIENTATION))
				this.orientation = exif.getInt(Keys.Exif.ORIENTATION);
			if(exif.has(Keys.Exif.IMAGE_LENGTH))
				this.imageLength = exif.getInt(Keys.Exif.IMAGE_LENGTH);
			if(exif.has(Keys.Exif.IMAGE_WIDTH))
				this.imageWidth = exif.getInt(Keys.Exif.IMAGE_WIDTH);
			if(exif.has(Keys.Exif.ISO))
				this.iso = exif.getString(Keys.Exif.ISO);
			if(exif.has(Keys.Exif.WHITE_BALANCE))
				this.whiteBalance = exif.getInt(Keys.Exif.WHITE_BALANCE);
			if(exif.has(Keys.Exif.FLASH))
				this.flash = exif.getInt(Keys.Exif.FLASH);
			if(exif.has(Keys.Exif.EXPOSURE))
				this.exposureTime = exif.getString(Keys.Exif.EXPOSURE);
			if(exif.has(Keys.Exif.FOCAL_LENGTH))
				this.focalLength = exif.getInt(Keys.Exif.FOCAL_LENGTH);
			if(exif.has(Keys.Exif.APERTURE))
				this.aperture = exif.getString(Keys.Exif.APERTURE);
		}
	}
	
	public class Device extends InformaZipper {
		String imei;
		Corroboration bluetoothInfo;
		
		public Device(String imei, Corroboration bluetoothInfo) {
			this.imei = imei;
			this.bluetoothInfo = bluetoothInfo;
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
		String consentGiven;
		
		public Subject(String subjectName, boolean informedConsentGiven, String consentGiven) {
			this.subjectName = subjectName;
			this.informedConsentGiven = informedConsentGiven;
			this.consentGiven = consentGiven;
		}
	}
	
	public class ImageRegion extends InformaZipper {
		CaptureTimestamp captureTimestamp;
		Location location;
		
		public String obfuscationType, unredactedRegionHash;
		JSONObject regionDimensions, regionCoordinates;
		char[] unredactedRegionData;
		
		Subject subject;
		
		public ImageRegion(CaptureTimestamp captureTimestamp, Location location, String obfuscationType, JSONObject regionDimensions, JSONObject regionCoordinates) throws JSONException {
			this.captureTimestamp = captureTimestamp;
			this.location = location;
			this.obfuscationType = obfuscationType;
			this.regionDimensions = regionDimensions;
			this.regionCoordinates = regionCoordinates;
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
	
	private String getMimeType(int sourceType) {
		String mime = "";
		switch(sourceType) {
		case MediaTypes.PHOTO:
			mime = ".jpg";
			break;
		case MediaTypes.VIDEO:
			mime = ".mp4";
			break;
		}
		return mime;
	}
	
	public class Image extends File {
		private static final long serialVersionUID = 1L;
		private String intendedDestination;
		private JSONObject metadataPackage;

		public Image(String path, String intendedDestination) throws JSONException, IllegalArgumentException, IllegalAccessException {
			super(path);
			
			this.intendedDestination = intendedDestination;
			Informa.this.intent = new Intent(intendedDestination);
			this.metadataPackage = new JSONObject();
			this.metadataPackage.put(Keys.Informa.INTENT, Informa.this.intent.zip());
			this.metadataPackage.put(Keys.Informa.GENEALOGY, Informa.this.genealogy.zip());
			this.metadataPackage.put(Keys.Informa.DATA, Informa.this.data.zip());
		}
		
		@SuppressWarnings("unused")
		public String getIntendedDestination() {
			return this.intendedDestination;
		}
		
		@SuppressWarnings("unused")
		public String getMetadataPackage() {
			return this.metadataPackage.toString();
		}
		
	}
	
	private Object getDBValue(String table, String[] keys, String matchKey, Object matchValue, Class<?> expectedType) {
		dh.setTable(db, table);
		Object result = new Object();
		
		try {
			Cursor c = dh.getValue(db, keys, matchKey, matchValue);
			c.moveToFirst();
			
			while(!c.isAfterLast()) {
				for(String key : keys) {
					int index = c.getColumnIndex(key);
					if(expectedType.equals(Long.class)) {
						result = c.getLong(index);
					} else if(expectedType.equals(String.class)) {
						result = c.getString(index);
					} else if(expectedType.equals(Integer.class)) {
						result = c.getInt(index);
					}
				}
				c.moveToNext();
			}
			
			c.close();
			
		} catch(NullPointerException e) {
			Log.e(InformaConstants.TAG, "cursor was nulllll",e);
		}
		
		return result;
	}
	
	private String getAPGEmail(long keyId) {
		return apg.getPublicUserId(_c, keyId);
	}
	
	public Image[] getImages() {
		return images;
	}
	
	public Informa(
			Context c,
			JSONObject imageData, 
			JSONArray regionData, 
			JSONArray capturedEvents, 
			long[] intendedDestinations) throws IllegalArgumentException, JSONException, IllegalAccessException, IOException, NoSuchAlgorithmException {
		
		
		_c = c;
		
		JSONObject mediaSaved = new JSONObject();
		JSONObject mediaCaptured = new JSONObject();
		JSONObject exifData = new JSONObject();
		Set<ImageRegion> imageRegions = new HashSet<ImageRegion>();
		Set<Corroboration> corroboration = new HashSet<Corroboration>();
		Set<Location> locations = new HashSet<Location>();
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(_c);
		
		dh = new DatabaseHelper(c);
		db = dh.getWritableDatabase(sp.getString(Keys.Settings.HAS_DB_PASSWORD, ""));
		
		apg = Apg.getInstance();
		apg.setSignatureKeyId((Long) getDBValue(Keys.Tables.SETUP, new String[] {Keys.Owner.SIG_KEY_ID}, BaseColumns._ID, 1, Long.class));
		
		for(int ce=0; ce<capturedEvents.length(); ce++) {
			JSONObject rd = (JSONObject) capturedEvents.get(ce);
						
			if(rd.getInt(Keys.CaptureEvent.TYPE) != CaptureEvents.BLUETOOTH_DEVICE_SEEN) {
				JSONObject geo = rd.getJSONObject(Keys.Suckers.GEO);
				JSONObject phone = rd.getJSONObject(Keys.Suckers.PHONE);
				JSONObject acc = rd.getJSONObject(Keys.Suckers.ACCELEROMETER);
				
				switch(rd.getInt(Keys.CaptureEvent.TYPE)) {
				case CaptureEvents.MEDIA_CAPTURED:
					mediaCaptured.put(Keys.Location.COORDINATES, geo.getString(Keys.Suckers.Geo.GPS_COORDS));
					mediaCaptured.put(Keys.Location.CELL_ID, phone.getString(Keys.Suckers.Phone.CELL_ID));
					mediaCaptured.put(Keys.Suckers.Accelerometer.LIGHT, 
							((JSONObject) acc.getJSONObject(Keys.Suckers.Accelerometer.LIGHT)).getInt(Keys.Suckers.Accelerometer.LIGHT_METER_VALUE));
					mediaCaptured.put(Keys.Suckers.Accelerometer.ACC, acc.getJSONObject(Keys.Suckers.Accelerometer.ACC));
					mediaCaptured.put(Keys.Suckers.Accelerometer.ORIENTATION, acc.getJSONObject(Keys.Suckers.Accelerometer.ORIENTATION));
					mediaCaptured.put(Keys.Image.TIMESTAMP, rd.getLong(Keys.CaptureEvent.MATCH_TIMESTAMP));
					mediaCaptured.put(Keys.Device.BLUETOOTH_DEVICE_ADDRESS, phone.getString(Keys.Suckers.Phone.BLUETOOTH_DEVICE_ADDRESS));
					mediaCaptured.put(Keys.Device.BLUETOOTH_DEVICE_NAME, phone.getString(Keys.Suckers.Phone.BLUETOOTH_DEVICE_NAME));
					mediaCaptured.put(Keys.Device.IMEI, phone.getString(Keys.Suckers.Phone.IMEI));
					
					JSONObject lomc = new JSONObject();
					lomc.put(Keys.Location.COORDINATES, mediaCaptured.getString(Keys.Suckers.Geo.GPS_COORDS));
					lomc.put(Keys.Location.CELL_ID, mediaCaptured.getString(Keys.Suckers.Phone.CELL_ID));
					locations.add(new Location(InformaConstants.LocationTypes.ON_MEDIA_CAPTURED, lomc));
					break;
				case CaptureEvents.MEDIA_SAVED:
					mediaSaved.put(Keys.Location.COORDINATES, geo.getString(Keys.Suckers.Geo.GPS_COORDS));
					mediaSaved.put(Keys.Location.CELL_ID, phone.getString(Keys.Suckers.Phone.CELL_ID));
					mediaSaved.put(Keys.Image.TIMESTAMP, rd.getLong(Keys.CaptureEvent.MATCH_TIMESTAMP));
					mediaSaved.put(Keys.Device.BLUETOOTH_DEVICE_ADDRESS, phone.getString(Keys.Suckers.Phone.BLUETOOTH_DEVICE_ADDRESS));
					mediaSaved.put(Keys.Device.BLUETOOTH_DEVICE_NAME, phone.getString(Keys.Suckers.Phone.BLUETOOTH_DEVICE_NAME));
					mediaSaved.put(Keys.Device.IMEI, phone.getString(Keys.Suckers.Phone.IMEI));
					
					JSONObject loms = new JSONObject();
					loms.put(Keys.Location.COORDINATES, mediaSaved.getString(Keys.Suckers.Geo.GPS_COORDS));
					loms.put(Keys.Location.CELL_ID, mediaSaved.getString(Keys.Suckers.Phone.CELL_ID));
					locations.add(new Location(InformaConstants.LocationTypes.ON_MEDIA_SAVED, loms));
					break;
				case CaptureEvents.EXIF_REPORTED:
					exifData = rd.getJSONObject(Keys.Image.EXIF);
					break;
				case CaptureEvents.REGION_GENERATED:
					for(int x=0; x< regionData.length(); x++) {
						JSONObject imageRegion = (JSONObject) regionData.get(x);
						
						long timestampToMatch = Long.parseLong(imageRegion.getString(Keys.ImageRegion.TIMESTAMP));
						if(rd.getLong(Keys.CaptureEvent.MATCH_TIMESTAMP) == timestampToMatch) {
														
							CaptureTimestamp ct = new CaptureTimestamp(InformaConstants.CaptureTimestamps.ON_REGION_GENERATED, timestampToMatch);
							
							JSONObject log = new JSONObject();
							log.put(Keys.Location.COORDINATES, geo.getString(Keys.Suckers.Geo.GPS_COORDS));
							log.put(Keys.Location.CELL_ID, phone.getString(Keys.Suckers.Phone.CELL_ID));
							Location locationOnGeneration = new Location(InformaConstants.LocationTypes.ON_REGION_GENERATED, log);
							
							JSONObject regionDimensions = new JSONObject();
							regionDimensions.put(Keys.ImageRegion.WIDTH, Float.parseFloat(imageRegion.getString(Keys.ImageRegion.WIDTH)));
							regionDimensions.put(Keys.ImageRegion.HEIGHT, Float.parseFloat(imageRegion.getString(Keys.ImageRegion.HEIGHT)));
							
							String[] rCoords = imageRegion.getString(Keys.ImageRegion.COORDINATES).substring(1, imageRegion.getString(Keys.ImageRegion.COORDINATES).length() -1).split(",");
							JSONObject regionCoordinates = new JSONObject();
							regionCoordinates.put(Keys.ImageRegion.TOP, Float.parseFloat(rCoords[0]));
							regionCoordinates.put(Keys.ImageRegion.LEFT, Float.parseFloat(rCoords[1]));
							
							ImageRegion ir = new ImageRegion(
									ct, 
									locationOnGeneration, 
									imageRegion.getString(Keys.ImageRegion.FILTER), 
									regionDimensions,
									regionCoordinates);
							
							
							if(imageRegion.has(Keys.ImageRegion.Subject.PSEUDONYM)) {
								ir.subject = new Subject(
									imageRegion.getString(Keys.ImageRegion.Subject.PSEUDONYM),
									Boolean.parseBoolean(imageRegion.getString(Keys.ImageRegion.Subject.INFORMED_CONSENT_GIVEN)),
									"[" + InformaConstants.Consent.GENERAL + "]");
							} else
								ir.subject = null;
							
							imageRegions.add(ir);
						}
					}
					break;
				}
			} else {
				corroboration.add(new Corroboration(rd.getString(Keys.Device.BLUETOOTH_DEVICE_ADDRESS), rd.getString(Keys.Device.BLUETOOTH_DEVICE_NAME), InformaConstants.Device.IS_NEIGHBOR));
			}
		}
		
		genealogy = new Genealogy(
				imageData.getString(Keys.Image.LOCAL_MEDIA_PATH), 
				mediaCaptured.getLong(Keys.Image.TIMESTAMP));
		data = new Data(
				imageData.getInt(Keys.Image.MEDIA_TYPE), 
				new Device(
						mediaSaved.getString(Keys.Device.IMEI), 
						new Corroboration(
								mediaSaved.getString(Keys.Device.BLUETOOTH_DEVICE_ADDRESS),
								mediaSaved.getString(Keys.Device.BLUETOOTH_DEVICE_NAME),
								InformaConstants.Device.IS_SELF)));
		
		data.imageRegions = imageRegions;
		data.corroboration = corroboration;
		data.location = locations;
		data.imageHash = MediaHasher.hash(new File(genealogy.localMediaPath), "MD5");
		data.exif = new Exif(exifData);
		
		try {
			images = new Image[intendedDestinations.length];
			for(int i=0; i<intendedDestinations.length; i++) {
				dh.setTable(db, Tables.TRUSTED_DESTINATIONS);
				try {
					Cursor td = dh.getValue(
							db, 
							new String[] {TrustedDestinations.DISPLAY_NAME, TrustedDestinations.EMAIL},
							TrustedDestinations.KEYRING_ID,
							intendedDestinations[i]);
					td.moveToFirst();
					String displayName = td.getString(td.getColumnIndex(TrustedDestinations.DISPLAY_NAME));
					String email = td.getString(td.getColumnIndex(TrustedDestinations.EMAIL));
					String newPath = 
							InformaConstants.DUMP_FOLDER + 
							genealogy.localMediaPath.substring(genealogy.localMediaPath.lastIndexOf("/"), genealogy.localMediaPath.length() - 4) +
							"_" + displayName.replace(" ", "-") +
							getMimeType(data.sourceType);
					td.close();
					images[i] = new Image(newPath, email);
				} catch(NullPointerException e) {
					Log.e(InformaConstants.TAG, "fracking npe",e); //watch your mouth!
				}
			}
		} catch(NullPointerException e) {
			Log.e(InformaConstants.TAG, "there are no intended destinations",e);
		}
		
		db.close();
		dh.close();
	}
}
