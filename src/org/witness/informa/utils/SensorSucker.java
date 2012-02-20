package org.witness.informa.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.witness.informa.Informa;
import org.witness.informa.Informa.Image;
import org.witness.informa.utils.suckers.*;
import org.witness.securesmartcam.ImageEditor;
import org.witness.sscphase1.ObscuraApp;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

@SuppressWarnings("unused")
public class SensorSucker extends Service {
	SensorLogger<GeoSucker> _geo;
	SensorLogger<PhoneSucker> _phone;
	SensorLogger<AccelerometerSucker> _acc;

	JSONArray capturedEvents, imageRegions;
	JSONObject imageData;
	Handler informaCallback;
	
	List<BroadcastReceiver> br = new ArrayList<BroadcastReceiver>();

	public class LocalBinder extends Binder {
		public SensorSucker getService() {
			return SensorSucker.this;
		}
	}
	
	private final IBinder binder = new LocalBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate() {
		
		Log.d(InformaConstants.TAG, "Informa v1.1 starting");
		
		br.add(new Broadcaster(new IntentFilter(InformaConstants.Keys.Service.STOP_SERVICE)));
		br.add(new Broadcaster(new IntentFilter(BluetoothDevice.ACTION_FOUND)));
		br.add(new Broadcaster(new IntentFilter(InformaConstants.Keys.Service.SET_CURRENT)));
		br.add(new Broadcaster(new IntentFilter(InformaConstants.Keys.Service.SEAL_LOG)));
		
		for(BroadcastReceiver b : br)
			registerReceiver(b, ((Broadcaster) b)._filter);
		
		_geo = new GeoSucker(getApplicationContext());
		_phone = new PhoneSucker(getApplicationContext());
		_acc = new AccelerometerSucker(getApplicationContext());
		
		capturedEvents = imageRegions = new JSONArray();
		imageData = new JSONObject();
	}
	
	public void onDestroy() {
		super.onDestroy();
		
		for(BroadcastReceiver b : br)
			unregisterReceiver(b);
	}
	
	public void stopSucking() {
		_geo.getSucker().stopUpdates();
		_phone.getSucker().stopUpdates();
		_acc.getSucker().stopUpdates();
		stopSelf();
	}
	
	private void handleBluetooth(BluetoothDevice device) throws JSONException {
		JSONObject captureEventData = new JSONObject();
		
		captureEventData.put(InformaConstants.Keys.CaptureEvent.TYPE, InformaConstants.CaptureEvents.BLUETOOTH_DEVICE_SEEN);
		captureEventData.put(InformaConstants.Keys.CaptureEvent.MATCH_TIMESTAMP, System.currentTimeMillis());
		captureEventData.put(InformaConstants.Keys.Suckers.Phone.BLUETOOTH_DEVICE_NAME, device.getName());
		captureEventData.put(InformaConstants.Keys.Suckers.Phone.BLUETOOTH_DEVICE_ADDRESS, device.getAddress());
		
		capturedEvents.put(captureEventData);
	}
	
	private void handleExif(String exif) throws JSONException {
		JSONObject captureEventData = new JSONObject();
		
		captureEventData.put(InformaConstants.Keys.CaptureEvent.TYPE, InformaConstants.CaptureEvents.EXIF_REPORTED);
		captureEventData.put(InformaConstants.Keys.Image.EXIF, (JSONObject) new JSONTokener(exif).nextValue());
		
		capturedEvents.put(captureEventData);
	}
	
	private void pushToSucker(SensorLogger<?> sucker, JSONObject payload) throws JSONException {
		if(sucker.getClass().equals(PhoneSucker.class))
			_phone.sendToBuffer(payload);
	}
	
	private void captureEventData(long timestampToMatch, int captureEvent) throws Exception {
		JSONObject captureEventData = new JSONObject();
		
		captureEventData.put(InformaConstants.Keys.CaptureEvent.TYPE, captureEvent);
		captureEventData.put(InformaConstants.Keys.CaptureEvent.MATCH_TIMESTAMP, timestampToMatch);
		captureEventData.put(InformaConstants.Keys.Suckers.GEO, _geo.returnCurrent());
		captureEventData.put(InformaConstants.Keys.Suckers.PHONE, _phone.returnCurrent());
		captureEventData.put(InformaConstants.Keys.Suckers.ACCELEROMETER, _acc.returnCurrent());
		
		capturedEvents.put(captureEventData);
	}
	
