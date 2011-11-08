package org.witness.informa.utils;

import java.io.File;
import java.util.List;
import java.util.TimerTask;

import org.json.JSONException;
import org.witness.sscphase1.ObscuraApp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

public class SensorSucker extends Service {
	GeoSucker _geo;
	PhoneSucker _phone;
	AccSucker _acc;
		
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
		startLog();
	}
	
	public void startLog() {
		shouldLog = true;
		
		_geo = new GeoSucker();
		_phone = new PhoneSucker();
		_acc = new AccSucker();
	}
	
	public void stopLog() {
		shouldLog = false;
	}
	
	public class GeoSucker extends SensorLogger implements LocationListener {
		LocationManager lm;
		Criteria criteria;
		
		GeoSucker() {
			lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
			
			criteria = new Criteria();
			criteria.setAccuracy(Criteria.NO_REQUIREMENT);
			
			mTask = new TimerTask() {

				@Override
				public void run() throws NullPointerException {
					if(shouldLog) {
						double[] loc = updateLocation();
						try {
							sendToBuffer(jPack("gpsCoords", "[" + loc[0] + "," + loc[1] + "]"));
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						if(isSensing) {
							// turn off service if it's still on...
							pauseLocationUpdates();
							isSensing = false;
						}
					}
				}
			};
			
			mTimer.schedule(mTask, 0, 30000L);
		}
		
		public double[] updateLocation() {
			try {
				String bestProvider = lm.getBestProvider(criteria, false);
				Location l = lm.getLastKnownLocation(bestProvider);
				return new double[] {l.getLatitude(),l.getLongitude()};
			} catch(NullPointerException e) {
				Log.d(ObscuraApp.TAG,e.toString());
				return null;
			} catch(IllegalArgumentException e) {
				Log.d(ObscuraApp.TAG, e.toString());
				return null;
			}
		}
		
		public void pauseLocationUpdates() {
			lm.removeUpdates(this);
			Log.d(ObscuraApp.TAG, "location manager calls disabled by service!");
		}

		@Override
		public void onLocationChanged(Location location) {}

		@Override
		public void onProviderDisabled(String provider) {}

		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}
		
		
	}
	
	public class PhoneSucker extends SensorLogger {
		TelephonyManager tm;
		
		PhoneSucker() {
			tm = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
			try {
				sendToBuffer(jPack("deviceId", getIMEI()));
			} catch (JSONException e) {}
			
			mTask = new TimerTask() {
				
				@Override
				public void run() throws NullPointerException {
					try {
						sendToBuffer(jPack("cellId", getCellId()));
					} catch (JSONException e) {}
				}
			};
			
			mTimer.schedule(mTask, 0, 30000L);
		}
		
		public String getIMEI() {
			try {
				return tm.getDeviceId();
			} catch(NullPointerException e) {
				Log.d(ObscuraApp.TAG,e.toString());
				return null;
			}
		}
		
		public String getCellId() {	
			try {
				String out = "";
				if (tm.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
					final GsmCellLocation gLoc = (GsmCellLocation) tm.getCellLocation();
					out = Integer.toString(gLoc.getCid());
				} else if(tm.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
					final CdmaCellLocation cLoc = (CdmaCellLocation) tm.getCellLocation();
					out = Integer.toString(cLoc.getBaseStationId());
				}
				return out;
			} catch(NullPointerException e) {
				Log.d(ObscuraApp.TAG,e.toString());
				return null;
			}
		}

	}

	public class AccSucker extends SensorLogger{
		SensorManager sm;
		List<Sensor> availableSensors;
		boolean hasAccelerometer, hasGyroscope, hasLight, hasMagneticField;
		
		AccSucker() {
			sm = (SensorManager) getSystemService(SENSOR_SERVICE);
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
					if(shouldLog) {
						if(hasAccelerometer)
							readAccelerometer();
						if(hasGyroscope)
							readGyroscope();
						if(hasLight)
							readLight();
						if(hasMagneticField)
							readMagField();
					}
				}
			};
			
			mTimer.schedule(mTask, 0, 30000L);
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
}
