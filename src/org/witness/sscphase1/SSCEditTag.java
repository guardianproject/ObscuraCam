package org.witness.sscphase1;

import org.json.JSONObject;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.PopupWindow;

public class SSCEditTag {
	private static final String SSC = "[SSCCameraObscura : Edit Tag] ****************************";
	
	private SSCCalculator calc;
	protected JSONObject jsonTag;
	
	protected final View anchor;
	private View root;
	private ViewGroup popupTracks;
	protected final PopupWindow win;
	protected final WindowManager wm;
	private final LayoutInflater li;
	
	protected static int vID;
	protected static float[] vCoords;
	
	public SSCEditTag(CharSequence vCoordString, View anchor) {
		this.anchor = anchor;
		this.win = new PopupWindow(anchor.getContext());
		
		li = (LayoutInflater) anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		root = (ViewGroup) li.inflate(R.layout.popup, null);
		setContentView(root);
		
		popupTracks = (ViewGroup) root.findViewById(R.id.popupTracks);
		for(int x=0;x<4;x++) {
			popupTracks.getChildAt(x).setFocusable(true);
			popupTracks.getChildAt(x).setClickable(true);
		}
		
		calc = new SSCCalculator();
		try {
			jsonTag = calc.jsonGetAll(vCoordString.toString());
			vID = jsonTag.getInt("id");
			vCoords = calc.jsonDeflate(1,vCoordString.toString());
			Log.v(SSC,"OK, json results says this tag\'s id is= " + vID);
		} catch (Exception e) {
			Log.d(SSC,"JSON ERROR MOTHERFUCK: " + e);
		}
		wm = (WindowManager) anchor.getContext().getSystemService(Context.WINDOW_SERVICE);
		onCreate();
		preShow();
	}
	
	public int[] getButtonIDs() {
		int[] buttonIDs = new int[4];
		for(int x=0;x<4;x++) {
			buttonIDs[x] = popupTracks.getChildAt(x).getId();
			popupTracks.getChildAt(x).setContentDescription("{\"id\":" + vID + "}");
		}
		return buttonIDs;
	}
	
	public void addActions(OnClickListener ocl) {
		for(int x=0;x<4;x++) {
			popupTracks.getChildAt(x).setOnClickListener(ocl);
		}
	}
	
	public void show() {
		win.showAtLocation(this.anchor,Gravity.NO_GRAVITY,(int) vCoords[0],(int) vCoords[1]);
	}
	
	protected void onCreate() {
		Log.v(SSC,"window made.");
	}
	
	protected void onShow() {}
	
	protected void preShow() {
		if(root == null) {
			throw new IllegalStateException("setContentView was not called with a view to display");
		}
		onShow();
		win.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		win.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		win.setTouchable(true);
		win.setFocusable(true);
		win.setOutsideTouchable(true);
		win.setContentView(root);		
	}
	
	public void setBackgroundDrawable(Drawable bg) {
		// set the background of the window if needed?
	}
	
	public void setContentView(View root) {
		this.root = root;
		win.setContentView(root);
	}
	
	public void setContentView(int layoutID) {
		LayoutInflater li = (LayoutInflater) anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		setContentView(li.inflate(layoutID, null));
	}
	
	public void dismiss() {
		win.dismiss();
	}
}
