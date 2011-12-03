package org.witness.informa.utils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.witness.informa.utils.suckers.GeoSucker;
import org.witness.sscphase1.ObscuraApp;

import android.content.Context;
import android.util.Log;

public class SensorLogger<T> {
	public T _sucker;
	
	Timer mTimer = new Timer();
	TimerTask mTask;
	
	File mLog;
	JSONArray mBuffer;
	
	boolean isRunning;
	
	public static Context _c;
	
	public SensorLogger(Context c) {
		_c = c;
		mBuffer = new JSONArray();
		isRunning = true;
	}
	
	public T getSucker() {
		return _sucker;
	}
	
	public void setSucker(T sucker) {
		_sucker = sucker;
	}
	
	public JSONArray getLog() {
		return mBuffer;
	}

	public Timer getTimer() {
		return mTimer;
	}
	
	public TimerTask getTask() {
		return mTask;
	}
	
	public void setTask(TimerTask task) {
		mTask = task;
	}
	
	public void setIsRunning(boolean b) {
		isRunning = b;
		if(!b)
			mTimer.cancel();
	}
	
	public boolean getIsRunning() {
		return isRunning;
	}
	
	// LOL reflection...
	public JSONObject returnCurrent() throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		JSONObject current = new JSONObject();
		
		if(_sucker.getClass().getDeclaredMethod("forceReturn", null) != null) {
			Method fr = _sucker.getClass().getDeclaredMethod("forceReturn", null);
			current = (JSONObject) fr.invoke(_sucker, null);
		} else {
			current = null;
		}
		
		return current;
	}

	public void sendToBuffer(JSONObject logItem) throws JSONException {
		// TODO: append to buffer, and...
		if(mBuffer.length() > 60) {
			mBuffer = null;
			mBuffer = new JSONArray();
			Log.d(ObscuraApp.TAG, "LOG CLEARED");
		}
		
		logItem.put("ts", new Date().getTime());
		mBuffer.put(logItem);		
	}
	
	public JSONObject jPack(String key, Object val) throws JSONException {
		JSONObject item = new JSONObject();
		item.put(key, val.toString());
		return item;
	}
}
