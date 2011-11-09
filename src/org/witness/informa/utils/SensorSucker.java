package org.witness.informa.utils;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
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
	
	List<BroadcastReceiver> br = new ArrayList<BroadcastReceiver>();

	boolean shouldLog = false;

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
		
		br.add(new Broadcaster(new IntentFilter("to_GEOSUCKER")));
		br.add(new Broadcaster(new IntentFilter(BluetoothDevice.ACTION_FOUND)));
		
		for(BroadcastReceiver b : br)
			registerReceiver(b, ((Broadcaster) b)._filter);
		
				
		_geo = new GeoSucker(getApplicationContext());
		_phone = new PhoneSucker(getApplicationContext());
		_acc = new AccelerometerSucker(getApplicationContext());
		
		startLog();
	}
	
	public void onDestroy() {
		super.onDestroy();
		
		for(BroadcastReceiver b : br)
			unregisterReceiver(b);
	}
	
	
	public void startLog() {
		shouldLog = true;
	}
	
	public void stopLog() {
		shouldLog = false;
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

		@Override
		public void onReceive(Context c, Intent i) {
			try {
				JSONObject d = new JSONObject();
				
				if(BluetoothDevice.ACTION_FOUND.equals(i.getAction())) {
					BluetoothDevice device = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					
					d.put("btNeighborDeviceName", device.getName());
					d.put("btNeighborDeviceAddress", device.getAddress());
					
					pushToSucker(_phone, d);
				}
			} catch(JSONException e) {}
		}
			
	}
}
