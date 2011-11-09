package org.witness.informa.utils.suckers;

import java.util.List;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;
import org.witness.informa.utils.SensorLogger;
import org.witness.sscphase1.ObscuraApp;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.content.Context;


import android.util.Log;

@SuppressWarnings("rawtypes")
public class AccelerometerSucker extends SensorLogger implements SensorEventListener {
	SensorManager sm;
	List<Sensor> availableSensors;
	boolean hasAccelerometer, hasOrientation, hasLight, hasMagneticField;
			
	public AccelerometerSucker(Context c) {
		super(c);
		
		Log.d(ObscuraApp.TAG,"! HELLO ACC SUCKER!");
		
		sm = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
		availableSensors = sm.getSensorList(Sensor.TYPE_ALL);
		
		for(Sensor s : availableSensors) {
			switch(s.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				hasAccelerometer = true;
				sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
				break;
			case Sensor.TYPE_LIGHT:
				Log.d(ObscuraApp.TAG,"AND THE LIGHT METER, TOO");
				sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
				hasLight = true;
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
				hasOrientation = true;
				break;
			}
				
		}
		
		setTask(new TimerTask() {
			
			@Override
			public void run() {
				if(hasAccelerometer)
					readAccelerometer();
				if(hasLight)
					readLight();
				if(hasMagneticField)
					readMagField();
				if(hasOrientation)
					readOrientation();
			}
		});
		
		getTimer().schedule(getTask(), 0, 30000L);
	}
	
	private void readAccelerometer() {
		
	}
	
	private void readOrientation() {
		
	}
	
	private void readLight() {
		
	}
	
	private void readMagField() {
		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	@Override
	public void onSensorChanged(SensorEvent event) {
		synchronized(this) {
			JSONObject sVals = new JSONObject();
			
			
			try {				
				switch(event.sensor.getType()) {
				case Sensor.TYPE_ACCELEROMETER:
					float[] acc = event.values.clone();
					sVals.put("acc_x", acc[0]);
					sVals.put("acc_y", acc[1]);
					sVals.put("acc_z", acc[2]);
					break;
				case Sensor.TYPE_MAGNETIC_FIELD:
					float[] geoMag = event.values.clone();
					sVals.put("azimuth", geoMag[0]);
					sVals.put("pitch", geoMag[1]);
					sVals.put("roll", geoMag[2]);
					break;
				case Sensor.TYPE_LIGHT:
					sVals.put("lightMeter", event.values[0]);
					break;
				}
				
				//sendToBuffer(sVals);
			} catch(JSONException e) {}
		}
	}
	
	public void stopUpdates() {
		sm.unregisterListener(this);
	}
}