package org.witness.sscphase1;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Vector;

import org.witness.sscphase1.secure.EncryptObscureMethod;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class ImageEditor extends Activity implements OnTouchListener, OnClickListener, OnLongClickListener {

	final static String LOGTAG = "[Camera Obscura : ImageEditor]";

	// Colors for region squares
	public final static int DRAW_COLOR = Color.argb(128, 0, 255, 0);// Green
	public final static int DETECTED_COLOR = Color.argb(128, 0, 0, 255); // Blue
	public final static int OBSCURED_COLOR = Color.argb(128, 255, 0, 0); // Red

	public final static int ABOUT_MENU_ITEM = 0;
	public final static int DELETE_ORIGINAL_MENU_ITEM = 1;
	public final static int SAVE_MENU_ITEM = 2;
	public final static int SHARE_MENU_ITEM = 3;
	
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

	static final int DEFAULT_REGION_WIDTH = 160;
	static final int DEFAULT_REGION_HEIGHT = 160;
	
	static final float MAX_SCALE = 10f;
	
	static final int DIALOG_DO_AUTODETECTION = 0;
	
	// For Zooming
	float startFingerSpacing = 0f;
	float endFingerSpacing = 0f;
	PointF startFingerSpacingMidPoint = new PointF();
	
	// For Dragging
	PointF startPoint = new PointF();
	
	float minMoveDistanceDP = 5f;
	float minMoveDistance; // = ViewConfiguration.get(this).getScaledTouchSlop();
	float scale; // Current display scale
	
	Button zoomIn, zoomOut;
	ImageView imageView;
	ImageView overlayImageView;
	
	RelativeLayout regionButtonsLayout;
	FrameLayout frameRoot;
	
	// Touch Timer Related (Long Clicks)
	Handler touchTimerHandler;
	Runnable touchTimerRunnable;
	
	Bitmap imageBitmap;
	Bitmap overlayBitmap;
	
	Canvas overlayCanvas;
	Paint overlayPaint;
	
	Vector<ImageRegion> imageRegions = new Vector<ImageRegion>(); 
		
	int originalImageWidth;
	int originalImageHeight;
		
	Uri imageUri;
	
	Vibrator vibe;
	
	// This will need to be updated when changes are made to the image
	boolean imageSaved = false;
	Uri savedImageUri = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
        requestWindowFeature(Window.FEATURE_NO_TITLE);  
		setContentView(R.layout.imageviewer);
		
		scale = this.getResources().getDisplayMetrics().density;
		minMoveDistance = minMoveDistanceDP * scale + 0.5f;
		
		imageView = (ImageView) findViewById(R.id.ImageEditorImageView);
		overlayImageView = (ImageView) findViewById(R.id.ImageEditorOverlayImageView);
		frameRoot = (FrameLayout) findViewById(R.id.frameRoot);

		zoomIn = (Button) this.findViewById(R.id.ZoomIn);
		zoomOut = (Button) this.findViewById(R.id.ZoomOut);

		zoomIn.setOnClickListener(this);
		zoomOut.setOnClickListener(this);

		// Passed in from CameraObscuraMainMenu
		imageUri = getIntent().getData();
		
		// Coming from another app via "share"
		if (imageUri == null && getIntent().hasExtra(Intent.EXTRA_STREAM)) {
			imageUri = (Uri) getIntent().getExtras().get(Intent.EXTRA_STREAM);
		}

		vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		if (imageUri != null) {
			// Load up smaller image
			try {
				// Load up the image's dimensions not the image itself
				BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
				bmpFactoryOptions.inJustDecodeBounds = true;

				// Needs to be this config for Google Face Detection 
				bmpFactoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
				
				imageBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri), null, bmpFactoryOptions);

				originalImageWidth = bmpFactoryOptions.outWidth;
				originalImageHeight = bmpFactoryOptions.outHeight;
				
				Display currentDisplay = getWindowManager().getDefaultDisplay();
				
				int widthRatio = (int) Math.ceil(bmpFactoryOptions.outWidth / (float) currentDisplay.getWidth());
				int heightRatio = (int) Math.ceil(bmpFactoryOptions.outHeight / (float) currentDisplay.getHeight());
	
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
				imageBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri), null, bmpFactoryOptions);
				Log.v(LOGTAG,"Was: " + imageBitmap.getConfig());

				if (imageBitmap == null) {
					Log.v(LOGTAG,"bmp is null");
				}
				
				float matrixScale = 1;
				matrix.setScale(matrixScale, matrixScale);
				imageView.setImageBitmap(createObscuredBitmap());
				imageView.setImageMatrix(matrix);
				overlayImageView.setImageMatrix(matrix);
								
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// Canvas for drawing
			overlayBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Config.ARGB_8888);
			overlayCanvas = new Canvas(overlayBitmap);
			overlayPaint = new Paint();
			overlayImageView.setImageBitmap(overlayBitmap); 
			//redrawOverlay();
				
			overlayImageView.setOnTouchListener(this);
			overlayImageView.setOnLongClickListener(this); // What about normal onClick??\
			// Long click doesn't give place.. :-(
			
			// Layout for Image Regions
			regionButtonsLayout = (RelativeLayout) this.findViewById(R.id.RegionButtonsLayout);
			
			// Do auto detect popup
			showDialog(DIALOG_DO_AUTODETECTION);
			
		}
	}
	
	private void handleDelete () 
	{
		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setIcon(android.R.drawable.ic_dialog_alert);
		b.setTitle(getString(R.string.app_name));
		b.setMessage(getString(R.string.confirm_delete));
		b.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                /* User clicked OK so do some stuff */
        		getContentResolver().delete(imageUri, null, null);
        		imageUri = null;
            }
        });
		b.setNegativeButton(android.R.string.no, null);
		b.show();
	}
		
	protected Dialog onCreateDialog(int id) {
		Log.v(LOGTAG,"Within onCreateDialog" + id);
	    Dialog dialog = null;
	    switch(id) {
	    	case DIALOG_DO_AUTODETECTION:
	    		Log.v(LOGTAG,"Within onCreateDialog right case");
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
	    		dialog = builder.create();	    		
	    		break;
	        
	    	default:
	    		dialog = null;
	    }
	    return dialog;
	}	
	
	private void doAutoDetection() {
		// This should be called via a pop-up/alert mechanism
		
		Rect[] autodetectedRects = runFaceDetection();
		for (int adr = 0; adr < autodetectedRects.length; adr++) {
			Log.v(LOGTAG,autodetectedRects[adr].toString());
			createImageRegion(
					autodetectedRects[adr].left,
					autodetectedRects[adr].top,
					autodetectedRects[adr].right,
					autodetectedRects[adr].bottom,
					overlayCanvas.getWidth(), 
					overlayCanvas.getHeight(), 
					originalImageWidth, 
					originalImageHeight, 
					DETECTED_COLOR);
		}
		
		Toast autodetectedToast = Toast.makeText(this, "" + autodetectedRects.length + " faces deteceted", Toast.LENGTH_SHORT);
		autodetectedToast.show();
		
		clearOverlay();			
	}
	
	private Rect[] runFaceDetection() {
		GoogleFaceDetection gfd = new GoogleFaceDetection(imageBitmap);
		int numFaces = gfd.findFaces();
        Log.v(LOGTAG,"Num Faces Found: " + numFaces); 
        Rect[] possibleFaceRects = gfd.getFaces();
		return possibleFaceRects;				
	}
	
	public boolean onTouch(View v, MotionEvent event) {
		
		boolean handled = false;
		
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				// Single Finger
				mode = TAP;
				
				// Save the Start point. 
				startPoint.set(event.getX(), event.getY());
												
				break;
				
			case MotionEvent.ACTION_POINTER_DOWN:
				// Two Fingers
				
				// Get the spacing of the fingers, 2 fingers
				float sx = event.getX(0) - event.getX(1);
				float sy = event.getY(0) - event.getY(1);
				startFingerSpacing = (float) Math.sqrt(sx * sx + sy * sy);

				Log.d(LOGTAG, "Start Finger Spacing=" + startFingerSpacing);
				
				if (startFingerSpacing > 10f) {

					float xsum = event.getX(0) + event.getX(1);
					float ysum = event.getY(0) + event.getY(1);
					startFingerSpacingMidPoint.set(xsum / 2, ysum / 2);
				
					mode = ZOOM;
					Log.d(LOGTAG, "mode=ZOOM");
				}
				
				break;
				
			case MotionEvent.ACTION_UP:
				// Single Finger Up
								
				mode = NONE;
				Log.v(LOGTAG,"mode=NONE");

				break;
				
			case MotionEvent.ACTION_POINTER_UP:
				// Multiple Finger Up
				
				mode = NONE;
				Log.d(LOGTAG, "mode=NONE");
				break;
				
			case MotionEvent.ACTION_MOVE:
				
				float distance = (float) (Math.sqrt(Math.abs(startPoint.x - event.getX()) + Math.abs(startPoint.y - event.getY())));
				Log.v(LOGTAG,"Move Distance: " + distance);
				Log.v(LOGTAG,"Min Distance: " + minMoveDistance);
				
				if (distance > minMoveDistance) {
				
					if (mode == TAP || mode == DRAG) {
						mode = DRAG;
						
						matrix.postTranslate(event.getX() - startPoint.x, event.getY() - startPoint.y);
						imageView.setImageMatrix(matrix);
						overlayImageView.setImageMatrix(matrix);
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
	
						Log.d(LOGTAG, "End Finger Spacing=" + endFingerSpacing);
		
						if (endFingerSpacing > minMoveDistance) {
							
							// Ratio of spacing..  If it was 5 and goes to 10 the image is 2x as big
							float scale = endFingerSpacing / startFingerSpacing;
							// Scale from the midpoint
							matrix.postScale(scale, scale, startFingerSpacingMidPoint.x, startFingerSpacingMidPoint.y);
		
							float[] matrixValues = new float[9];
							matrix.getValues(matrixValues);
							Log.v(LOGTAG, "Total Scale: " + matrixValues[0]);
							Log.v(LOGTAG, "" + matrixValues[0] + " " + matrixValues[1]
									+ " " + matrixValues[2] + " " + matrixValues[3]
									+ " " + matrixValues[4] + " " + matrixValues[5]
									+ " " + matrixValues[6] + " " + matrixValues[7]
									+ " " + matrixValues[8]);
							if (matrixValues[0] > MAX_SCALE) {
								matrix.set(savedMatrix);
							}
							imageView.setImageMatrix(matrix);
							overlayImageView.setImageMatrix(matrix);
							
							putOnScreen();
							redrawRegions();
	
							// Reset Start Finger Spacing
							float esx = event.getX(0) - event.getX(1);
							float esy = event.getY(0) - event.getY(1);
							startFingerSpacing = (float) Math.sqrt(esx * esx + esy * esy);
							Log.d(LOGTAG, "New Start Finger Spacing=" + startFingerSpacing);
							
							float xsum = event.getX(0) + event.getX(1);
							float ysum = event.getY(0) + event.getY(1);
							startFingerSpacingMidPoint.set(xsum / 2, ysum / 2);
							
							handled = true;
						}
					}
				}
				break;
		}


		return handled; // indicate event was handled
	}
	
	public void updateDisplayImage ()
	{

		imageView.setImageBitmap(createObscuredBitmap());

	}
	
	public void putOnScreen() {

		// Get Rectangle of Tranformed Image
		RectF theRect = new RectF(0,0,imageBitmap.getWidth(), imageBitmap.getHeight());
		matrix.mapRect(theRect);
		
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
		imageView.setImageMatrix(matrix);
		overlayImageView.setImageMatrix(matrix);
	}
	
	public void clearOverlay() {
				
		Paint clearPaint = new Paint();
		clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		overlayCanvas.drawRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight(), clearPaint);
		
		overlayImageView.invalidate();
		
	}
	
	private void clearImageRegionsEditMode ()
	{
		Iterator<ImageRegion> itRegions = imageRegions.iterator();
		
		while (itRegions.hasNext())
		{
			itRegions.next().changeMode(ImageRegion.NORMAL_MODE);
		}
	}
	
	public void createImageRegion(int _scaledStartX, int _scaledStartY, 
			int _scaledEndX, int _scaledEndY, 
			int _scaledImageWidth, int _scaledImageHeight, 
			int _imageWidth, int _imageHeight, 
			int _backgroundColor) {
		
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
		addImageRegionToLayout(imageRegion);
		clearOverlay();
	}
	
	public void deleteRegion(ImageRegion ir)
	{
		imageRegions.remove(ir);
		redrawRegions();
	}
	
	public RectF getScaleOfImage() {
		RectF theRect = new RectF(0,0,imageBitmap.getWidth(), imageBitmap.getHeight());
		matrix.mapRect(theRect);
		return theRect;
	}

	public void addImageRegionToLayout(ImageRegion imageRegion) {

		// Get Rectangle of Current Transformed Image
		RectF theRect = new RectF(0,0,imageBitmap.getWidth(), imageBitmap.getHeight());
		matrix.mapRect(theRect);
		Log.v(LOGTAG,"New Width:" + theRect.width());

		Rect regionScaledRect = imageRegion.getScaledRect((int)theRect.width(), (int)theRect.height());
		
    	RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(regionScaledRect.width(),regionScaledRect.height());
    	lp.leftMargin = (int)theRect.left + regionScaledRect.left;
    	lp.topMargin = (int)theRect.top + regionScaledRect.top;
    	imageRegion.setLayoutParams(lp);
    	
    	// should always have been removed
    	//if (regionButtonsLayout.)
    	regionButtonsLayout.addView(imageRegion,lp);
    }
	
	public void drawRegions() {
		Iterator<ImageRegion> i = imageRegions.iterator();
	    while (i.hasNext()) {
	    	ImageRegion currentRegion = i.next();
	    	addImageRegionToLayout(currentRegion);
	    }
	}
	
	public void redrawRegions() {
		regionButtonsLayout.removeAllViews();
		drawRegions();
	}
	
	public void onClick(View v) {
		if (v == zoomIn) 
		{
			float scale = 1.5f;
			
			PointF midpoint = new PointF(imageView.getWidth()/2, imageView.getHeight()/2);
			matrix.postScale(scale, scale, midpoint.x, midpoint.y);
			imageView.setImageMatrix(matrix);
			overlayImageView.setImageMatrix(matrix);
			putOnScreen();
			redrawRegions();
		} else if (v == zoomOut) {
			float scale = 0.75f;

			PointF midpoint = new PointF(imageView.getWidth()/2, imageView.getHeight()/2);
			matrix.postScale(scale, scale, midpoint.x, midpoint.y);
			imageView.setImageMatrix(matrix);
			overlayImageView.setImageMatrix(matrix);
			putOnScreen();
			redrawRegions();
		} else if (v instanceof ImageRegion) {
			// Menu goes here
		}
	}
	
	public boolean onLongClick (View v)
	{
		if (mode != DRAG && mode != ZOOM) {
			vibe.vibrate(50);
			createImageRegion((int)startPoint.x-DEFAULT_REGION_WIDTH/2, (int)startPoint.y-DEFAULT_REGION_HEIGHT/2, (int)startPoint.x+DEFAULT_REGION_WIDTH/2, (int)startPoint.y+DEFAULT_REGION_HEIGHT/2, overlayCanvas.getWidth(), overlayCanvas.getHeight(), originalImageWidth, originalImageHeight, DRAW_COLOR);
			return true;
		}
		
		return false;
	}
	
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {

    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.image_editor_menu, menu);
		/*
    	MenuItem newTagMenuItem = menu.add(Menu.NONE, NEW_TAG_MENU_ITEM, Menu.NONE, "New Tag");
		MenuItem panicMenuItem = menu.add(Menu.NONE, PANIC_MENU_ITEM, Menu.NONE, "Panic");
        MenuItem preferencesMenuItem = menu.add(Menu.NONE, PREFERENCES_MENU_ITEM, Menu.NONE, "Preferences");
        MenuItem saveMenuItem = menu.add(Menu.NONE, SAVE_MENU_ITEM, Menu.NONE, "Save");
        MenuItem shareMenuItem = menu.add(Menu.NONE, SHARE_MENU_ITEM, Menu.NONE, "Share");
        MenuItem hashMenuItem = menu.add(Menu.NONE, HASH_MENU_ITEM, Menu.NONE, "Send Hash");
        */
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
 
    		case R.id.menu_new_region:
    			// Set the Start point. 
				startPoint.set(overlayCanvas.getWidth()/2, overlayCanvas.getHeight()/2);
    			// Add new region at default location (center)
    			createImageRegion((int)startPoint.x-DEFAULT_REGION_WIDTH/2, (int)startPoint.y-DEFAULT_REGION_HEIGHT/2, (int)startPoint.x+DEFAULT_REGION_WIDTH/2, (int)startPoint.y+DEFAULT_REGION_HEIGHT/2, overlayCanvas.getWidth(), overlayCanvas.getHeight(), originalImageWidth, originalImageHeight, DRAW_COLOR);

    			return true;
        	case R.id.menu_save:
        		// Save Image
        		saveImage();
        		
        		return true;
        	case R.id.menu_share:
        		// Share Image
        		shareImage();
        		
        		return true;
    		default:
    			return false;
    	}
    }
    
    private void shareImage() {
    	    	
    	saveImage();
    	
    	Intent share = new Intent(Intent.ACTION_SEND);
    	share.setType("image/jpeg");
    	share.putExtra(Intent.EXTRA_STREAM, savedImageUri);
    	startActivity(Intent.createChooser(share, "Share Image"));    	
    }
    
    /*
     * This may introduce memory issues and therefore have to be done in a 
     * different activity
     */
    private Bitmap createObscuredBitmap() {
    	Bitmap obscuredBmp = Bitmap.createBitmap( imageBitmap.getWidth(),imageBitmap.getHeight(),imageBitmap.getConfig());
    	Canvas obscuredCanvas = new Canvas(obscuredBmp); 
    	
    	Paint obscuredPaint = new Paint();     	
    	Matrix obscuredMatrix = new Matrix();
    	obscuredCanvas.drawBitmap(imageBitmap, obscuredMatrix, obscuredPaint);

    	Iterator<ImageRegion> i = imageRegions.iterator();
	    while (i.hasNext()) {
	    	ImageRegion currentRegion = i.next();
	    	
	    	//REWORK THIS CODE
	    	// If the currentRegion is to be obscured:
            // Using the interface
            ObscureMethod om;
            int obscureMethod = 1;
            
            // Load the appropriate class/method based on obscureMethod variable/constants
            if (obscureMethod == 1) {
            	//om = new BlurObscure(obscuredBmp);
            	om = new PixelizeObscure(obscuredBmp);
            	//om = new AnonObscure(this, obscuredBmp, obscuredPaint);
            }
            else if (obscureMethod == 2)
            {
            	long signKey = 1;
    	    	long encryptKeys[] = {1};
    	    	om = new EncryptObscureMethod(this, obscuredBmp, signKey, encryptKeys);    	    		
            }
            else {
            	om = new PaintSquareObscure();		            	
            }
            
	    	
	    	// WORKS
	    	//obscuredCanvas.drawRect(currentRegion.getScaledRect(imageBitmap.getWidth(), imageBitmap.getHeight()), obscuredPaint);
	    	
	    	// This should be determined by the currentRegion.whatever  
	     		
	    		//new PaintSquareObscure();
             Rect rect = currentRegion.getScaledRect(imageBitmap.getWidth(), imageBitmap.getHeight());
	    	om.obscureRect(rect, obscuredCanvas);

	    }
	    
    	return obscuredBmp;
    }
    
    private void saveImage() {
    	
    	Bitmap obscuredBmp = createObscuredBitmap();
    	
    	// Uri is savedImageUri which is global
    	if (savedImageUri == null) {
    		// Create the Uri, this should put it in the gallery
    		savedImageUri = getContentResolver().insert(
					Media.EXTERNAL_CONTENT_URI, new ContentValues());
    	}	
    	
		OutputStream imageFileOS;
		try {
			int quality = 100; //lossless?  good question - still a smaller version
			imageFileOS = getContentResolver().openOutputStream(savedImageUri);
			obscuredBmp.compress(CompressFormat.JPEG, quality, imageFileOS);

			imageSaved = true;

    		Toast t = Toast.makeText(this,"Saved JPEG!", Toast.LENGTH_SHORT); 
    		t.show();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

    }
    
    @Override
    public void onConfigurationChanged(Configuration conf) {
        super.onConfigurationChanged(conf);
    }
}
