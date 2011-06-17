package org.witness.sscphase1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class ImageEditor extends Activity implements OnTouchListener, OnClickListener, OnLongClickListener {

	final static String LOGTAG = "[Camera Obscura ImageEditor]";

	/* This isn't necessary 
	// Maximum dimension for sharing
	//public final static int SHARE_SIZE_MAX_WIDTH_HEIGHT = 320;
	*/
	
	// Colors for region squares
	public final static int DRAW_COLOR = Color.argb(200, 0, 255, 0);// Green
	public final static int DETECTED_COLOR = Color.argb(200, 0, 0, 255); // Blue
	public final static int OBSCURED_COLOR = Color.argb(200, 255, 0, 0); // Red

	// Constants for the menu items, currently these are in an XML file (menu/image_editor_menu.xml, strings.xml)
	public final static int ABOUT_MENU_ITEM = 0;
	public final static int DELETE_ORIGINAL_MENU_ITEM = 1;
	public final static int SAVE_MENU_ITEM = 2;
	public final static int SHARE_MENU_ITEM = 3;
	public final static int NEW_REGION_MENU_ITEM = 4;
	
	// Image Matrix
	Matrix matrix = new Matrix();

	// Saved Matrix for not allowing a current operation (over max zoom)
	Matrix savedMatrix = new Matrix();
		
	// We can be in one of these 3 states
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	static final int TAP = 3;
	int mode = NONE;

	// Default ImageRegion width and height
	static final int DEFAULT_REGION_WIDTH = 160;
	static final int DEFAULT_REGION_HEIGHT = 160;
	
	// Maximum zoom scale
	static final float MAX_SCALE = 10f;
	
	// Constant for autodetection dialog
	static final int DIALOG_DO_AUTODETECTION = 0;
	
	// For Zooming
	float startFingerSpacing = 0f;
	float endFingerSpacing = 0f;
	PointF startFingerSpacingMidPoint = new PointF();
	
	// For Dragging
	PointF startPoint = new PointF();
	
	// Don't allow it to move until the finger moves more than this amount
	// Later in the code, the minMoveDistance in real pixels is calculated
	// to account for different touch screen resolutions
	float minMoveDistanceDP = 5f;
	float minMoveDistance; // = ViewConfiguration.get(this).getScaledTouchSlop();
	
	// zoom in and zoom out buttons
	Button zoomIn, zoomOut;
	
	// ImageView for the original (scaled) image
	ImageView imageView;
	
	// Layout that all of the ImageRegions will be in
	RelativeLayout regionButtonsLayout;
		
	// Bitmap for the original image (scaled)
	Bitmap imageBitmap;
		
	// Vector to hold ImageRegions 
	Vector<ImageRegion> imageRegions = new Vector<ImageRegion>(); 
		
	// The original image dimensions (not scaled)
	int originalImageWidth;
	int originalImageHeight;

	// So we can give some haptic feedback to the user
	Vibrator vibe;

	// Original Image Uri
	Uri originalImageUri;
	
	// Saved Image Uri
	Uri savedImageUri;
	
	// Constant for temp filename
	private final static String TMP_FILE_NAME = "temporary.jpg";
	private final static String TMP_FILE_DIRECTORY = "/Android/data/org.witness.sscphase1/files/";
	
	// Temporary Image Uri
	Uri tmpImageUri;
	
	//handles threaded events for the UI thread
    private Handler mHandler = new Handler();
    
    // Handles when we should do realtime preview and when we shouldn't
    boolean doRealtimePreview = true;

    private Runnable mUpdateTimeTask = new Runnable() {
    	   public void run() {
    		   doAutoDetection();
    	   }
    	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.imageviewer);

		// Calculate the minimum distance
		minMoveDistance = minMoveDistanceDP * this.getResources().getDisplayMetrics().density + 0.5f;
		
		// The ImageView that contains the image we are working with
		imageView = (ImageView) findViewById(R.id.ImageEditorImageView);
		
		// Buttons for zooming
		zoomIn = (Button) this.findViewById(R.id.ZoomIn);
		zoomOut = (Button) this.findViewById(R.id.ZoomOut);

		// this, ImageEditor will be the onClickListener for the buttons
		zoomIn.setOnClickListener(this);
		zoomOut.setOnClickListener(this);

		// Passed in from CameraObscuraMainMenu
		originalImageUri = getIntent().getData();
		
		// If originalImageUri is null, we are likely coming from another app via "share"
		if (originalImageUri == null && getIntent().hasExtra(Intent.EXTRA_STREAM)) {
			originalImageUri = (Uri) getIntent().getExtras().get(Intent.EXTRA_STREAM);
		}

		Log.v(LOGTAG,"The Path" + pullPathFromUri(originalImageUri));
		
		// Instantiate the vibrator
		vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		// Load the image if it isn't null
		if (originalImageUri != null) {
			// Load up smaller image
			try {
				// Load up the image's dimensions not the image itself
				BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
				bmpFactoryOptions.inJustDecodeBounds = true;

				// Needs to be this config for Google Face Detection 
				bmpFactoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
				
				// Parse the image
				imageBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(originalImageUri), null, bmpFactoryOptions);

				// Hold onto the unscaled dimensions
				originalImageWidth = bmpFactoryOptions.outWidth;
				originalImageHeight = bmpFactoryOptions.outHeight;
				
				// Get the current display to calculate ratios
				Display currentDisplay = getWindowManager().getDefaultDisplay();

				Log.v(LOGTAG,"Display Width: " + currentDisplay.getWidth());
				Log.v(LOGTAG,"Display Height: " + currentDisplay.getHeight());
				
				Log.v(LOGTAG,"Image Width: " + originalImageWidth);
				Log.v(LOGTAG,"Image Height: " + originalImageHeight);
				
				// Ratios between the display and the image
				int widthRatio = (int) Math.floor(bmpFactoryOptions.outWidth / (float) currentDisplay.getWidth());
				int heightRatio = (int) Math.floor(bmpFactoryOptions.outHeight / (float) currentDisplay.getHeight());

				Log.v(LOGTAG, "HEIGHTRATIO:" + heightRatio);
				Log.v(LOGTAG, "WIDTHRATIO:" + widthRatio);
	
				// If both of the ratios are greater than 1,
				// one of the sides of the image is greater than the screen
				if (heightRatio > 1 && widthRatio > 1) {
					if (heightRatio > widthRatio) {
						// Height ratio is larger, scale according to it
						bmpFactoryOptions.inSampleSize = heightRatio;
					} else {
						// Width ratio is larger, scale according to it
						bmpFactoryOptions.inSampleSize = widthRatio;
					}
				}
		
				// Decode it for real
				bmpFactoryOptions.inJustDecodeBounds = false;
				imageBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(originalImageUri), null, bmpFactoryOptions);
				Log.v(LOGTAG,"Was: " + imageBitmap.getConfig());

				if (imageBitmap == null) {
					Log.v(LOGTAG,"bmp is null");
				}
				
				float matrixWidthRatio = (float) currentDisplay.getWidth() / (float) imageBitmap.getWidth();
				float matrixHeightRatio = (float) currentDisplay.getHeight() / (float) imageBitmap.getHeight();

				// Setup the imageView and matrix for scaling
				float matrixScale = matrixHeightRatio;
				
				if (matrixWidthRatio < matrixHeightRatio) {
					matrixScale = matrixWidthRatio;
				} 
				
				imageView.setImageBitmap(imageBitmap);
				
				PointF midpoint = new PointF((float)imageBitmap.getWidth()/2f, (float)imageBitmap.getHeight()/2f);
				matrix.postScale(matrixScale, matrixScale);

				// This doesn't completely center the image but it get's closer
				int fudge = 42;
				matrix.postTranslate((float)((float)currentDisplay.getWidth()-(float)imageBitmap.getWidth()*(float)matrixScale)/2f,(float)((float)currentDisplay.getHeight()-(float)imageBitmap.getHeight()*matrixScale)/2f-fudge);
				
				imageView.setImageMatrix(matrix);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// Set the OnTouch and OnLongClick listeners to this (ImageEditor)
			imageView.setOnTouchListener(this);
			imageView.setOnLongClickListener(this);
			
			// Layout for Image Regions
			regionButtonsLayout = (RelativeLayout) this.findViewById(R.id.RegionButtonsLayout);
			
			// Do auto detect popup
			Toast autodetectedToast = Toast.makeText(this, "Detecting faces...", Toast.LENGTH_SHORT);
			autodetectedToast.show();
			mHandler.postDelayed(mUpdateTimeTask, 1000);
		}
	}
	
	/*
	 * Call this to delete the original image, will ask the user
	 */
	private void handleDelete() 
	{
		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setIcon(android.R.drawable.ic_dialog_alert);
		b.setTitle(getString(R.string.app_name));
		b.setMessage(getString(R.string.confirm_delete));
		b.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                // User clicked OK so go ahead and delete
        		deleteOriginal();
            }
        });
		b.setNegativeButton(android.R.string.no, null);
		b.show();
	}
	
	/*
	 * Actual deletion of original
	 */
	private void deleteOriginal() {
		getContentResolver().delete(originalImageUri, null, null);
		originalImageUri = null;
	}
	
	/*
	 * Ask user to approve auto detection
	 */
	private void askToDoAutoDetect() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Scan Image?");
		builder.setMessage("Would you like to scan this image for faces?");
		builder.setCancelable(false);
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                ImageEditor.this.doAutoDetection();
		           }
		       });
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		builder.show();
	}
	
	/*
	 * Do actual auto detection and create regions
	 * 
	 * public void createImageRegion(int _scaledStartX, int _scaledStartY, 
			int _scaledEndX, int _scaledEndY, 
			int _scaledImageWidth, int _scaledImageHeight, 
			int _imageWidth, int _imageHeight, 
			int _backgroundColor) {
	 */
	
	private void doAutoDetection() {
		// This should be called via a pop-up/alert mechanism
		
		RectF[] autodetectedRects = runFaceDetection();
		for (int adr = 0; adr < autodetectedRects.length; adr++) {

			Log.v(LOGTAG,"AUTODETECTED imageView Width, Height: " + imageView.getWidth() + " " + imageView.getHeight());
			Log.v(LOGTAG,"UNSCALED RECT:" + autodetectedRects[adr].left + " " + autodetectedRects[adr].top + " " + autodetectedRects[adr].right + " " + autodetectedRects[adr].bottom);
			
			float scaledStartX = (float)autodetectedRects[adr].left * (float)imageView.getWidth()/(float)imageBitmap.getWidth();
			float scaledStartY = (float)autodetectedRects[adr].top * (float)imageView.getHeight()/(float)imageBitmap.getHeight();
			float scaledEndX = (float)autodetectedRects[adr].right * (float)imageView.getWidth()/(float)imageBitmap.getWidth();
			float scaledEndY = (float)autodetectedRects[adr].bottom * (float)imageView.getHeight()/(float)imageBitmap.getHeight();
			
			RectF autodetectedRectScaled = new RectF(scaledStartX, scaledStartY, scaledEndX, scaledEndY);
			Log.v(LOGTAG,"SCALED RECT:" + autodetectedRectScaled.left + " " + autodetectedRectScaled.top + " " + autodetectedRectScaled.right + " " + autodetectedRectScaled.bottom);

			
			// Probably need to map autodetectedRects to scaled rects
			//matrix.mapRect(autodetectedRects[adr]);		
			//Log.v(LOGTAG,"MAPPED RECT:" + autodetectedRects[adr].left + " " + autodetectedRects[adr].top + " " + autodetectedRects[adr].right + " " + autodetectedRects[adr].bottom);
			
			float faceBuffer = (autodetectedRectScaled.right-autodetectedRectScaled.left)/5;
			
			boolean showPopup = false;
			if (adr == autodetectedRects.length - 1) {
				showPopup = true;
			}
			createImageRegion(
					(int)(autodetectedRectScaled.left-faceBuffer),
					(int)(autodetectedRectScaled.top-faceBuffer),
					(int)(autodetectedRectScaled.right+faceBuffer),
					(int)(autodetectedRectScaled.bottom+faceBuffer),
					imageView.getWidth(),
					imageView.getHeight(),
					originalImageWidth, 
					originalImageHeight, 
					DETECTED_COLOR,
					showPopup);
		}	
			/*
			createImageRegion(
					(int)autodetectedRects[adr].left,
					(int)autodetectedRects[adr].top,
					(int)autodetectedRects[adr].right,
					(int)autodetectedRects[adr].bottom,
					imageView.getWidth(),
					imageView.getHeight(),
					originalImageWidth, 
					originalImageHeight, 
					DETECTED_COLOR);
		}
		*/
		
		Toast autodetectedToast = Toast.makeText(this, "" + autodetectedRects.length + " faces detected", Toast.LENGTH_SHORT);
		autodetectedToast.show();
	}
	
	/*
	 * The actual face detection calling method
	 */
	private RectF[] runFaceDetection() {
		GoogleFaceDetection gfd = new GoogleFaceDetection(imageBitmap);
		int numFaces = gfd.findFaces();
        Log.v(LOGTAG,"Num Faces Found: " + numFaces); 
        RectF[] possibleFaceRects = gfd.getFaces();
		return possibleFaceRects;				
	}
	
	/*
	 * Handles touches on ImageView
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) 
	{
		boolean handled = false;
	
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				// Single Finger down
				mode = TAP;
				
				// Don't do realtime preview while touching screen
				//doRealtimePreview = false;
				//updateDisplayImage();

				// Save the Start point. 
				startPoint.set(event.getX(), event.getY());
						
				clearImageRegionsEditMode();
				
				break;
				
			case MotionEvent.ACTION_POINTER_DOWN:
				// Two Fingers down

				// Don't do realtime preview while touching screen
				//doRealtimePreview = false;
				//updateDisplayImage();

				// Get the spacing of the fingers, 2 fingers
				float sx = event.getX(0) - event.getX(1);
				float sy = event.getY(0) - event.getY(1);
				startFingerSpacing = (float) Math.sqrt(sx * sx + sy * sy);

				//Log.d(LOGTAG, "Start Finger Spacing=" + startFingerSpacing);
				
				// Get the midpoint
				float xsum = event.getX(0) + event.getX(1);
				float ysum = event.getY(0) + event.getY(1);
				startFingerSpacingMidPoint.set(xsum / 2, ysum / 2);
				
				mode = ZOOM;
				//Log.d(LOGTAG, "mode=ZOOM");
				
				clearImageRegionsEditMode();
				break;
				
			case MotionEvent.ACTION_UP:
				// Single Finger Up
				
			    // Re-enable realtime preview
				doRealtimePreview = true;
				updateDisplayImage();
				
				mode = NONE;
				//Log.v(LOGTAG,"mode=NONE");

				break;
				
			case MotionEvent.ACTION_POINTER_UP:
				// Multiple Finger Up
			    // Re-enable realtime preview
				doRealtimePreview = true;
				updateDisplayImage();
				mode = NONE;
				//Log.d(LOGTAG, "mode=NONE");
				break;
				
			case MotionEvent.ACTION_MOVE:
				
				// Calculate distance moved
				float distance = (float) (Math.sqrt(Math.abs(startPoint.x - event.getX()) + Math.abs(startPoint.y - event.getY())));
				//Log.v(LOGTAG,"Move Distance: " + distance);
				//Log.v(LOGTAG,"Min Distance: " + minMoveDistance);
				
				// If greater than minMoveDistance, it is likely a drag or zoom
				if (distance > minMoveDistance) {
					if (mode == TAP || mode == DRAG) {
						mode = DRAG;
						
						matrix.postTranslate(event.getX() - startPoint.x, event.getY() - startPoint.y);
						imageView.setImageMatrix(matrix);
						// Reset the start point
						startPoint.set(event.getX(), event.getY());
	
						putOnScreen();
						redrawRegions();
						handled = true;
					} else if (mode == ZOOM) {
						// Save the current matrix so that if zoom goes to big, we can revert
						savedMatrix.set(matrix);
						// Get the spacing of the fingers, 2 fingers
						float ex = event.getX(0) - event.getX(1);
						float ey = event.getY(0) - event.getY(1);
						endFingerSpacing = (float) Math.sqrt(ex * ex + ey * ey);
	
						//Log.d(LOGTAG, "End Finger Spacing=" + endFingerSpacing);
		
						// If we moved far enough
						if (endFingerSpacing > minMoveDistance) {
							
							// Ratio of spacing..  If it was 5 and goes to 10 the image is 2x as big
							float scale = endFingerSpacing / startFingerSpacing;
							// Scale from the midpoint
							matrix.postScale(scale, scale, startFingerSpacingMidPoint.x, startFingerSpacingMidPoint.y);
		
							// Make sure that the matrix isn't bigger than max scale/zoom
							float[] matrixValues = new float[9];
							matrix.getValues(matrixValues);
							/*
							Log.v(LOGTAG, "Total Scale: " + matrixValues[0]);
							Log.v(LOGTAG, "" + matrixValues[0] + " " + matrixValues[1]
									+ " " + matrixValues[2] + " " + matrixValues[3]
									+ " " + matrixValues[4] + " " + matrixValues[5]
									+ " " + matrixValues[6] + " " + matrixValues[7]
									+ " " + matrixValues[8]);
							*/
							// x = 1.5 * 1 + 0 * y + -120 * 1
							
							if (matrixValues[0] > MAX_SCALE) {
								matrix.set(savedMatrix);
							}
							imageView.setImageMatrix(matrix);
							
							putOnScreen();
							redrawRegions();
	
							// Reset Start Finger Spacing
							float esx = event.getX(0) - event.getX(1);
							float esy = event.getY(0) - event.getY(1);
							startFingerSpacing = (float) Math.sqrt(esx * esx + esy * esy);
							//Log.d(LOGTAG, "New Start Finger Spacing=" + startFingerSpacing);
							
							// Reset the midpoint
							float x_sum = event.getX(0) + event.getX(1);
							float y_sum = event.getY(0) + event.getY(1);
							startFingerSpacingMidPoint.set(x_sum / 2, y_sum / 2);
							handled = true;
						}
					}
				}
				break;
		}
		return handled; // indicate event was handled
	}
	
	/*
	 * For live previews
	 */	
	public void updateDisplayImage()
	{
		if (doRealtimePreview) {
			imageView.setImageBitmap(createObscuredBitmap(imageBitmap.getWidth(),imageBitmap.getHeight()));
		} else {
			imageView.setImageBitmap(imageBitmap);
		}
	}
	
	/*
	 * Move the image onto the screen if it has been moved off
	 */
	public void putOnScreen() 
	{
		// Get Rectangle of Tranformed Image
		RectF theRect = getScaleOfImage();
		
		Log.v(LOGTAG,theRect.width() + " " + theRect.height());
		
		float deltaX = 0, deltaY = 0;
		if (theRect.width() < imageView.getWidth()) {
			deltaX = (imageView.getWidth() - theRect.width())/2 - theRect.left;
		} else if (theRect.left > 0) {
			deltaX = -theRect.left;
		} else if (theRect.right < imageView.getWidth()) {
			deltaX = imageView.getWidth() - theRect.right;
		}		
		
		if (theRect.height() < imageView.getHeight()) {
			deltaY = (imageView.getHeight() - theRect.height())/2 - theRect.top;
		} else if (theRect.top > 0) {
			deltaY = -theRect.top;
		} else if (theRect.bottom < imageView.getHeight()) {
			deltaY = imageView.getHeight() - theRect.bottom;
		}
		
		Log.v(LOGTAG,"Deltas:" + deltaX + " " + deltaY);
		
		matrix.postTranslate(deltaX,deltaY);
		updateDisplayImage();
		imageView.setImageMatrix(matrix);
	}
	
	/* 
	 * Put all regions into normal mode, out of edit mode
	 */
	public void clearImageRegionsEditMode()
	{
		Iterator<ImageRegion> itRegions = imageRegions.iterator();
		
		while (itRegions.hasNext())	{
			itRegions.next().changeMode(ImageRegion.NORMAL_MODE);
		}
	}
	
	/*
	 * Create new ImageRegion
	 */
	public void createImageRegion(int _scaledStartX, int _scaledStartY, 
			int _scaledEndX, int _scaledEndY, 
			int _scaledImageWidth, int _scaledImageHeight, 
			int _imageWidth, int _imageHeight, 
			int _backgroundColor, boolean showPopup) {
		
		clearImageRegionsEditMode();
		
		ImageRegion imageRegion = new ImageRegion(
				this, 
				_scaledStartX, 
				_scaledStartY, 
				_scaledEndX, 
				_scaledEndY, 
				_scaledImageWidth, 
				_scaledImageHeight, 
				_imageWidth, 
				_imageHeight, 
				_backgroundColor);
		
		imageRegions.add(imageRegion);
		addImageRegionToLayout(imageRegion,showPopup);
	}
	
	/*
	 * Delete/Remove specific ImageRegion
	 */
	public void deleteRegion(ImageRegion ir)
	{
		imageRegions.remove(ir);
		redrawRegions();
		updateDisplayImage();
	}
	
	/*
	 * Returns the Rectangle of Tranformed Image
	 */
	public RectF getScaleOfImage() 
	{
		RectF theRect = new RectF(0,0,imageBitmap.getWidth(), imageBitmap.getHeight());
		matrix.mapRect(theRect);
		return theRect;
	}

	/*
	 * Add an ImageRegion to the layout
	 */
	public void addImageRegionToLayout(ImageRegion imageRegion, boolean showPopup) 
	{
		// Get Rectangle of Current Transformed Image
		RectF theRect = getScaleOfImage();
		Log.v(LOGTAG,"New Width:" + theRect.width());
		imageRegion.updateScaledRect((int)theRect.width(), (int)theRect.height());
				
    	regionButtonsLayout.addView(imageRegion);
    	if (showPopup) {
    		imageRegion.inflatePopup(true);
    	}
    }
	
	/*
	 * Removes and adds all of the regions to the layout again 
	 */
	public void redrawRegions() {
		regionButtonsLayout.removeAllViews();
		
		Iterator<ImageRegion> i = imageRegions.iterator();
	    while (i.hasNext()) {
	    	ImageRegion currentRegion = i.next();
	    	addImageRegionToLayout(currentRegion, false);
	    }
	}
	
	/*
	 * Handles normal onClicks for buttons registered to this.
	 * Currently only the zoomIn and zoomOut buttons
	 */
	@Override
	public void onClick(View v) {
		if (v == zoomIn) 
		{
			float scale = 1.5f;
			
			PointF midpoint = new PointF(imageView.getWidth()/2, imageView.getHeight()/2);
			matrix.postScale(scale, scale, midpoint.x, midpoint.y);
			imageView.setImageMatrix(matrix);
			putOnScreen();
			redrawRegions();
		} 
		else if (v == zoomOut) 
		{
			float scale = 0.75f;

			PointF midpoint = new PointF(imageView.getWidth()/2, imageView.getHeight()/2);
			matrix.postScale(scale, scale, midpoint.x, midpoint.y);
			imageView.setImageMatrix(matrix);
			putOnScreen();
			redrawRegions();
		} 
	}
	
	// Long Clicks create new image regions
	@Override
	public boolean onLongClick (View v)
	{
		if (mode != DRAG && mode != ZOOM) {
			vibe.vibrate(50);
			
			float scaledStartX = (float)startPoint.x-DEFAULT_REGION_WIDTH/2 * (float)imageView.getWidth()/(float)imageBitmap.getWidth();
			float scaledStartY = (float)startPoint.y-DEFAULT_REGION_HEIGHT/2 * (float)imageView.getHeight()/(float)imageBitmap.getHeight();
			float scaledEndX = (float)startPoint.x+DEFAULT_REGION_WIDTH/2 * (float)imageView.getWidth()/(float)imageBitmap.getWidth();
			float scaledEndY = (float)startPoint.y+DEFAULT_REGION_HEIGHT/2 * (float)imageView.getHeight()/(float)imageBitmap.getHeight();

			createImageRegion((int)scaledStartX, (int)scaledStartY, 
							(int)scaledEndX, (int)scaledEndY, 
							imageView.getWidth(), imageView.getHeight(), 
							originalImageWidth, originalImageHeight, DRAW_COLOR, false);
			return true;
		}
		return false;
	}
	
	/*
	 * Standard method for menu items.  Uses res/menu/image_editor_menu.xml
	 */
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.image_editor_menu, menu);

        return true;
    }
	
    /*
     * Normal menu item selected method.  Uses menu items defined in XML: res/menu/image_editor_menu.xml
     */
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {

    	switch (item.getItemId()) {
    		case R.id.menu_new_region:
    			// Set the Start point. 
				startPoint.set(imageView.getWidth()/2, imageView.getHeight()/2);
				
    			// Add new region at default location (center)
    			createImageRegion((int)startPoint.x-DEFAULT_REGION_WIDTH/2, 
    					(int)startPoint.y-DEFAULT_REGION_HEIGHT/2, 
    					(int)startPoint.x+DEFAULT_REGION_WIDTH/2, 
    					(int)startPoint.y+DEFAULT_REGION_HEIGHT/2, 
    					imageView.getWidth(), 
    					imageView.getHeight(), 
    					originalImageWidth, 
    					originalImageHeight, 
    					DRAW_COLOR, false);

    			return true;
    			
        	case R.id.menu_save:
        		// Save Image
        		saveImage();
        		
        		return true;
        		
        	case R.id.menu_share:
        		// Share Image
        		shareImage();
        		
        		return true;
        	
/*
 			case R.id.menu_delete_original:
        		// Delete Original Image
        		handleDelete();
        		
        		return true;
*/        		
        	case R.id.menu_about:
        		// Pull up about screen
        		displayAbout();
        		
        		return true;
        	
        	case R.id.menu_preview:
        		showPreview();
        		
        		return true;
        		
    		default:
    			return false;
    	}
    }
	/*
	 * Display the about screen
	 */
	private void displayAbout() {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse("https://guardianproject.info/apps/securecam/about-v1/"));
		startActivity(i);
	}
	
	/*
	 * Display preview image
	 */
	private void showPreview() {
		// Open Preview Activity
		saveTmpImage();
		
		Intent intent = new Intent(this, ImagePreview.class);
    	intent.putExtra(ImagePreview.IMAGEURI, tmpImageUri.toString());
		startActivity(intent);				
	}
	
	/*
	 * When the user selects the Share menu item
	 * Uses saveTmpImage (overwriting what is already there) and uses the standard Android Share Intent
	 */
    private void shareImage() {
    	if (saveTmpImage()) {
        	Intent share = new Intent(Intent.ACTION_SEND);
        	share.setType("image/jpeg");
        	share.putExtra(Intent.EXTRA_STREAM, tmpImageUri);
        	startActivity(Intent.createChooser(share, "Share Image"));    	
    	} else {
    		Toast t = Toast.makeText(this,"Saving Temporary File Failed!", Toast.LENGTH_SHORT); 
    		t.show();
    	}
    }
    
    /*
     * Goes through the regions that have been defined and creates a bitmap with them obscured.
     * This may introduce memory issues and therefore have to be done in a different manner.
     */
    private Bitmap createObscuredBitmap(int width, int height) 
    {
    	// Create the bitmap that we'll output from this method
    	Bitmap obscuredBmp = Bitmap.createBitmap(width, height,imageBitmap.getConfig());
    	
    	// Create the canvas to draw on
    	Canvas obscuredCanvas = new Canvas(obscuredBmp); 
    	// Create the paint used to draw with
    	Paint obscuredPaint = new Paint();   
    	// Create a default matrix
    	Matrix obscuredMatrix = new Matrix();
    	
    	// Draw the scaled image on the new bitmap
    	obscuredCanvas.drawBitmap(imageBitmap, obscuredMatrix, obscuredPaint);

    	// Iterate through the regions that have been created
    	Iterator<ImageRegion> i = imageRegions.iterator();
	    while (i.hasNext()) 
	    {
	    	ImageRegion currentRegion = i.next();
	    	
	    	// Would like this to not be dependent on knowing the relationship between 
	    	// the classes and the constants in ImageRegion.  Would like there to be a 
	    	// method within ImageRegion that creates the ObscureMethod object and passes
	    	// it back.  Right now though, all of the ObscureMethods take in different
	    	// arguments which makes it painful.
	    	// Select the ObscureMethod as contained in the ImageRegion
	    	ObscureMethod om;
			switch (currentRegion.obscureType) {
				case ImageRegion.BLUR:
					Log.v(LOGTAG,"obscureType: BLUR");
					om = new CrowdBlurObscure(obscuredBmp);
				break;
				
				case ImageRegion.ANON:
					Log.v(LOGTAG,"obscureType: ANON");
					om = new AnonObscure(this.getApplicationContext(), obscuredBmp, obscuredPaint);
					break;
					
				case ImageRegion.SOLID:
					Log.v(LOGTAG,"obscureType: SOLID");
					om = new PaintSquareObscure();
					break;
					
				case ImageRegion.PIXELIZE:
					Log.v(LOGTAG,"obscureType: PIXELIZE");
					om = new PixelizeObscure(obscuredBmp);
					break;
					
				default:
					Log.v(LOGTAG,"obscureType: NONE/BLUR");
					om = new BlurObscure(obscuredBmp);
					break;
			}
			
			// Get the Rect for the region and do the obscure
            Rect rect = currentRegion.getAScaledRect(obscuredBmp.getWidth(),obscuredBmp.getHeight());
            Log.v(LOGTAG,"unscaled rect: left:" + rect.left + " right:" + rect.right 
            		+ " top:" + rect.top + " bottom:" + rect.bottom);
            			
	    	om.obscureRect(rect, obscuredCanvas);
		}

	    return obscuredBmp;
    }
    
    /*
     * Save a temporary image for sharing only
     */
    private boolean saveTmpImage() {
    	
    	String storageState = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(storageState)) {
        	Toast t = Toast.makeText(this,"External storage not available", Toast.LENGTH_SHORT); 
    		t.show();
    		return false;
    	}
    	
    	//Why does this not show?
    	ProgressDialog progressDialog = ProgressDialog.show(this, "", "Exporting for share...", true, true);    	
    	
    	// Create the bitmap that will be saved
    	// Perhaps this should be smaller than screen size??
    	int w = imageBitmap.getWidth();
    	int h = imageBitmap.getHeight();
    	/* This isn't necessary
    	if (imageBitmap.getWidth() > SHARE_SIZE_MAX_WIDTH_HEIGHT || imageBitmap.getHeight() > SHARE_SIZE_MAX_WIDTH_HEIGHT) {
    		// Size it down proportionally
    		float ratio = 1;
    		if (imageBitmap.getWidth() > imageBitmap.getHeight()) {
    			ratio = (float)SHARE_SIZE_MAX_WIDTH_HEIGHT/(float)imageBitmap.getWidth();
    		} else {
    			ratio = (float)SHARE_SIZE_MAX_WIDTH_HEIGHT/(float)imageBitmap.getHeight();
    		}

			w = (int) (ratio * imageBitmap.getWidth());
			h = (int) (ratio * imageBitmap.getHeight());
    	}
    	*/
    	Bitmap obscuredBmp = createObscuredBitmap(w,h);
    	
    	// Create the Uri - This can't be "private"
    	File tmpFileDirectory = new File(Environment.getExternalStorageDirectory().getPath() + TMP_FILE_DIRECTORY);
    	File tmpFile = new File(tmpFileDirectory,TMP_FILE_NAME);
    	Log.v(LOGTAG, tmpFile.getPath());
    	
		try {
	    	if (!tmpFileDirectory.exists()) {
	    		tmpFileDirectory.mkdirs();
	    	}
	    	tmpImageUri = Uri.fromFile(tmpFile);
	    	
			OutputStream imageFileOS;

			int quality = 75;
			imageFileOS = getContentResolver().openOutputStream(tmpImageUri);
			obscuredBmp.compress(CompressFormat.JPEG, quality, imageFileOS);

			progressDialog.cancel();
			return true;
		} catch (FileNotFoundException e) {
			progressDialog.cancel();
			e.printStackTrace();
			return false;
		}
    }
    
    /*
     * The method that actually saves the altered image.  
     * This in combination with createObscuredBitmap could/should be done in another, more memory efficient manner. 
     */
    private void saveImage() 
    {
    	//Why does this not show?
    	ProgressDialog progressDialog = ProgressDialog.show(this, "", "Saving...", true, true);

    	// Create the bitmap that will be saved
    	// Screen size
    	Bitmap obscuredBmp = createObscuredBitmap(imageBitmap.getWidth(),imageBitmap.getHeight());
    	
    	ContentValues cv = new ContentValues();
    	/* 
    	// Add a date so it shows up in a reasonable place in the gallery - Should we do this??
    	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
    	Date date = new Date();

		// Which one?
    	cv.put(Media.DATE_ADDED, dateFormat.format(date));
    	cv.put(Media.DATE_TAKEN, dateFormat.format(date));
    	cv.put(Media.DATE_MODIFIED, dateFormat.format(date));
    	*/
    	
    	// Uri is savedImageUri which is global
    	// Create the Uri, this should put it in the gallery
    	// New Each time
    	savedImageUri = getContentResolver().insert(
				Media.EXTERNAL_CONTENT_URI, cv);
    	    	
		OutputStream imageFileOS;
		try {
			int quality = 100; //lossless?  good question - still a smaller version
			imageFileOS = getContentResolver().openOutputStream(savedImageUri);
			obscuredBmp.compress(CompressFormat.JPEG, quality, imageFileOS);

    		Toast t = Toast.makeText(this,"Saved JPEG!", Toast.LENGTH_SHORT); 
    		t.show();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		progressDialog.cancel();
    }
    
    // Queries the contentResolver to pull out the path for the actual file.
    /*  This code is currently unused but i often find myself needing it so I 
     * am placing it here for safe keeping ;-) */
    public String pullPathFromUri(Uri originalUri) {
    	String originalImageFilePath = null;
    	String[] columnsToSelect = { MediaStore.Images.Media.DATA };
    	Cursor imageCursor = getContentResolver().query( originalImageUri, columnsToSelect, null, null, null );
    	if ( imageCursor != null && imageCursor.getCount() == 1 ) {
	        imageCursor.moveToFirst();
	        originalImageFilePath = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
    	}

    	return originalImageFilePath;
    }

    /*
     * Handling screen configuration changes ourselves, we don't want the activity to restart on rotation
     */
    @Override
    public void onConfigurationChanged(Configuration conf) 
    {
        super.onConfigurationChanged(conf);
    }

	@Override
	protected void onPostResume() {
		// TODO Auto-generated method stub
		super.onPostResume();
		
		
	}
}
