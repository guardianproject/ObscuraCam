package org.witness.ssc.video;

import org.witness.sscphase1.ObscuraApp;

import android.graphics.RectF;
import android.util.Log;

public class ObscureRegion {

	/*
	 * Thinking about whether or not a region should contain multiple start/end times
	 * realizing that doing this would make editing a real pita
	 * Of course, it would make displaying be a 1000x better though.
	class PositionTime {

		int sx = 0; 
		int sy = 0; 
		int ex = 0;
		int ey = 0;
		long startTime = 0; 
		long endTime = 0;
		
		PositionTime(int _sx, int _sy, int _ex, int _ey, long _startTime, long _endTime) {
			
		}
	}
	*/
	
	public static final String LOGTAG = ObscuraApp.TAG;

	public static final String DEFAULT_COLOR = "pixel";

	public static final long DEFAULT_LENGTH = 10; // Seconds
	
	public static final float DEFAULT_X_SIZE = 150;
	public static final float DEFAULT_Y_SIZE = 150;
		
	public float sx = 0;
	public float sy = 0;
	
	public float ex = 0;
	public float ey = 0;
		
	public long startTime = 0;
	public long endTime = 0;
	
	public long mediaDuration = 0;
	
	public ObscureRegion(long _duration, long _startTime, long _endTime, float _sx, float _sy, float _ex, float _ey) {
		mediaDuration = _duration;
		startTime = _startTime;
		endTime = _endTime;
		sx = _sx;
		sy = _sy;
		ex = _ex;
		ey = _ey;
		
		if (sx < 0) { 
			sx = 0;
		} else if (sy < 0) {
			sy = 0;
		}

		
	}

	public ObscureRegion(long _duration, long _startTime, float _sx, float _sy, float _ex, float _ey) {
		this(_duration, _startTime, _startTime+DEFAULT_LENGTH, _sx, _sy, _ex, _ey);
	}

	public ObscureRegion(long _duration, long _startTime, long _endTime, float _sx, float _sy) {
		this(_duration, _startTime, _endTime, _sx - DEFAULT_X_SIZE/2, _sy - DEFAULT_Y_SIZE/2, _sx + DEFAULT_X_SIZE/2, _sy + DEFAULT_Y_SIZE/2);
	}

	public ObscureRegion(long _duration, long _startTime, float _sx, float _sy) {
		this(_duration, _startTime, _startTime+DEFAULT_LENGTH, _sx - DEFAULT_X_SIZE/2, _sy - DEFAULT_Y_SIZE/2, _sx + DEFAULT_X_SIZE/2, _sy + DEFAULT_Y_SIZE/2);
	}
	
	public void moveRegion(float _sx, float _sy) {
		this.moveRegion(_sx - DEFAULT_X_SIZE/2, _sy - DEFAULT_Y_SIZE/2, _sx + DEFAULT_X_SIZE/2, _sy + DEFAULT_Y_SIZE/2);
	}
	
	public void moveRegion(float _sx, float _sy, float _ex, float _ey) {
		sx = _sx;
		sy = _sy;
		ex = _ex;
		ey = _ey;
	}
	
	public RectF getRectF() {
		return new RectF(sx, sy, ex, ey);
	}
	
	public RectF getBounds() {
		return getRectF();
	}
	
	public boolean existsInTime(long time) {
		if (time < endTime && time >= startTime) {
			return true;
		}
		return false;
	}

	public String getStringData(float sizeMult) {
		//left, right, top, bottom
		return "" + (float)startTime/(float)1000 + "," + (float)endTime/(float)1000 + "," + (int)(sx*sizeMult) + "," + (int)(ex*sizeMult) + "," + (int)(sy*sizeMult) + "," + (int)(ey*sizeMult) + "," + DEFAULT_COLOR;
	}
}