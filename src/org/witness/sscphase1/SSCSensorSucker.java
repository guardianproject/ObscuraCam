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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
	public final boolean ALLOWED = true;
	public final boolean NOT_ALLOWED = false;
	
	BluetoothAdapter ba;
	ArrayList<String> bluetoothNeighbors;
	
	LocationManager lm;
	Location loc;
	
	private static Sensor sensor;
	private static SensorManager sm;
	ArrayList<String> acc;
	float[] sensorValues;
	
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
							loc = updateLocation();
						}
						
						if(logBT) {
							doDiscovery();
						}
						
						if(logAcc) {
							acc = updateAccelerometer();
						}
						String btListToString = "";
						String accListToString = "";
						for(int x=0;x<bluetoothNeighbors.size();x++) {
							btListToString += bluetoothNeighbors.get(x) + ",\n";
						}
						Log.v(SSC,"Data dump:\nBT: " + btListToString);
						Log.v(SSC,"Data dump:\nLOC: " + loc.getLatitude() + "," + loc.getLongitude());
						Log.v(SSC,"Data dump:\nACC: " + accListToString);
					}
				});
			}
    	}, 100L, 1 * (60000L));
	}
	
	public void startBT() {
		bluetoothNeighbors = new ArrayList<String>();
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
		sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		List<Sensor> slist = sm.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if(slist.size() <= 0) {
			Log.d(SSC,"this user\'s device does not support accelerometers");
			logAcc = false;
		} else {
			sensor = slist.get(0);
			sm.registerListener(sl,sensor, 0);
		}
	}
	
	public void startGeo() {
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	}
	
	private void doDiscovery() {
		if(ba.isDiscovering()) {
			ba.cancelDiscovery();
		}
		ba.startDiscovery();
	}
	
	
	public void linkData(int index) {
		mdh = new SSCMetadataHandler(this);
		/*
		try {
			mdh.createDatabase();
		} catch(IOException e) {}
		*/
		try {
			mdh.openDataBase();
		} catch(SQLException e) {}
	}
	
	public Location updateLocation() {
		Location l = lm.getLastKnownLocation(lm.getProviders(true).get(0));
		return l;
	}
	
	public void updateBluetooth(String neighbor) {
		if(ba.isDiscovering()) {
			ba.cancelDiscovery();
		}
		ba.enable();
		ba.startDiscovery();
	}
	
	public ArrayList<String> updateAccelerometer() {
		ArrayList<String> acc = new ArrayList<String>();
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
			this.unregisterReceiver(br);
		}
	}
	
    private final BroadcastReceiver br = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if(BluetoothDevice.ACTION_FOUND.equals(action)) {
				Log.v(SSC,"Bluetooth discovering...");
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				bluetoothNeighbors.add(device.getName() + ";" + device.getAddress());
			} else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				Log.v(SSC,"Bluetooth discovery finished.");
				ba.disable();
			} else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				Log.v(SSC,"Bluetooth discovery starting...");
				bluetoothNeighbors.clear();
			}
		}
    };
    
    private final SensorEventListener sl = new SensorEventListener() {
		
		public void onSensorChanged(SensorEvent event) {
			sensorValues = event.values;			
		}
				
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

}
