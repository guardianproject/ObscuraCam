package org.witness.informa.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
import org.witness.informa.utils.suckers.*;
import org.witness.sscphase1.ObscuraApp;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

@SuppressWarnings("unused")
public class SensorSucker extends Service {
	SensorLogger<GeoSucker> _geo;
	SensorLogger<PhoneSucker> _phone;
	SensorLogger<AccelerometerSucker> _acc;

	JSONArray capturedEvents, imageRegions;
	JSONObject imageData;
	
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
		
		br.add(new Broadcaster(new IntentFilter(ObscuraApp.STOP_SUCKING)));
		br.add(new Broadcaster(new IntentFilter(BluetoothDevice.ACTION_FOUND)));
		br.add(new Broadcaster(new IntentFilter(ObscuraApp.SET_CURRENT)));
		br.add(new Broadcaster(new IntentFilter(ObscuraApp.SEAL_LOG)));
		
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
		JSONObject d = new JSONObject();		
		
		d.put("btNeighborDeviceName", device.getName());
		d.put("btNeighborDeviceAddress", device.getAddress());
		
		pushToSucker(_phone, d);
	}
	
	private void pushToSucker(SensorLogger<?> sucker, JSONObject payload) throws JSONException {
		if(sucker.getClass().equals(PhoneSucker.class))
			_phone.sendToBuffer(payload);
	}
	
	private void captureEventData(long timestampToMatch, int captureEvent) throws Exception {
		JSONObject captureEventData = new JSONObject();
		
		captureEventData.put("captureEvent", captureEvent);
		captureEventData.put("timestamp", timestampToMatch);
		captureEventData.put("geo", _geo.returnCurrent());
		captureEventData.put("phone", _phone.returnCurrent());
		captureEventData.put("acc", _acc.returnCurrent());
		
		capturedEvents.put(captureEventData);
	}
	
	private void sealLog(String imageRegionData, String localMediaPath, long[] encryptTo) throws Exception {
		imageData.put("localMediaPath", localMediaPath);
		imageData.put("sourceType", 101);
		
		imageRegions = (JSONArray) new JSONTokener(imageRegionData).nextValue();
		
		Informa informa = new Informa(imageData, imageRegions, capturedEvents);
		Log.d(ObscuraApp.TAG, "also we are encypting to " + encryptTo.toString() + " (length: " + encryptTo.length + ")");
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
			try {
				if(ObscuraApp.STOP_SUCKING.equals(i.getAction())) {
					stopSucking();
				} else if(BluetoothDevice.ACTION_FOUND.equals(i.getAction())) {
					handleBluetooth((BluetoothDevice) i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
				} else if(ObscuraApp.SEAL_LOG.equals(i.getAction())) {
					sealLog(i.getStringExtra("regionData"), i.getStringExtra("newImagePath"), i.getLongArrayExtra("encryptTo"));
				} else if(ObscuraApp.SET_CURRENT.equals(i.getAction())) {
					captureEventData(
							i.getLongExtra("timestampToMatch", 0L),
							i.getIntExtra("eventType", ObscuraApp.CAPTURE_EVENTS.RegionGenerated)
					);
				}
			} catch (Exception e) {
				Log.d(ObscuraApp.TAG, "************************** broadcast error: " + e);
			}
			
			/*
				try {
					if(BluetoothDevice.ACTION_FOUND.equals(i.getAction())) {
						JSONObject d = new JSONObject();
						BluetoothDevice device = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
						
						d.put("btNeighborDeviceName", device.getName());
						d.put("btNeighborDeviceAddress", device.getAddress());
						
						pushToSucker(_phone, d);
					} else if(ObscuraApp.SEAL_LOG.equals(i.getAction())) {
						sealLog(i.getStringExtra("regionData"));
					} else if(ObscuraApp.CENTER_CAPTURE.equals(i.getAction())) {
						_centerTS = i.getStringExtra(ObscuraApp.CENTER_CAPTURE);
												
						if((_geo.getLog() != null && _geo.getLog().length() > 0)) {
							imageData = appendToArray(imageData, _geo.getLog());
						}
						
						if((_phone.getLog() != null && _phone.getLog().length() > 0)) {
							imageData = appendToArray(imageData, _phone.getLog());
						}
						
						if((_acc.getLog() != null && _acc.getLog().length() > 0)) {
							imageData = appendToArray(imageData, _acc.getLog());
						}
					} else if(ObscuraApp.SET_CURRENT.equals(i.getAction())) {
						JSONObject imageRegionInit = new JSONObject();
						imageRegionInit.put("timestamp", i.getLongExtra("timestampToMatch", 0L));
						imageRegionInit.put("geo", _geo.returnCurrent());
						imageRegionInit.put("phone", _phone.returnCurrent());
						
						storedRegionData.put(imageRegionInit);
						Log.d(ObscuraApp.TAG, "init image region with: " + imageRegionInit.toString());
					}
				} catch(JSONException e) {} 
				catch (SecurityException e) {}
				catch (IllegalArgumentException e) {}
				catch (NoSuchMethodException e) {}
				catch (IllegalAccessException e) {}
				catch (InvocationTargetException e) {}
			}
			*/
		}
			
	}
}
