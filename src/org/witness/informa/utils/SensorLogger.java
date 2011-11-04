package org.witness.informa.utils;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

public class SensorLogger {
	Timer mTimer;
	TimerTask mTask;
	File mLog;
	
	JSONObject mBuffer;
	
	boolean hasLog;
	
	SensorLogger() {
		hasLog = false;
		mTimer = new Timer();
	}

	public void sendToBuffer(JSONObject logItem) {
		// append to buffer, and...
		
		if(hasLog)
			sendToLog(logItem);
		
	}
	
	public void sendToLog(JSONObject logItem) {
		
	}

	public boolean startLog(File log) {
		mLog = log;
		hasLog = true;
		return hasLog;
	}

	
	public boolean stopLog() {
		hasLog = false;
		return hasLog;
	}
	
	public JSONObject jPack(String key, Object val) {
		JSONObject item = null;
		
		return item;
	}
}
