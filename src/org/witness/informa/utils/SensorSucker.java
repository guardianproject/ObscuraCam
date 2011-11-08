package org.witness.informa.utils;

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
	
	private final BroadcastReceiver broadcaster = new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent i) {
			if(i.getAction().equals(BluetoothDevice.ACTION_FOUND))
				pushToSucker(_phone, i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE).toString());
			else if(i.getAction().equals("to_GEOSUCKER"))
				pushToSucker(_geo,"to_GEOSUCKER");
		}
	};

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
	
	@Override
	public void onCreate() {
		registerReceiver(broadcaster, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		startLog();
	}
	
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(broadcaster);
	}
	
	public void startLog() {
		shouldLog = true;
		new Thread(new Runnable() {
			@Override
			public void run() {
				_geo = new SensorLogger<GeoSucker>(getApplicationContext());
			}
		}).start();
		sendBroadcast(new Intent().setAction("to_GEOSUCKER"));
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				_phone = new SensorLogger<PhoneSucker>(getApplicationContext());
			}
		}).start();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				_acc = new SensorLogger<AccelerometerSucker>(getApplicationContext());
			}
		}).start();
	}
	
	public void stopLog() {
		shouldLog = false;
	}
	
	public void pushToSucker(SensorLogger<?> sucker, String payload) {
		Log.d(ObscuraApp.TAG, "PUSHED TO: " + sucker.getClass().getName());
	}
}
