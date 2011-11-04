package org.witness.securesmartcam.io;

import java.util.Timer;
import java.util.TimerTask;

import org.witness.sscphase1.ObscuraApp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.util.Log;

public class DCIMMonitor extends Service {
	private DCIMObserver mDCIMObserver;
	
	String mPath = Environment.getExternalStorageDirectory().getPath() + "/DCIM/";
	Timer t;
	TimerTask tt;
	Runnable r;
	boolean shouldRun = false;

	public class LocalBinder extends Binder {
		public DCIMMonitor getService() {
			return DCIMMonitor.this;
		}
	}
	
	private final IBinder binder = new LocalBinder();
	
	@Override
	public void onCreate() {
		Log.d(ObscuraApp.TAG,"DCIM Monitor is on and observing path: " + mPath);
		
		mDCIMObserver = new DCIMObserver(mPath);
		mDCIMObserver.startWatching();
		
		t = new Timer();
		tt = new TimerTask() {

			@Override
			public void run() {
				Log.d(ObscuraApp.TAG,"********************* DCIM Monitor continues to observe ********************");
			}
			
		};
		
		r = new Runnable() {

			@Override
			public void run() {
				if(shouldRun)
					t.schedule(tt, 0, 30000L);
			}
			
		};
		
		shouldRun = true;
		r.run();
	}
	
	@Override
	public void onDestroy() {
		Log.d(ObscuraApp.TAG,"DCIM Monitor has been destroyed");
		shouldRun = false;
		mDCIMObserver.stopWatching();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	public class DCIMObserver extends FileObserver {
		
		public DCIMObserver(String path) {
			super(path);
		}

		public void alertFileModified() {
			// action to perform if new file is added to folder
		}

		@Override
		public void onEvent(int event, String path) {
			if(path == null) {
				Log.d(ObscuraApp.TAG,"************************* path is null?");
				return;
			}
			
			String e = "";
			if((FileObserver.CREATE & event) != 0)
				e = "file created";
			
			if((FileObserver.OPEN & event) != 0)
				e = "file opened";

			if((FileObserver.MODIFY & event) != 0)
				e = "file modified";
			
			if((FileObserver.ACCESS & event) != 0)
				e = "file accessed";
			
			if((FileObserver.CLOSE_NOWRITE & event) != 0)
				e = "file closed (non-write)";
			
			if((FileObserver.CLOSE_WRITE & event) != 0)
				e = "file closed (write)";
			
			if((FileObserver.DELETE & event) != 0)
				e = "file deleted";
			
			Log.d(ObscuraApp.TAG, "**************************** event: " + e + " on path: " + path);
			
		}

	}

}
