package org.witness.sscphase1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import org.witness.sscphase1.secure.Apg;
import org.witness.sscphase1.secure.EncryptObscureMethod;
import org.witness.sscphase1.secure.EncryptTagger;
import org.witness.sscphase1.secure.MediaHasher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.SQLException;
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
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class ImageEditor extends Activity implements OnTouchListener, OnClickListener {

	final static String LOGTAG = "[Camera Obscura : ImageEditor] **************************** ";
	final static String SSC = LOGTAG;

	// Colors for region squares
	public final static int DRAW_COLOR = Color.argb(128, 0, 255, 0);// Green
	public final static int DETECTED_COLOR = Color.argb(128, 0, 0, 255); // Blue
	public final static int OBSCURED_COLOR = Color.argb(128, 255, 0, 0); // Red
	public final static int TAGGED_COLOR = Color.argb(128, 128, 128, 0); // Something else
	
	public final static int PREFERENCES_MENU_ITEM = 0;
	public final static int PANIC_MENU_ITEM = 1;
	public final static int SAVE_MENU_ITEM = 2;
	public final static int SHARE_MENU_ITEM = 3;
	public final static int NEW_TAG_MENU_ITEM = 4;
	public final static int HASH_MENU_ITEM = 5;
	
	// Image Matrix
	Matrix matrix = new Matrix();
	// Saved Matrix for not allowing a current operation (over max zoom)
	Matrix savedMatrix = new Matrix();
		
	// We can be in one of these 3 states
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	static final int DRAW = 3;
	static final int TAP = 4;
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
	
	//float minMoveDistance = 10f; // 10 pixels??  Perhaps should be density independent
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
	
	Vector<ImageRegion> imageRegions = new Vector();  // Being lazy 
	//ImageRegion[] imageRegions;
		
	int originalImageWidth;
	int originalImageHeight;
		
	/* For database handling of metadata
	 * imageUriSource added because if user takes a photo
	 * with the camera, a file URI is returned
	 * but if user chooses a gallery image, URI has to be looked up.
	 */
	Bundle imageSource;
	Uri imageUri;
	SSCMetadataHandler mdh;
	
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

		// I made this URI global, as we should require it in other methods (HNH 2/22/11)
		imageUri = getIntent().getData();
		imageSource = getIntent().getExtras();
		
		vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		if (imageUri != null) {
			
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
				imageView.setImageBitmap(imageBitmap);
				imageView.setImageMatrix(matrix);
				overlayImageView.setImageMatrix(matrix);
								
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			// Call SSCMetadataHandler to make a new entry into the database
			mdh = new SSCMetadataHandler(this);
			try {
				mdh.createDatabase();
			} catch(IOException e) {}
			try {
				mdh.openDataBase();
			} catch(SQLException e) {}
			
			try {
				mdh.initiateMedia(imageUri,1,imageSource.getInt("imageSource"));
			} catch (IOException e) {}
			
			// Canvas for drawing
			overlayBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Config.ARGB_8888);
			overlayCanvas = new Canvas(overlayBitmap);
			overlayPaint = new Paint();
			overlayImageView.setImageBitmap(overlayBitmap); 
			//redrawOverlay();
				
			overlayImageView.setOnTouchListener(this);
			//overlayImageView.setOnLongClickListener(this); // What about normal onClick??\
			// Long click doesn't give place.. :-(
			
			// Layout for Image Regions
			regionButtonsLayout = (RelativeLayout) this.findViewById(R.id.RegionButtonsLayout);
			
			// Only want to do this if we are starting with a new image
			// Do popup
			showDialog(DIALOG_DO_AUTODETECTION);
			

		}
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
	

	/*
	 * This handles the callbacks from the EncryptObscure hand off to APG, or any other
	 * Result-based Intent launch
	 * 
	 * Also now included: callbacks from IdTagger and EncryptTagger classes.
	 */	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode ==  Apg.ENCRYPT_MESSAGE
				|| requestCode == Apg.SELECT_PUBLIC_KEYS
					|| requestCode == Apg.SELECT_SECRET_KEY)
		{
			Apg apg = Apg.getInstance();
			apg.onActivityResult(this, requestCode, resultCode, data);
			
			String encryptedData = apg.getEncryptedData();
			
		} else if(requestCode == RESULT_ID_TAGGER) {
			if(resultCode == Activity.RESULT_OK) {
				String ir = data.getStringExtra("imageRegion");
				// loop through image regions to find the matching image region, and set its subject
				// is this sloppy?  i don't know!!!  other suggestions welcome :)
				for(ImageRegion m : imageRegions) {
					if(ir.compareTo(m.toString()) == 0) {
						m.addSubjectId(data.getStringExtra("addedSubject"), Integer.parseInt(data.getStringExtra("subjectConsent")));
					}
				}
				mdh.registerSubject(data.getStringExtra("addedSubject"),
						Integer.parseInt(data.getStringExtra("subjectConsent")),
						null, mdh.getImageRegionResource(),ir);
			}
		} else if(requestCode == RESULT_ENCRYPT_TAGGER) {
			if(resultCode == Activity.RESULT_OK) {
				String ir = data.getStringExtra("imageRegion");
				for(ImageRegion m : imageRegions) {
					if(ir.compareTo(m.toString()) == 0) {
						m.addEncryptedKey(data.getStringArrayListExtra("addedKeys"));
					}
				}
				mdh.registerKeys(data.getStringArrayListExtra("addedKeys"),mdh.getImageRegionResource(),ir);
			}
		}
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
	
	// This should really have a class for the regions to obscure, each with it's own information
	/*
	private void createObscuredBmp(Rect[] obscureRects) {
		// Canvas for drawing
		Bitmap alteredBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), imageBitmap.getConfig());
		Canvas canvas = new Canvas(alteredBitmap);
		Paint paint = new Paint();
		canvas.drawBitmap(imageBitmap, 0, 0, paint);
		
		int OBSCURE_SQUARE = 0;
		int OBSCURE_BLUR = 1;
		
		int obscureMethod = OBSCURE_SQUARE;
		
        // Using the interface
        ObscureMethod om;
        
        // Load the appropriate class/method based on obscureMethod variable/constants
        if (obscureMethod == OBSCURE_BLUR) {
        	om = new BlurObscure(alteredBitmap);
        } else {
        	om = new PaintSquareObscure();		            	
        }
        
		for (int i = 0; i < obscureRects.length; i++) {				            	
			// Apply the obscure method
			om.obscureRect(obscureRects[i], canvas);
		}
	}*/

	
	private void sendSecureHashSMS ()
	{
		Date hashTime = new Date();
		String hash = generateSecureHash ();
		
		Intent sendIntent = new Intent(Intent.ACTION_VIEW);         
		sendIntent.setData(Uri.parse("sms:"));
		sendIntent.putExtra("sms_body", "Obscura hash: " + hash + " (" + hashTime.toGMTString() + ")"); 
		startActivity(sendIntent);
		 
	}
	
	private String generateSecureHash()
	{
		String result = "";
	//	ProgressDialog progressDialog =ProgressDialog.show(this, "", "generating hash...");
		
		
		try
		{
			InputStream is = getContentResolver().openInputStream(imageUri);		
			result = MediaHasher.hash(is, "SHA-1");
			mdh.mediaHash(result);
		}
		catch (Exception e)
		{
			Log.e(LOGTAG, "error generating hash",e);
		}
		finally
		{
		//	progressDialog.dismiss();
		}
		
		
		
		return result;
	}
	
	private Rect[] runFaceDetection() {
		GoogleFaceDetection gfd = new GoogleFaceDetection(imageBitmap);
		int numFaces = gfd.findFaces();
        Log.v(LOGTAG,"Num Faces Found: " + numFaces); 
        Rect[] possibleFaceRects = gfd.getFaces();
		return possibleFaceRects;				
	}
	
	
	public boolean onTouch(View v, MotionEvent event) {
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
				
				if (mode == TAP) {
					vibe.vibrate(50);
					createImageRegion((int)startPoint.x-DEFAULT_REGION_WIDTH/2, (int)startPoint.y-DEFAULT_REGION_HEIGHT/2, (int)startPoint.x+DEFAULT_REGION_WIDTH/2, (int)startPoint.y+DEFAULT_REGION_HEIGHT/2, overlayCanvas.getWidth(), overlayCanvas.getHeight(), originalImageWidth, originalImageHeight, DRAW_COLOR);
				}
				
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
				
					if (mode == TAP) {
						mode = DRAG;
						
						matrix.postTranslate(event.getX() - startPoint.x, event.getY() - startPoint.y);
						imageView.setImageMatrix(matrix);
						overlayImageView.setImageMatrix(matrix);
						// Reset the start point
						startPoint.set(event.getX(), event.getY());
	
						putOnScreen();
						redrawRegions();
	
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
						}
					}
				}
				break;
		}

		return true; // indicate event was handled
	}
	
	
	
	/*
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				// Single Finger
				
				// Stop current timer if running
				if (touchTimerRunnable != null) {
					touchTimerHandler.removeCallbacks(touchTimerRunnable);
				}
				
				// Save the Start point. 
				startPoint.set(event.getX(), event.getY());
				
				// Start out as a drag unless they keep their finger down
				mode = DRAG;
				Log.d(LOGTAG, "mode=DRAG");
				
				// Start timer to determine if this should be drawing mode
				touchTimerRunnable = new Runnable() {
					public void run() {
						mode = DRAW;
						// Tell the user somehow
						Log.v(LOGTAG,"DRAWING DRAWING DRAWING");
					}
				};
				touchTimerHandler = new Handler();
				touchTimerHandler.postDelayed(touchTimerRunnable,1000); // Hold down for one second
				
				break;
				
			case MotionEvent.ACTION_POINTER_DOWN:
				// Two Fingers
				
				// Stop current timer if running
				if (touchTimerRunnable != null) {
					touchTimerHandler.removeCallbacks(touchTimerRunnable);
				}
				
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
				
				if (touchTimerRunnable != null) {
					touchTimerHandler.removeCallbacks(touchTimerRunnable);
				}
				
				if (mode == DRAW) {
					// Create Region
					vibe.vibrate(50);
					
					createImageRegion((int)startPoint.x, (int)startPoint.y, (int)event.getX(), (int)event.getY(), overlayCanvas.getWidth(), overlayCanvas.getHeight(), originalImageWidth, originalImageHeight, DRAW_COLOR);
				}

				mode = NONE;
				Log.v(LOGTAG,"mode=NONE");
				break;
				
			case MotionEvent.ACTION_POINTER_UP:
				// Multiple Finger Up
				
				if (touchTimerRunnable != null) {
					touchTimerHandler.removeCallbacks(touchTimerRunnable);
				}
								
				mode = NONE;
				Log.d(LOGTAG, "mode=NONE");
				break;
				
			case MotionEvent.ACTION_MOVE:
				
				float distance = (float) (Math.sqrt(Math.abs(startPoint.x - event.getX()) + Math.abs(startPoint.y - event.getY())));
				Log.v(LOGTAG,"Move Distance: " + distance);
				Log.v(LOGTAG,"Min Distance: " + minMoveDistance);
				
				if (distance > minMoveDistance) {
				
					if (touchTimerRunnable != null) {
						touchTimerHandler.removeCallbacks(touchTimerRunnable);
					}
					
					if (mode == DRAG) {
					
						matrix.postTranslate(event.getX() - startPoint.x, event.getY() - startPoint.y);
						imageView.setImageMatrix(matrix);
						overlayImageView.setImageMatrix(matrix);
						// Reset the start point
						startPoint.set(event.getX(), event.getY());
	
						putOnScreen();
						redrawRegions();
	
					} else if (mode == DRAW) { 
						
						clearOverlay();
						overlayPaint.setColor(DRAW_COLOR);
						overlayPaint.setStyle(Paint.Style.STROKE);
						overlayCanvas.drawRect(startPoint.x, startPoint.y, event.getX(), event.getY(), overlayPaint);
						overlayImageView.invalidate();
	
					} else if (mode == ZOOM) {
						
						// Save the current matrix so that if zoom goes to big, we can revert
						savedMatrix.set(matrix);
						
						// Get the spacing of the fingers, 2 fingers
						float ex = event.getX(0) - event.getX(1);
						float ey = event.getY(0) - event.getY(1);
						endFingerSpacing = (float) Math.sqrt(ex * ex + ey * ey);
	
						Log.d(LOGTAG, "End Finger Spacing=" + endFingerSpacing);
		
						if (endFingerSpacing > 10f) {
							
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
						}
					}
				}
				break;
		}

		return true; // indicate event was handled
	}*/

	
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
	
	public void createImageRegion(int _scaledStartX, int _scaledStartY, 
			int _scaledEndX, int _scaledEndY, 
			int _scaledImageWidth, int _scaledImageHeight, 
			int _imageWidth, int _imageHeight, 
			int _backgroundColor) {
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
		mdh.registerImageRegion(imageRegion);
		clearOverlay();
		

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
    	
    	// TODO: this is throwing an error when tags are re-drawn - update database
    	// mdh.registerTag((String) imageRegion.getContentDescription());
    	
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
		// Easier ??
		regionButtonsLayout.removeAllViews();
		drawRegions();
		
		// Put the buttons in the right place
		/* Not working
		Iterator<ImageRegion> i = imageRegions.iterator();
	    while (i.hasNext()) {
	    	ImageRegion currentRegion = i.next();
	    	Rect scaledRect = currentRegion.getScaledRect(overlayCanvas.getWidth(), overlayCanvas.getHeight());
	    	RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) currentRegion.getLayoutParams();
	    	params.leftMargin = scaledRect.left;
	    	params.topMargin = scaledRect.top;
	    	params.height = scaledRect.height();
	    	params.width = scaledRect.width();
	    	currentRegion.setLayoutParams(params);
	    	currentRegion.invalidate();
	    }		
	    */
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
	    	/*
			SSCEditTag et = new SSCEditTag(v.getContentDescription(), regionButtonsLayout);
	    	buttonIDs = et.getButtonIDs();
	    	OnClickListener ocl = new OnClickListener() {
	    		// each button (except the image prefs button, which is gloabl)
	    		// is linked with the image tag --
	    		// call v.getContentDescription
				public void onClick(View v) {
					if(v.getId() == buttonIDs[0]) {
						// Edit Tag
						Log.v(LOGTAG,"Edit Tag clicked for tag# " + v.getContentDescription());
					} else if(v.getId() == buttonIDs[1]) {
						// ID Tag
						launchIdTagger((String) v.getContentDescription());
						Log.v(LOGTAG,"ID Tag clicked for tag# " + v.getContentDescription());
					} else if(v.getId() == buttonIDs[2]) {
						// Blur Tag
						Log.v(LOGTAG,"Blur Tag clicked for tag# " + v.getContentDescription());
					} else if(v.getId() == buttonIDs[3]) {
						// Encrypt/Decrypt image region
						Log.v(LOGTAG,"Image Prefs clicked");
						launchEncryptTagger((String) v.getContentDescription());
					}
				}
	    	};
	    	et.addActions(ocl);
	    	et.show();
	    	*/
		}
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
    		case R.id.menu_panic:
    		// Look up preferences and do what is required
    		
    			return true;
    		case R.id.menu_image_prefs:
        	 	// Load Image Preferences Activity
        		launchImagePrefs();
        		return true;

        	case R.id.menu_save:
        		// Save Image
        		saveImage();
        		
        		return true;
        	case R.id.menu_share:
        		// Share Image
        		shareImage();
        		
        		return true;
        	case R.id.menu_send_hash:
        		// Send hash of image via SMS
        		sendSecureHashSMS();
        		
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
            	om = new BlurObscure(obscuredBmp);
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
	    	om.obscureRect(currentRegion.getScaledRect(imageBitmap.getWidth(), imageBitmap.getHeight()), obscuredCanvas);

	    }
	    
    	return obscuredBmp;
    }
    
    private void saveImage() {
    	
    	Bitmap obscuredBmp = createObscuredBitmap();
    	
    	// Uri is savedImageUri which is global
    	if (savedImageUri == null) {
    		// Create the Uri
    		File newFile = createSecureFile();
    		if (newFile != null) {
    			savedImageUri = Uri.fromFile(newFile);
    		} else {
    			imageSaved = false;    			
    			return;
    		}
    	}	
    	
		OutputStream imageFileOS;
		try {
			int quality = 100; //lossless?  good question - still a smaller version
			imageFileOS = getContentResolver().openOutputStream(savedImageUri);
			obscuredBmp.compress(CompressFormat.JPEG, quality, imageFileOS);
			
			// Trying out an EXIF write method
			Date hashTime = new Date();
			String hash = generateSecureHash();
			
			try {
				ExifInterface ei = new ExifInterface(savedImageUri.getPath());
				// Maybe we can put everything in the TAG_MODEL or TAG_MAKE??
				ei.setAttribute(ExifInterface.TAG_MODEL,hash + "(" + hashTime.toGMTString() + ")");
				// Add in other attributes
				ei.saveAttributes();
	    		Toast t = Toast.makeText(this,"EXIF Data Saved", Toast.LENGTH_SHORT); 
	    		t.show();
			} catch (IOException ioe) {
				ioe.printStackTrace();
	    		Toast t = Toast.makeText(this,"EXIF Data Failed", Toast.LENGTH_SHORT); 
	    		t.show();

			}
			
    		Toast t = Toast.makeText(this,"Saved JPEG!", Toast.LENGTH_SHORT); 
    		t.show();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// Do the saving
    	imageSaved = true;
    	
    	// Encrypt Regions
    	Iterator<ImageRegion> i = imageRegions.iterator();
	    while (i.hasNext()) {
	    	ImageRegion currentRegion = i.next();
	    	if (currentRegion.whattodo == ImageRegion.ENCRYPT) {
	    		// NATHAN
	    	}
	    }
    }
    
    private File createSecureFile() {
    	
    	File newFile = null;
    	// This could be a secure directory
    	File directory = new File("/sdcard/");
    	try {
			newFile = File.createTempFile("ssc", ".jpg", directory);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
    	return newFile;
    }
    
    // TODO: let's handle the menu activities here...
    public void launchImagePrefs() {
    	// save state here?
		Intent intent = new Intent(this, PreferencesActivity.class);
		startActivity(intent);
    }
    
   
    private int RESULT_ENCRYPT_TAGGER = 9999;
    private int RESULT_ID_TAGGER = 9998;
    
    public void launchEncryptTagger(String ir) {
    	Intent intent = new Intent(this, EncryptTagger.class);
    	intent.putExtra("imageRegion", ir);
    	startActivityForResult(intent,RESULT_ENCRYPT_TAGGER);
    }
    
    public void launchIdTagger(String ir) {
    	Intent intent = new Intent(this, IdTagger.class);
    	intent.putExtra("imageRegion", ir);
    	startActivityForResult(intent,RESULT_ID_TAGGER);
    }
    
    @Override
    public void onConfigurationChanged(Configuration conf) {
        super.onConfigurationChanged(conf);
    }
    
    /*
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	Log.v(LOGTAG,"onRestoreInstanceState");
        if (savedInstanceState.containsKey("imageRegions")) {
        	imageRegions = (Vector) savedInstanceState.getSerializable("imageRegions");
        	redrawRegions();
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	Log.v(LOGTAG,"onSaveInstanceState");
    	savedInstanceState.putSerializable("imageRegions", imageRegions);
    	super.onSaveInstanceState(savedInstanceState);
    }*/
    
}
