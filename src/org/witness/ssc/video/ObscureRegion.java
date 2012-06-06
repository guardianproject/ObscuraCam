package org.witness.ssc.video;

import org.witness.sscphase1.ObscuraApp;

import android.graphics.RectF;
import android.util.Log;

public class ObscureRegion  {

	/*
	 * Thinking about whether or not a region should contain multiple start/end times
	 * realizing that doing this would make editing a real pita
	 * Of course, it would make displaying be a 1000x better though.
	class PositionTime {

		int sx = 0; 
		int sy = 0; 
		int ex = 0;
		int ey = 0;
		int startTime = 0; 
		int endTime = 0;
		
		PositionTime(int _sx, int _sy, int _ex, int _ey, int _startTime, int _endTime) {
			
		}
	}
	*/
	
	public static final String LOGTAG = ObscuraApp.TAG;

	public static final String MODE_REDACT = "black";
	public static final String MODE_MOSAIC = "pixel";
	
	public static final String DEFAULT_MODE = MODE_MOSAIC;

	public static final float DEFAULT_X_SIZE = 150;
	public static final float DEFAULT_Y_SIZE = 150;
		
	public float sx = 0;
	public float sy = 0;
	
	public float ex = 0;
	public float ey = 0;
		
	public int timeStamp = 0;
	
	public RegionTrail regionTrail;
	
	public String currentMode = DEFAULT_MODE;
	
	public ObscureRegion(int _timeStamp, float _sx, float _sy, float _ex, float _ey, String _mode) {
		
		timeStamp = _timeStamp;
		sx = _sx;
		sy = _sy;
		ex = _ex;
		ey = _ey;
		
		if (sx < 0) { 
			sx = 0;
		} else if (sy < 0) {
			sy = 0;
		}
		
		currentMode = _mode;

		
	}

	public ObscureRegion(int _startTime, float _sx, float _sy, float _ex, float _ey) {
		this(_startTime, _sx, _sy, _ex, _ey, DEFAULT_MODE);
	}

	public ObscureRegion(int _startTime, float _sx, float _sy) {
		this(_startTime, _sx - DEFAULT_X_SIZE/2, _sy - DEFAULT_Y_SIZE/2, _sx + DEFAULT_X_SIZE/2, _sy + DEFAULT_Y_SIZE/2, DEFAULT_MODE);
	}

	
	public void moveRegion(float _sx, float _sy) {
		moveRegion(_sx - DEFAULT_X_SIZE/2, _sy - DEFAULT_Y_SIZE/2, _sx + DEFAULT_X_SIZE/2, _sy + DEFAULT_Y_SIZE/2);
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
	
	

	public String getStringData(float widthMod, float heightMod, int duration) {
		//left, right, top, bottom
		return "" + (float)timeStamp/(float)1000 + ',' + (float)(timeStamp+duration)/(float)1000 + ',' + (int)(sx*widthMod) + ',' + (int)(ex*widthMod) + ',' + (int)(sy*heightMod) + ',' + (int)(ey*heightMod) + ',' + currentMode;
	}

	public RegionTrail getRegionTrail() {
		return regionTrail;
	}

	public void setRegionTrail(RegionTrail regionTrail) {
		this.regionTrail = regionTrail;
	}
}