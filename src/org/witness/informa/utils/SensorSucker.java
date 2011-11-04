package org.witness.informa.utils;

import java.io.File;
import java.util.TimerTask;

import org.witness.sscphase1.ObscuraApp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
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
		_geo = new GeoSucker();
		//_geo.run();
	}
	
	public void startLog() {
		shouldLog = true;
	}
	
	public void stopLog() {
		shouldLog = false;
	}
	
	public class GeoSucker extends SensorLogger {
		LocationManager lm;
		Criteria criteria;
		
		GeoSucker() {
			lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
			mTask = new TimerTask() {

				@Override
				public void run() {
					try {
						double[] loc = updateLocation();
						sendToBuffer(jPack("gpsCoords", loc));
					} catch(NullPointerException e) {
						// updatelocation was null?
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
			}
		}
		
		
	}
	
	public class PhoneSucker extends SensorLogger {
		TelephonyManager tm;
		
		PhoneSucker() {
			tm = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
			mTask = new TimerTask() {
				
				@Override
				public void run() {
					try {
						String cellId = getCellId();
						String deviceId = getIMEI();
						
						sendToBuffer(jPack("cellId", cellId));
						sendToBuffer(jPack("deviceId", deviceId));
					} catch(NullPointerException e) {
						// geCellId or getIMEI was null?
					}
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
		AccSucker() {
			
		}
	}
}
