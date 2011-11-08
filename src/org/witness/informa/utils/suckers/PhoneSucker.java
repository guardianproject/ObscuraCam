package org.witness.informa.utils.suckers;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.json.JSONException;
import org.witness.informa.utils.SensorLogger;
import org.witness.sscphase1.ObscuraApp;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

@SuppressWarnings("rawtypes")
public class PhoneSucker extends SensorLogger {
	TelephonyManager tm;
	BluetoothAdapter ba;
	private TimerTask mTask;
	
	PhoneSucker(Context c) {
		super(c);
		tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
		ba = BluetoothAdapter.getDefaultAdapter();
		
		// if bluetooth is off, turn it on... (be sure to turn off when finished)
		if(!ba.isEnabled())
			ba.enable();
		
		// if wifi is off, turn it on... (be sure to turn off when finished)
		
		try {
			sendToBuffer(jPack("deviceId", getIMEI()));
			sendToBuffer(jPack("bluetoothAddress", ba.getAddress()));
		} catch (JSONException e) {}
		
		mTask = new TimerTask() {
			
			@Override
			public void run() throws NullPointerException {
				try {
					sendToBuffer(jPack("cellId", getCellId()));
					
					// find other bluetooth devices around
					if(!ba.isDiscovering())
						ba.startDiscovery();
					
					
					ba.cancelDiscovery();
					
				} catch (JSONException e) {}
			}
		};
		
		getTimer().schedule(mTask, 0, 30000L);
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
	
	public List<String> getBluetoothNeighbors() {
		List<String> bt = new ArrayList<String>();
		
		return bt;
	}
	
	public List<String> getWifiNetworks() {
		List<String> wifi = new ArrayList<String>();
		
		return wifi;
	}

}