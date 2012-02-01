package org.witness.informa.utils.suckers;

import java.util.List;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;
import org.witness.informa.utils.InformaConstants;
import org.witness.informa.utils.SensorLogger;
import org.witness.sscphase1.ObscuraApp;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Context;


import android.util.Log;

@SuppressWarnings("rawtypes")
public class AccelerometerSucker extends SensorLogger implements SensorEventListener {
	SensorManager sm;
	List<Sensor> availableSensors;
	boolean hasAccelerometer, hasOrientation, hasLight, hasMagneticField;
	JSONObject currentAccelerometer, currentLight, currentMagField;
			
	@SuppressWarnings("unchecked")
	public AccelerometerSucker(Context c) {
		super(c);
		setSucker(this);
		
		sm = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
		availableSensors = sm.getSensorList(Sensor.TYPE_ALL);
		
		currentAccelerometer = currentLight = currentMagField = new JSONObject();
		
		for(Sensor s : availableSensors) {
			switch(s.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				hasAccelerometer = true;
				sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
				break;
			case Sensor.TYPE_LIGHT:
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
				try {
					if(hasAccelerometer)
						readAccelerometer();
					if(hasLight)
						readLight();
					if(hasOrientation)
						readOrientation();
				} catch(JSONException e){}
			}
		});
		
		getTimer().schedule(getTask(), 0, 10000L);
	}
	
	private void readAccelerometer() throws JSONException {
		sendToBuffer(currentAccelerometer);
	}
	
	private void readOrientation() throws JSONException {
		sendToBuffer(currentMagField);
	}
	
	private void readLight() throws JSONException {
		sendToBuffer(currentLight);
	}
	
	public JSONObject forceReturn() {
		try {
			JSONObject fr = new JSONObject();
			fr.put(InformaConstants.Keys.Suckers.Accelerometer.ACC, currentAccelerometer);
			fr.put(InformaConstants.Keys.Suckers.Accelerometer.ORIENTATION, currentMagField);
			fr.put(InformaConstants.Keys.Suckers.Accelerometer.LIGHT, currentLight);
			return fr;
		} catch(JSONException e) {
			return null;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	@Override
	public void onSensorChanged(SensorEvent event) {
		synchronized(this) {
			if(getIsRunning()) {
				JSONObject sVals = new JSONObject();
				
				try {				
					switch(event.sensor.getType()) {
					case Sensor.TYPE_ACCELEROMETER:
						float[] acc = event.values.clone();
						sVals.put(InformaConstants.Keys.Suckers.Accelerometer.X, acc[0]);
						sVals.put(InformaConstants.Keys.Suckers.Accelerometer.Y, acc[1]);
						sVals.put(InformaConstants.Keys.Suckers.Accelerometer.Z, acc[2]);
						currentAccelerometer = sVals;
						break;
					case Sensor.TYPE_MAGNETIC_FIELD:
						float[] geoMag = event.values.clone();
						sVals.put(InformaConstants.Keys.Suckers.Accelerometer.AZIMUTH, geoMag[0]);
						sVals.put(InformaConstants.Keys.Suckers.Accelerometer.PITCH, geoMag[1]);
						sVals.put(InformaConstants.Keys.Suckers.Accelerometer.ROLL, geoMag[2]);
						currentMagField = sVals;
						break;
					case Sensor.TYPE_LIGHT:
						sVals.put(InformaConstants.Keys.Suckers.Accelerometer.LIGHT_METER_VALUE, event.values[0]);
						currentLight = sVals;
						break;
					}
				} catch(JSONException e) {}
			}
		}
	}
	
	public void stopUpdates() {
		setIsRunning(false);
		sm.unregisterListener(this);
		Log.d(InformaConstants.TAG, "shutting down AccelerometerSucker...");
	}
}