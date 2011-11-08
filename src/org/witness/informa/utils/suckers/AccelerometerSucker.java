package org.witness.informa.utils.suckers;

import java.util.List;
import java.util.TimerTask;

import org.witness.informa.utils.SensorLogger;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.content.Context;


import android.util.Log;

@SuppressWarnings("rawtypes")
public class AccelerometerSucker extends SensorLogger {
	SensorManager sm;
	List<Sensor> availableSensors;
	boolean hasAccelerometer, hasGyroscope, hasLight, hasMagneticField;
	private TimerTask mTask;
	
	AccelerometerSucker(Context c) {
		super(c);
		sm = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
		availableSensors = sm.getSensorList(Sensor.TYPE_ALL);
		
		for(Sensor s : availableSensors) {
			switch(s.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				hasAccelerometer = true;
				break;
			case Sensor.TYPE_GYROSCOPE:
				hasGyroscope = true;
				break;
			case Sensor.TYPE_LIGHT:
				hasLight = true;
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				hasMagneticField = true;
				break;
			}
				
		}
		
		mTask = new TimerTask() {
			
			@Override
			public void run() {
				if(hasAccelerometer)
					readAccelerometer();
				if(hasGyroscope)
					readGyroscope();
				if(hasLight)
					readLight();
				if(hasMagneticField)
					readMagField();
			}
		};
		
		getTimer().schedule(mTask, 0, 30000L);
	}
	
	private void readAccelerometer() {
		
	}
	
	private void readGyroscope() {
		
	}
	
	private void readLight() {
		
	}
	
	private void readMagField() {
		
	}
}