	private void sealLog(String imageRegionData, String localMediaPath, long[] encryptTo) throws Exception {
		imageData.put(InformaConstants.Keys.Image.LOCAL_MEDIA_PATH, localMediaPath);
		imageData.put(InformaConstants.Keys.Image.MEDIA_TYPE, InformaConstants.MediaTypes.PHOTO);
		imageRegions = (JSONArray) new JSONTokener(imageRegionData).nextValue();
		final long[] intendedDestinations = encryptTo;
		
		//TODO informa in new thread
		//Informa informa = new Informa(getApplicationContext(), imageData, imageRegions, capturedEvents, encryptTo);
		informaCallback = new Handler();
		
		Runnable r = new Runnable() {
			Informa informa; 
			
			@Override
			public void run() {
				try {
					informa = new Informa(getApplicationContext(), imageData, imageRegions, capturedEvents, intendedDestinations);
					for(Image img : informa.getImages()) {
						ImageConstructor ic = new ImageConstructor(getApplicationContext(), img.getAbsolutePath(), img.getMetadataPackage());
					}
				} catch (IllegalArgumentException e) {
					Log.d(InformaConstants.TAG, "informa called Illegal Arguments: " + e.toString());
				} catch (JSONException e) {
					Log.d(InformaConstants.TAG, "informa called JSONException?: " + e.toString());
				} catch (IllegalAccessException e) {
					Log.d(InformaConstants.TAG, "informa called Illegal Access: " + e.toString());
				} catch (NoSuchAlgorithmException e) {
					Log.d(InformaConstants.TAG, "informa called NoSuchAlgoException: " + e.toString());
				} catch (IOException e) {
					Log.d(InformaConstants.TAG, "informa called IOException: " + e.toString());
				}
				
			}
			
		};
		new Thread(r).start();
		
		stopSucking();
		
	}
	
	public class Broadcaster extends BroadcastReceiver {
		IntentFilter _filter;
		
		public Broadcaster(IntentFilter filter) {
			_filter = filter;
		}
		
		public JSONArray appendToArray(JSONArray big, JSONArray small) throws JSONException {
			for(int x=0; x<small.length(); x++) {
				big.put(small.get(x));
			}
			return big;
		}

		@Override
		public void onReceive(Context c, Intent i) {
			Log.d(InformaConstants.TAG, c.getPackageName());
			try {
				if(InformaConstants.Keys.Service.STOP_SERVICE.equals(i.getAction())) {
					stopSucking();
				} else if(BluetoothDevice.ACTION_FOUND.equals(i.getAction())) {
					handleBluetooth((BluetoothDevice) i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
				} else if(InformaConstants.Keys.Service.SEAL_LOG.equals(i.getAction())) {
					sealLog(
						i.getStringExtra(InformaConstants.Keys.ImageRegion.DATA), 
						i.getStringExtra(InformaConstants.Keys.Image.LOCAL_MEDIA_PATH), 
						i.getLongArrayExtra(InformaConstants.Keys.Intent.ENCRYPT_LIST));
				} else if(InformaConstants.Keys.Service.SET_CURRENT.equals(i.getAction())) {
					captureEventData(
						i.getLongExtra(InformaConstants.Keys.CaptureEvent.MATCH_TIMESTAMP, 0L),
						i.getIntExtra(InformaConstants.Keys.CaptureEvent.TYPE, InformaConstants.CaptureEvents.REGION_GENERATED));
				} else if(InformaConstants.Keys.Service.SET_EXIF.equals(i.getAction())) {
					handleExif(i.getStringExtra(InformaConstants.Keys.Image.EXIF));
				}
			} catch (Exception e) {
				Log.d(InformaConstants.TAG, "error: " + e);
			}
		}
			
	}
}
