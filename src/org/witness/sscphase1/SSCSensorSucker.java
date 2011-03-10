/*
 * This class is an android Service that handles
 * all sensor data.  This includes:
 * 		1. geolocative data
 * 		2. accelerometer data
 * 		3. bluetooth "neighbors"
 */
package org.witness.sscphase1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.SQLException;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class SSCSensorSucker extends Service {
	private final String SSC = "[Camera Obscura : SSCSensorSucker] **********************************";
	Timer timer;
	Handler handler;
	SSCMetadataHandler mdh;
	IntentFilter iF;
	
	boolean logBT,logGeo,logAcc;
	private final boolean ALLOWED = true;
	private final boolean NOT_ALLOWED = false;
	
	BluetoothAdapter ba;
	List<String> bluetoothNeighbors;
	
	LocationManager lm;
	Location loc;
	
	List<String> acc;
	
	@Override
	public void onCreate() {}
	
	@Override
	public void onStart(Intent i, int s) {
		Bundle b = i.getExtras();
		
		logBT = b.getBoolean("logBT");
		logAcc = b.getBoolean("logAcc");
		logGeo = b.getBoolean("logGeo");
		
		Log.v(SSC,"service starting in the background with the following preferences:\n"
				+ "logBT = " + logBT + "\n"
				+ "logAcc = " + logAcc + "\n"
				+ "logGeo = " + logGeo
		);
		
		timer = new Timer();
    	handler = new Handler();
    	
    	if(logBT) startBT();
    	if(logAcc) startAcc();
    	if(logGeo) startGeo();
    	
    	timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					public void run() {
						if(logGeo) {
							//loc = updateLocation();
						}
						
						if(logBT) {
							bluetoothNeighbors = updateBluetooth();
						}
						
						if(logAcc) {
							acc = updateAccelerometer();
						}
					}
				});
			}
    	}, 100L, 60000L);
	}
	
	public void startBT() {
		iF = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(br, iF);
	
		iF = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(br, iF);
	
		ba = BluetoothAdapter.getDefaultAdapter();
		if(ba == null) {
			Log.d(SSC,"this user\'s device does not support bluetooth.");
			this.unregisterReceiver(br);
			logBT = false;
		} else {
			if(ba.isDiscovering()) {
				ba.cancelDiscovery();
			}
			ba.enable();
			ba.startDiscovery();
			Log.v(SSC,"BT STATE: " + ba.getState());
		}
	}
	
	public void startAcc() {
		
	}
	
	public void startGeo() {
		
	}
	
	private void doDiscovery() {
		Log.v(SSC,"some more info about this BT connection:" +
				"\nAddress: " + ba.getAddress() +
				"\nName: " + ba.getName() +
				"\nScan Mode: " + ba.getScanMode() +
				"\nState: " + ba.getState());
	}
	
	
	public void linkData(int index) {
		mdh = new SSCMetadataHandler(this);
		try {
			mdh.createDatabase();
		} catch(IOException e) {}
		try {
			mdh.openDataBase();
		} catch(SQLException e) {}
	}
	
	public Location updateLocation() {
		Location l = lm.getLastKnownLocation(lm.getProviders(true).get(0));
		//Log.v(SSC,"attempting to update location");
		return l;
	}
	
	public List<String> updateBluetooth() {
		List<String> bt = new ArrayList<String>();
		//Log.v(SSC,"attempting to update bluetooth");
		return bt;
	}
	
	public List<String> updateAccelerometer() {
		List<String> acc = new ArrayList<String>();
		//Log.v(SSC,"attempting to update accelerometer data");
		return acc;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onDestroy() {
		timer.cancel();
		if(ba != null) {
			ba.cancelDiscovery();
		}
	}
	
    private final BroadcastReceiver br = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if(BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Log.v(SSC,"Bluetooth Device found: " + device.getName() + " @ " + device.getAddress());
			} else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				Log.v(SSC,"Bluetooth discovery finished.");
				ba.disable();
			} else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				Log.v(SSC,"Bluetooth discovery starting...");
			}
		}
    };

}
