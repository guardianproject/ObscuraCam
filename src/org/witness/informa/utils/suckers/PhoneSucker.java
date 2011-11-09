package org.witness.informa.utils.suckers;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.json.JSONException;
import org.witness.informa.utils.SensorLogger;
import org.witness.sscphase1.ObscuraApp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

@SuppressWarnings("rawtypes")
public class PhoneSucker extends SensorLogger {
	TelephonyManager tm;
	BluetoothAdapter ba;
	
	boolean hasBluetooth = false;
	
	public PhoneSucker(Context c) {
		super(c);
		
		Log.d(ObscuraApp.TAG,"! HELLO PHONE SUCKER!");
		
		tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
		ba = BluetoothAdapter.getDefaultAdapter();
		
		if(ba != null)
			hasBluetooth = true;
		else
			Log.d(ObscuraApp.TAG,"no bt?");
		
		// if bluetooth is off, turn it on... (be sure to turn off when finished)
		if(!ba.isEnabled() && hasBluetooth)
			ba.enable();
		
		// TODO: if wifi is off, turn it on... (be sure to turn off when finished)
		
		try {
			sendToBuffer(jPack("deviceId", getIMEI()));
			sendToBuffer(jPack("bluetoothAddress", ba.getAddress()));
			sendToBuffer(jPack("bluetoothName", ba.getName()));
			
		} catch (JSONException e) {}
		catch(NullPointerException e) {}
		
		setTask(new TimerTask() {
			
			@Override
			public void run() throws NullPointerException {
				try {
					sendToBuffer(jPack("cellId", getCellId()));
					
					// find other bluetooth devices around
					if(!ba.isDiscovering())
						ba.startDiscovery();
					
					
					
					//ba.cancelDiscovery();
					
				} catch (JSONException e) {}
			}
		});
		
		getTimer().schedule(getTask(), 0, 30000L);
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
	
	public List<String> getWifiNetworks() {
		List<String> wifi = new ArrayList<String>();
		
		return wifi;
	}
	
	public void stopUpdates() {
		if(ba.isDiscovering()) {
			ba.cancelDiscovery();
			ba.disable();
		}
	}

}