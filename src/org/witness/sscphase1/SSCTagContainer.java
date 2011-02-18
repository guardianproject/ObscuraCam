package org.witness.sscphase1;

import android.util.Log;
import android.widget.ImageButton;

public class SSCTagContainer {
	private static final String SSC = "[SSCCameraObscura : Tag Container] ****************************";
	ImageButton move;
	
	float x, y;
	
	public SSCTagContainer(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public float[] initNewTag() {
		float[] tagBounds = {x-76,y+76,x+76,y-76};
		return tagBounds;
	}
	
	public float[] getUpdatedTagBounds() {
		// return the tag's top-left and bottom-right 
		float[] tagBounds = new float[4];
		return tagBounds;
	}

}
