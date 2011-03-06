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
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class SSCSensorSucker extends Service {
	private final String SSC = "[Camera Obscura : SSCSensorSucker] **********************************";
	Timer timer;
	Handler handler;
	SSCMetadataHandler mdh;
	
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
		timer = new Timer();
    	handler = new Handler();
    	
    	lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    	ba = BluetoothAdapter.getDefaultAdapter();
    	
    	/*
    	 * TODO: parse preference file for user's stance on sensor logging
    	 * for now, i set them all as "allowed"
    	 */
    	logBT = logGeo = logAcc = ALLOWED;
    	
    	if(ba == null) {
    		Log.d(SSC,"this user\'s device does not support bluetooth.");
    		logBT = false;
    	} else {
    		if(logBT) {
    			ba.startDiscovery();
    		}
    	}
    	
    	Log.v(SSC,"service starting in the background...");
    	
    	timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					public void run() {
						if(logGeo) {
							loc = updateLocation();
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

}
