package org.witness.sscphase1;

import java.io.Serializable;
import java.util.ArrayList;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;

import android.R.drawable;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.PopupWindow.OnDismissListener;

public class ImageRegion extends FrameLayout implements OnTouchListener, OnClickListener, Serializable {

	private static final long serialVersionUID = -244965540057504061L;

	float startX;
	float startY;
	float endX;
	float endY;
	
	int imageWidth;
	int imageHeight;
	
	/*
	public final static int DRAW_COLOR = Color.argb(128, 0, 255, 0);// Green
	public final static int DETECTED_COLOR = Color.argb(128, 0, 0, 255); // Blue
	public final int[] BGCOLOR = {
			Color.argb(200, 255, 255, 255),
			Color.argb(128, 176, 176, 176),
			Color.argb(128, 252, 194, 53)};  // see-thru, grey, yellow
	*/
	public static final int[] BKGDRAWABLES = {R.drawable.region_editbox,R.drawable.region_inactive,R.drawable.region_active};
	
	public static final int EDIT_MODE = 0;
	public static final int NORMAL_MODE = 1;
	public static final int ACTIVE_MODE = 2;
	int mode = ACTIVE_MODE;
	
	public static final int OBSCURE = 0;
	public static final int ENCRYPT = 1;
	int whattodo = OBSCURE;
	
	
	
	// QuickAction items
	String[] UIMenuItemNames;
	ArrayList<ActionItem> aiList;
	String testout;
	QuickAction qa;
	OnDismissListener dismissPopup = new OnDismissListener() {
		public void onDismiss() {
			//changeMode(mode);
		}
	};
	
	OnClickListener uiEditTag = new OnClickListener() {
		public void onClick(View v) {
			editTag();
		}	
	};
	
	OnClickListener uiIdTag = new OnClickListener() {
		public void onClick(View v) {
			idTag();
		}
	};
	
	OnClickListener uiEncryptTag = new OnClickListener() {
		public void onClick(View v) {
			encryptTag();
		}
	};
	
	OnClickListener uiDestroyTag = new OnClickListener() {
		public void onClick(View v) {
			destroyTag();
		}
	};
	
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
			int _imageWidth, int _imageHeight) 
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
		
		//setBackgroundColor(_backgroundColor);
		inflatePopup();
		this.setOnClickListener(this);
		changeMode(mode);
		setBackgroundDrawable(null);
		
	
		// FIgure out how to do layout
		///this.setLayout(R.layout.imageregion);
		// Implement buttons/whatever
		//rightCorner = (Button) 
		
	}
	
	public void inflatePopup() {
		aiList = new ArrayList<ActionItem>();
		UIMenuItemNames = this.getResources().getStringArray(R.array.UIMenuItemNames);
		OnClickListener[] ocl = {
			uiEditTag,
			uiIdTag,
			uiEncryptTag,
			uiDestroyTag
		};
		int[] UIMenuItemIcons = {
				R.drawable.ic_context_edit,
				R.drawable.ic_context_id,
				R.drawable.ic_context_encrypt,
				R.drawable.ic_context_destroy};

		for(int x=0;x<UIMenuItemNames.length;x++) {
			ActionItem ai = new ActionItem();
			ai.setTitle(UIMenuItemNames[x]);
			ai.setIcon(this.getResources().getDrawable(UIMenuItemIcons[x]));
			ai.setOnClickListener(ocl[x]);
			aiList.add(ai);
		}
	}
	
	public void changeMode(int newMode) {
		mode = newMode;
		Log.v(SSC,"WE ARE IN MODE " + mode + " NOW");
	}
	
	/*
	public void changeMode(int newMode) {
		mode = newMode;
		if (mode == EDIT_MODE) {
			leftCorner.setVisibility(View.VISIBLE);
			rightCorner.setVisibility(View.VISIBLE);
			this.setBackgroundColor(bgColors[mode]);
		} else {
			leftCorner.setVisibility(View.GONE);
			rightCorner.setVisibility(View.GONE);
		}
	}
	*/
	
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
		changeMode(ACTIVE_MODE);
		qa = new QuickAction(v);
		for(int x=0;x<aiList.size();x++) {
			qa.addActionItem(aiList.get(x));
		}
		qa.setAnimStyle(QuickAction.ANIM_REFLECT);
		qa.show();
		qa.setOnDismissListener(dismissPopup);
	}
	
	public void editTag() {
		qa.dismiss();
		changeMode(EDIT_MODE);
	}
	
	public void idTag() {
		qa.dismiss();
	}
	
	public void encryptTag() {
		qa.dismiss();
	}
	
	public void destroyTag() {
		qa.dismiss();
	}
}
