package org.witness.sscphase1;

import java.io.Serializable;

import net.londatiga.android.QuickAction;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.FrameLayout;

public class ImageRegion extends FrameLayout implements OnTouchListener, OnClickListener, Serializable {

	private static final long serialVersionUID = -244965540057504061L;

	float startX;
	float startY;
	float endX;
	float endY;
	
	int imageWidth;
	int imageHeight;
		
	public static final int EDIT_MODE = 0;
	public static final int NORMAL_MODE = 1;
	int mode = EDIT_MODE;
	
	public static final int OBSCURE = 0;
	public static final int ENCRYPT = 1;
	int whattodo = OBSCURE;
	
	public static final String SSC = "[Camera Obscura : ImageRegion] **************************** ";
			
	//public ImageRegion(Context context, String jsonVersion) {
		// Implement this from JSON
		//this(context, _scaledStartX, _scaledStartY, _scaledEndX, _scaledEndY, _scaledImageWidth, _scaledImageHeight, _imageWidth, _imageHeight, _backgroundColor);	
	//}
	
	Button leftCorner;
	Button rightCorner;
		
	public ImageRegion(
			Context context, 
			int _scaledStartX, int _scaledStartY, 
			int _scaledEndX, int _scaledEndY, 
			int _scaledImageWidth, int _scaledImageHeight, 
			int _imageWidth, int _imageHeight, 
			int _backgroundColor) 
	{
		super(context);
		
		/*
		original 300
		current 100
		scaled x 20
		real x 60
		original/current * scaled = real
		
		scaled = real * current/original
		*/
		
		startX = (float)_imageWidth/(float)_scaledImageWidth * (float)_scaledStartX;
		startY = (float)_imageHeight/(float)_scaledImageHeight * (float)_scaledStartY;
		endX = (float)_imageWidth/(float)_scaledImageWidth * (float)_scaledEndX;
		endY = (float)_imageHeight/(float)_scaledImageHeight * (float)_scaledEndY;
				
		imageWidth = _imageWidth;
		imageHeight = _imageHeight;
		
		setBackgroundColor(_backgroundColor);
		
		
		
		this.setOnClickListener(this);
	
		// FIgure out how to do layout
		///this.setLayout(R.layout.imageregion);
		// Implement buttons/whatever
		//rightCorner = (Button) 
		
	}
	
	public void changeMode(int newMode) {
		mode = newMode;
		if (mode == EDIT_MODE) {
			leftCorner.setVisibility(View.VISIBLE);
			rightCorner.setVisibility(View.VISIBLE);
		} else {
			leftCorner.setVisibility(View.GONE);
			rightCorner.setVisibility(View.GONE);
		}
	}
	
	public Rect getScaledRect(int _scaledImageWidth, int _scaledImageHeight) {
		
		float scaledStartX = (float)startX * (float)_scaledImageWidth/(float)imageWidth;
		float scaledStartY = (float)startY * (float)_scaledImageHeight/(float)imageHeight;
		float scaledEndX = (float)endX * (float)_scaledImageWidth/(float)imageWidth;
		float scaledEndY = (float)endY * (float)_scaledImageHeight/(float)imageHeight;

		return new Rect((int)scaledStartX, (int)scaledStartY, (int)scaledEndX, (int)scaledEndY);
	}

	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		return false;
	}

	public void onClick(View v) {
		Log.d(SSC,"CLICKED View " + v.toString());
		QuickAction qa = new QuickAction(v);
		qa.show();
		
	}
}
