package org.witness.informa;

import info.guardianproject.database.sqlcipher.SQLiteDatabase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.witness.informa.utils.JpegParser;
import org.witness.informa.utils.secure.Apg;
import org.witness.informa.utils.secure.MediaHasher;
import org.witness.securesmartcam.io.ObscuraDatabaseHelper;
import org.witness.securesmartcam.io.ObscuraDatabaseHelper.TABLES;
import org.witness.sscphase1.ObscuraApp;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class Informa {
	public Intent intent;
	public Geneaology geneaology;
	public Data data;
	
	private Context c;
	private SQLiteDatabase db;
	private ObscuraDatabaseHelper odh;
	private Apg apg;
	private File image;
		
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
						Iterator i = ((Set<?>) value).iterator();
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
		
		public Intent() {
			this.owner = new Owner();
			this.securityLevel = 1; // TODO: get answer from db
			this.intendedDestination = "INTENDED-DESTINATION"; // TODO: solve this please.
		}
	}
	
	public class Geneaology extends InformaZipper {
		String localMediaPath;
		long dateCreated, dateAcquired;
		
		public Geneaology(String localMediaPath, long dateCreated) {
			this.localMediaPath = localMediaPath;
			this.dateCreated = dateCreated;
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
			this.captureTimestamp = new HashSet<CaptureTimestamp>();
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
	
	public class CaptureTimestamp extends InformaZipper {
		int timestampType;
		long timestamp;
		
		public CaptureTimestamp(int timestampType, long timestamp) {
			this.timestampType = timestampType;
			this.timestamp = timestamp;
		}
	}
	
	public class Owner extends InformaZipper {
		String ownerKey;
		int ownershipType;
		
		public Owner() {
			this.ownerKey = getPGPKey();
			this.ownershipType = getOwnershipType();
		}
		
		public String getPGPKey() {
			return "MY-IDENTITY-IS-HERE"; // TODO: hook into prefs to return this
		}
		
		public int getOwnershipType() {
			return 25; // TODO: call db for this answer
		}
	}
	
	public class Device extends InformaZipper {
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
	
	public class Corroboration extends InformaZipper {
		String deviceBTAddress, deviceBTName;
		int selfOrNeighbor;	// -1 is self, 1 is neighbor
		
		public Corroboration(String deviceBTAddress, String deviceBTName, int selfOrNeighbor) {
			this.deviceBTAddress = deviceBTAddress;
			this.deviceBTName = deviceBTName;
			this.selfOrNeighbor = selfOrNeighbor;
		}
	}
	
	public class ImageRegion extends InformaZipper {
		CaptureTimestamp captureTimestamp;
		Location location;
		
		public String obfuscationType;
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
		}
	}
	
	public Informa(Context c, JSONObject imageData, JSONArray regionData, JSONArray capturedEvents, long[] encryptTo) throws Exception {
		this.c = c;
		
		// initialize the jpegRedaction lib, the db and apg
		SQLiteDatabase.loadLibs(c);
		SharedPreferences _sp = PreferenceManager.getDefaultSharedPreferences(c);
		apg = Apg.getInstance();
		
		
		
		
		image = init(imageData, regionData, capturedEvents);
		if(image != null) {
			odh = new ObscuraDatabaseHelper(c);
			db = odh.getWritableDatabase(_sp.getString("informaPref_dbpw", ""));
			
			// get the imageHash (which is used as the unique identifier for the database)
			String imageHash = MediaHasher.hash(image, "MD5");	
			data.imageHash = imageHash;
			
			JSONObject informaMetadata = renderAsJson();
			JpegParser jpegParser = new JpegParser(image.getAbsolutePath(), informaMetadata);
			
			
			//GZIPOutputStream gzip = parcelizeImage(image, imageHash, informaMetadata);
			
			
			// encrypt saved version on SDCard
		}
	}
	
	private GZIPOutputStream parcelizeImage(File img, String imageHash, JSONObject metadata) throws IOException, NoSuchAlgorithmException {
		// takes the output (from JpegRedaction ultimately, but any jpeg will do)
		InputStream is = new FileInputStream(img);		
		final int BLOB_MAX = 1048576;

		// write file to buffers
		Log.d(ObscuraApp.TAG, "****** blobMax = " + BLOB_MAX);
		Log.d(ObscuraApp.TAG, "****** file length = " + img.length());
		int bytesLeft = (int) img.length();
		int bytesTransfered = 0;
		int blobCount = 0;
		
		JSONArray containmentArray = new JSONArray();
		odh.setTable(db, TABLES.OBSCURA_BITS);
		
		while(bytesTransfered < bytesLeft) {
			int bytesToWrite = 0;
			
			if(bytesLeft - bytesTransfered >= BLOB_MAX)
				bytesToWrite = BLOB_MAX;
			else
				bytesToWrite = bytesLeft - bytesTransfered;
			
			Log.d(ObscuraApp.TAG, "setting bytes " + bytesTransfered + " through " + (bytesTransfered + bytesToWrite));
			byte[] b = new byte[bytesToWrite];
			
			is.read(b, bytesTransfered, bytesToWrite);
			
			Log.d(ObscuraApp.TAG, "inserted blob #" + blobCount + ". size is " + b.length);
			blobCount++;
			
			long thisHash = System.currentTimeMillis();
			
			ContentValues cv = new ContentValues();
			cv.put("hash", thisHash);
			cv.put("data", b);
			db.insert(odh.getTable(), null, cv);
			
			containmentArray.put(thisHash);			
			bytesTransfered += bytesToWrite;
		}
		
		// insert list of hashes into obscura db
		odh.setTable(db, TABLES.OBSCURA);
		ContentValues cv = new ContentValues();
		cv.put("imageHash", imageHash);
		cv.put("containmentArray", containmentArray.toString());
		cv.put("metadata", metadata.toString());
		db.insert(odh.getTable(), null, cv);
		
		db.close();
		odh.close();
		
		String zipName = imageHash + "_" + System.currentTimeMillis() + ".zip";
		
		// create an input stream from the file
		ZipInputStream fZis = new ZipInputStream(is);
		
		// create an input stream from the jsonobj (as .json?)
		byte[] mBytes = metadata.toString().getBytes();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		
		
		
		
		GZIPOutputStream gzip = new GZIPOutputStream(new FileOutputStream(imageHash + "_" + System.currentTimeMillis() + ".zip"));
		
		is.close();
		return gzip;
		
	}
	
	private File init(JSONObject imageData, JSONArray regionData, JSONArray capturedEvents) throws JSONException {
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
						data.imageRegions.add(new ImageRegion(imageRegion));			
					}
				}
				
				break;
			}
		}
		
		return new File(geneaology.localMediaPath);
		
	}
	
	public JSONObject renderAsJson() throws JSONException, IllegalArgumentException, IllegalAccessException {
		JSONObject sourceObject = new JSONObject();
		sourceObject.put("intent", intent.zip());
		sourceObject.put("geneaology", geneaology.zip());
		sourceObject.put("data", data.zip());
		Log.d(ObscuraApp.TAG, "INFORMA READOUT ****************\n" + sourceObject.toString());
		return sourceObject;
	}
}
