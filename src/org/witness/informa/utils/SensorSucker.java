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

	String _centerTS;
	File mLog;
	JSONArray imageData, storedRegionData, imageRegions;
	
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
		br.add(new Broadcaster(new IntentFilter(ObscuraApp.CENTER_CAPTURE)));
		br.add(new Broadcaster(new IntentFilter(ObscuraApp.SET_CURRENT)));
		br.add(new Broadcaster(new IntentFilter(ObscuraApp.SEAL_LOG)));
		
		for(BroadcastReceiver b : br)
			registerReceiver(b, ((Broadcaster) b)._filter);
		
				
		_geo = new GeoSucker(getApplicationContext());
		_phone = new PhoneSucker(getApplicationContext());
		_acc = new AccelerometerSucker(getApplicationContext());
		
		imageData = storedRegionData = imageRegions = new JSONArray();
		
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
		
		Log.d(ObscuraApp.TAG, "the suckers have all been stopped.");
		stopSelf();
	}
	
	public void pushToSucker(SensorLogger<?> sucker, JSONObject payload) throws JSONException {
		if(sucker.getClass().equals(PhoneSucker.class))
			_phone.sendToBuffer(payload);
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
			if(ObscuraApp.STOP_SUCKING.equals(i.getAction())) {
				stopSucking();
			} else if(ObscuraApp.SEAL_LOG.equals(i.getAction())) {
				try {
					JSONObject regionDataPackage = (JSONObject) new JSONTokener(i.getStringExtra("regionData")).nextValue();
					JSONArray regionData = regionDataPackage.getJSONArray("imageRegions");
					for(int x=0;x< regionData.length(); x++) {
						// associate the timestamp with the stored values here
						JSONObject imageRegion = (JSONObject) regionData.get(x);
						long timestampToMatch = Long.parseLong(imageRegion.getString("timestampOnGeneration"));
						
						// grep storedRegionData for matching TS and attendant values
						for(int y=0;y< storedRegionData.length(); y++) {
							JSONObject rd = (JSONObject) storedRegionData.get(y);
							if(rd.getLong("timestamp") == timestampToMatch) {
								JSONObject geo = rd.getJSONObject("geo");
								JSONObject phone = rd.getJSONObject("phone");
								
								Map<String, Object> locationOnGeneration = new HashMap<String, Object>();
								locationOnGeneration.put("gpsCoords", geo.getString("gpsCoords"));
								locationOnGeneration.put("cellId", phone.getString("cellId"));
								
								JSONObject locOnGen = (JSONObject) new JSONTokener(locationOnGeneration.toString()).nextValue();
								
								imageRegion.put("locationOnGeneration", locOnGen);
							}
								
						}
					}
					// TODO: WHAT TO DO WITH THE DATA?
				} catch (JSONException e) {}
					
				/*
				String imgName = new File(i.getStringExtra("newImagePath")).getName();
				String newLogName = 
						"/mnt/sdcard/DCIM/Camera/informa/" + 
						imgName.substring(0,imgName.length() - 4) +
						"_informa.txt";
				mLog = new File(newLogName);
				
				try {
					FileWriter fw = new FileWriter(newLogName);
					
					JSONObject regionData = new JSONObject();
					
					regionData.put("regionData", i.getStringExtra("regionData"));
					imageData.put(regionData);
					
					fw.write(imageData.toString());
					fw.close();
				} catch(JSONException e) {}
				catch (IOException e) {
					e.printStackTrace();
				}
				*/
			} else {
				try {
					if(BluetoothDevice.ACTION_FOUND.equals(i.getAction())) {
						JSONObject d = new JSONObject();
						BluetoothDevice device = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
						
						d.put("btNeighborDeviceName", device.getName());
						d.put("btNeighborDeviceAddress", device.getAddress());
						
						pushToSucker(_phone, d);
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
		}
			
	}
}
