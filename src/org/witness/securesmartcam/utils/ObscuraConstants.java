package org.witness.securesmartcam.utils;

import org.witness.sscphase1.R;

import android.content.Context;
import android.widget.Toast;

public class ObscuraConstants {
	public final static String TAG = "************ OBSCURA ***********";
	
	public final static int CAMERA_RESULT = 0;
	public final static int GALLERY_RESULT = 1;
	public final static int IMAGE_EDITOR = 2;
	
	public final static int ABOUT = 0;
	public final static int PREFS = 1;
	
	public final static String CAMERA_TMP_FILE = "ssctmp.jpg";
	public final static String MIME_TYPE_JPEG = "image/jpeg";
	
	public static final int NONE = 0;
	public static final int DRAG = 1;
	public static final int ZOOM = 2;
	public static final int TAP = 3;
	
	
	public final static String[] mFilterLabels = {"Redact","Pixelate","CrowdPixel","Identify"};
	public final static int[] mFilterIcons = {R.drawable.ic_context_fill,R.drawable.ic_context_pixelate,R.drawable.ic_context_pixelate, R.drawable.ic_context_id};
	
	// Maximum zoom scale
	public static final float MAX_SCALE = 10f;
	
	// Constant for autodetection dialog
	public static final int DIALOG_DO_AUTODETECTION = 0;
	
	// Colors for region squares
	public final static int DRAW_COLOR = 0x00000000;
	public final static int DETECTED_COLOR = 0x00000000;
	public final static int OBSCURED_COLOR = 0x00000000;
	
	// Constants for the menu items, currently these are in an XML file (menu/image_editor_menu.xml, strings.xml)
	public final static int ABOUT_MENU_ITEM = 0;
	public final static int DELETE_ORIGINAL_MENU_ITEM = 1;
	public final static int SAVE_MENU_ITEM = 2;
	public final static int SHARE_MENU_ITEM = 3;
	public final static int NEW_REGION_MENU_ITEM = 4;
	
	public final static class Preferences {
		public final static class Keys {
			public final static String LANGUAGE = "obscura.language";
		}
	}
	
	public final static class ImageRegion {
		public final static String PROPERTIES = "mProps";
	}
	
	public static void makeToast(Context c, String m) {
		Toast.makeText(c, m, Toast.LENGTH_LONG).show();
	}
}
