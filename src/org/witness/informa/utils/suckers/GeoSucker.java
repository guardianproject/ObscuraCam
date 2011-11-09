package org.witness.informa.utils.suckers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.json.JSONException;
import org.witness.informa.utils.SensorLogger;
import org.witness.sscphase1.ObscuraApp;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

@SuppressWarnings("rawtypes")
public class GeoSucker extends SensorLogger implements LocationListener {
	LocationManager lm;
	Criteria criteria;
	
	public GeoSucker(Context c) {
		super(c);
		Log.d(ObscuraApp.TAG,"! HELLO GEO SUCKER!");
				
		lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
		lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		
		criteria = new Criteria();
		criteria.setAccuracy(Criteria.NO_REQUIREMENT);
		
		setTask(new TimerTask() {

			@Override
			public void run() throws NullPointerException {
				double[] loc = updateLocation();
				try {
					sendToBuffer(jPack("gpsCoords", "[" + loc[0] + "," + loc[1] + "]"));
				} catch (JSONException e) {
					Log.d(ObscuraApp.TAG,e.toString());
				}
			}
		});
		
		getTimer().schedule(getTask(), 0, 30000L);
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
	
	public void stopUpdates() {
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
