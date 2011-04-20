package org.witness.sscphase1;

import java.io.Serializable;
import java.util.ArrayList;

import org.witness.sscphase1.secure.EncryptTagger;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.util.Log;
import android.view.LayoutInflater;
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
	
	private ImageEditor imageEditor;
	
	// QuickAction items
	String[] UIMenuItemNames;
	ArrayList<ActionItem> aiList;

	QuickAction qa;
	
	public static final String SSC = "[Camera Obscura : ImageRegion] **************************** ";
	public static final String LOGTAG = SSC;
	//public ImageRegion(Context context, String jsonVersion) {
		// Implement this from JSON
		//this(context, _scaledStartX, _scaledStartY, _scaledEndX, _scaledEndY, _scaledImageWidth, _scaledImageHeight, _imageWidth, _imageHeight, _backgroundColor);	
	//}
	
	View topLeftCorner;
	View topRightCorner;
	View bottomLeftCorner;
	View bottomRightCorner;
	
	Context context;
		
	public ImageRegion(
			ImageEditor imageEditor, 
			int _scaledStartX, int _scaledStartY, 
			int _scaledEndX, int _scaledEndY, 
			int _scaledImageWidth, int _scaledImageHeight, 
			int _imageWidth, int _imageHeight, 
			int _backgroundColor,
			Context _context) 
	{
		super(imageEditor);
		context = _context;
		
		this.imageEditor = imageEditor;
		
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
		
		inflatePopup();

		this.setOnClickListener(this);
		
		// Inflate Layout
		LayoutInflater inflater = LayoutInflater.from(context);        
        View innerView = inflater.inflate(R.layout.imageregioninner, null);
        
        topLeftCorner = innerView.findViewById(R.id.TopLeftCorner);
        topRightCorner = innerView.findViewById(R.id.TopRightCorner);
        bottomLeftCorner = innerView.findViewById(R.id.BottomLeftCorner);
        bottomRightCorner = innerView.findViewById(R.id.BottomRightCorner);

		topLeftCorner.setVisibility(View.GONE);
		topRightCorner.setVisibility(View.GONE);
		bottomLeftCorner.setVisibility(View.GONE);
		bottomRightCorner.setVisibility(View.GONE);

        this.addView(innerView);
    		
	}
	
	public void inflatePopup() {
		// Not sure this works since we need to identify these later, in the onclick's
		
		aiList = new ArrayList<ActionItem>();
		UIMenuItemNames = this.getResources().getStringArray(R.array.UIMenuItemNames);
		int[] UIMenuItemIcons = {
				R.drawable.ic_context_edit,
				R.drawable.ic_context_id,
				R.drawable.ic_context_encrypt,
				R.drawable.ic_context_destroy};
		for(int x=0;x<UIMenuItemNames.length;x++) {
			ActionItem ai = new ActionItem();
			ai.setTitle(UIMenuItemNames[x]);			
			ai.setIcon(this.getResources().getDrawable(UIMenuItemIcons[x]));
			ai.setOnClickListener(new OnClickListener() {
				
				public void onClick(View v) {
					
					Log.v(SSC,"YOU CLICKED OPTION " + v.toString());
					v.getId();
					
					ImageRegion.this.changeMode(EDIT_MODE);
					/*
					public static final int EDIT_MODE = 0;
					public static final int NORMAL_MODE = 1;
					*/
				}
			});
			
			aiList.add(ai);
		}		
	}
	
	
	    
	public void changeMode(int newMode) {
		mode = newMode;
		if (mode == EDIT_MODE) {
			topLeftCorner.setVisibility(View.VISIBLE);
			topRightCorner.setVisibility(View.VISIBLE);
			bottomLeftCorner.setVisibility(View.VISIBLE);
			bottomRightCorner.setVisibility(View.VISIBLE);
		} else {
			topLeftCorner.setVisibility(View.GONE);
			topRightCorner.setVisibility(View.GONE);
			bottomLeftCorner.setVisibility(View.GONE);
			bottomRightCorner.setVisibility(View.GONE);
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
		if (v == this) {
			qa = new QuickAction(v);
			for(int x=0;x<aiList.size();x++) {
				qa.addActionItem(aiList.get(x));
			}
			
			qa.setAnimStyle(QuickAction.ANIM_REFLECT);
			qa.show();
		} 
	}
}
