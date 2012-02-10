package org.witness.securesmartcam;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.witness.informa.KeyChooser;
import org.witness.informa.Tagger;
import org.witness.informa.utils.ImageConstructor;
import org.witness.informa.utils.InformaConstants;
import org.witness.securesmartcam.detect.GoogleFaceDetection;
import org.witness.securesmartcam.filters.InformaTagger;
import org.witness.securesmartcam.filters.RegionProcesser;
import org.witness.securesmartcam.jpegredaction.JpegRedaction;
import org.witness.securesmartcam.utils.ObscuraConstants;
import org.witness.sscphase1.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class ImageEditor extends Activity implements OnTouchListener, OnClickListener {
	// Image Matrix
	Matrix matrix = new Matrix();

	// Saved Matrix for not allowing a current operation (over max zoom)
	Matrix savedMatrix = new Matrix();
		
	// We can be in one of these 3 states
	int mode = ObscuraConstants.NONE;
	
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
	Button zoomIn, zoomOut, btnSave, btnShare, btnPreview, btnNew;
	
	// ImageView for the original (scaled) image
	ImageView imageView;
	
		
	// Bitmap for the original image (scaled)
	Bitmap imageBitmap;
	
	// Bitmap for holding the realtime obscured image
    Bitmap obscuredBmp;
    
    // Canvas for drawing the realtime obscuring
    Canvas obscuredCanvas;
	
    // Paint obscured
    Paint obscuredPaint;
    
    //bitmaps for corners
    private final static float CORNER_SIZE = 26;
    Bitmap bitmapCornerUL;
    Bitmap bitmapCornerUR;
    Bitmap bitmapCornerLL;
    Bitmap bitmapCornerLR;
    
    
    
	// Vector to hold ImageRegions 
	ArrayList<ImageRegion> imageRegions = new ArrayList<ImageRegion>(); 
		
	// The original image dimensions (not scaled)
	int originalImageWidth;
	int originalImageHeight;

	// So we can give some haptic feedback to the user
	Vibrator vibe;

	// Original Image Uri
	Uri originalImageUri;
	
	// sample sized used to downsize from native photo
	int inSampleSize;
	
	// Saved Image Uri
	Uri savedImageUri;
	
	//handles threaded events for the UI thread
    private Handler mHandler = new Handler();

    //UI for background threads
    ProgressDialog mProgressDialog;
    
    // Handles when we should do realtime preview and when we shouldn't
    boolean doRealtimePreview = true;
    
    // Keep track of the orientation
    private int originalImageOrientation = ExifInterface.ORIENTATION_NORMAL;    

	private class mAutoDetectTask extends AsyncTask<Integer, Integer, Long> {
		protected Long doInBackground(Integer... params) {
	    	  return (long)doAutoDetection();	         
	    }

	    protected void onProgressUpdate(Integer... progress) {
	       
	    }

	    protected void onPostExecute(Long result) {
	     
	    	mProgressDialog.dismiss();
	    	 
	    	Toast autodetectedToast = Toast.makeText(ImageEditor.this, result + " face(s) detected", Toast.LENGTH_SHORT);
	 		autodetectedToast.show();
	    }
	}
    	
    
	private BroadcastReceiver ibr = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(InformaConstants.Keys.Service.GENERATE_IMAGE.equals(intent.getAction())) {
				generateInformaImage(intent.getStringExtra(
						InformaConstants.Keys.Image.LOCAL_MEDIA_PATH), 
						intent.getStringExtra(InformaConstants.Keys.Image.METADATA),
						intent.getIntExtra(InformaConstants.Keys.Service.IMAGES_GENERATED, 0));				
			}
			
		}
		
	};
	
	private int numInformaImagesGenerated = 0;
	
	@SuppressWarnings("unused")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		
		sendBroadcast(new Intent()
			.setAction(InformaConstants.Keys.Service.SET_CURRENT)
			.putExtra(InformaConstants.Keys.CaptureEvent.MATCH_TIMESTAMP, System.currentTimeMillis())
			.putExtra(InformaConstants.Keys.CaptureEvent.TYPE, InformaConstants.CaptureEvents.MEDIA_CAPTURED));
		
		
        String versNum = "";
        
        try {
            String pkg = getPackageName();
            versNum = getPackageManager().getPackageInfo(pkg, 0).versionName;
        } catch (Exception e) {
        	versNum = "";
        }

        setTitle(getString(R.string.app_name) + " (" + versNum + ")");
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.imageviewer);

		// Calculate the minimum distance
		minMoveDistance = minMoveDistanceDP * this.getResources().getDisplayMetrics().density + 0.5f;
		
		// The ImageView that contains the image we are working with
		imageView = (ImageView) findViewById(R.id.ImageEditorImageView);

		// Buttons for zooming
		zoomIn = (Button) this.findViewById(R.id.ZoomIn);
		zoomOut = (Button) this.findViewById(R.id.ZoomOut);
		btnNew = (Button) this.findViewById(R.id.New);
		btnSave = (Button) this.findViewById(R.id.Save);
		btnShare = (Button) this.findViewById(R.id.Share);
		btnPreview = (Button) this.findViewById(R.id.Preview);
		
		// this, ImageEditor will be the onClickListener for the buttons
		zoomIn.setOnClickListener(this);
		zoomOut.setOnClickListener(this);
		btnNew.setOnClickListener(this);
		btnSave.setOnClickListener(this);
		btnShare.setOnClickListener(this);
		btnPreview.setOnClickListener(this);

		// Instantiate the vibrator
		vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		
		// Passed in from CameraObscuraMainMenu
		originalImageUri = getIntent().getData();
		
		// If originalImageUri is null, we are likely coming from another app via "share"
		if (originalImageUri == null)
		{
			if (getIntent().hasExtra(Intent.EXTRA_STREAM)) 
			{
				originalImageUri = (Uri) getIntent().getExtras().get(Intent.EXTRA_STREAM);
			}
			else if (getIntent().hasExtra("bitmap"))
			{
				Bitmap b = (Bitmap)getIntent().getExtras().get("bitmap");
				setBitmap(b);
				
				boolean autodetect = true;

				if (autodetect)
				{

					mProgressDialog = ProgressDialog.show(this, "", "Detecting faces...", true, true);
					
					new mAutoDetectTask().execute(1);
					
					
				}
				
				originalImageWidth = b.getWidth();
				originalImageHeight = b.getHeight();
				return;
				
			}
		}
		
		
		// Load the image if it isn't null
		if (originalImageUri != null) {
			
			// Get the orientation
			File originalFilename = pullPathFromUri(originalImageUri);			
			try {
				ExifInterface ei = new ExifInterface(originalFilename.getAbsolutePath());
				originalImageOrientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
				debug(ObscuraConstants.TAG,"Orientation: " + originalImageOrientation);
			} catch (IOException e1) {
				debug(ObscuraConstants.TAG,"Couldn't get Orientation");
				e1.printStackTrace();
			}

			// Load up smaller image
			try {
				// Load up the image's dimensions not the image itself
				BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
				bmpFactoryOptions.inJustDecodeBounds = true;
				// Needs to be this config for Google Face Detection 
				bmpFactoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
				// Parse the image
				Bitmap loadedBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(originalImageUri), null, bmpFactoryOptions);

				// Hold onto the unscaled dimensions
				originalImageWidth = bmpFactoryOptions.outWidth;
				originalImageHeight = bmpFactoryOptions.outHeight;
				// If it is rotated, transpose the width and height
				// Should probably look to see if there are different rotation constants being used
				if (originalImageOrientation == ExifInterface.ORIENTATION_ROTATE_90 
						|| originalImageOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
					int tmpWidth = originalImageWidth;
					originalImageWidth = originalImageHeight;
					originalImageHeight = tmpWidth;
				}

				// Get the current display to calculate ratios
				Display currentDisplay = getWindowManager().getDefaultDisplay();
				
				// Ratios between the display and the image
				double widthRatio =  Math.floor(bmpFactoryOptions.outWidth / currentDisplay.getWidth());
				double heightRatio = Math.floor(bmpFactoryOptions.outHeight / currentDisplay.getHeight());

				
				// If both of the ratios are greater than 1,
				// one of the sides of the image is greater than the screen
				if (heightRatio > 1 && widthRatio > 1) {
					if (heightRatio > widthRatio) {
						// Height ratio is larger, scale according to it
						inSampleSize = (int)heightRatio;
					} else {
						// Width ratio is larger, scale according to it
						inSampleSize = (int)widthRatio;
					}
				}
				else
				{
					inSampleSize = 1;
				}
				
				bmpFactoryOptions.inSampleSize = inSampleSize;
		
				// Decode it for real
				bmpFactoryOptions.inJustDecodeBounds = false;
				loadedBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(originalImageUri), null, bmpFactoryOptions);
				debug(ObscuraConstants.TAG,"Was: " + loadedBitmap.getConfig());

				if (loadedBitmap == null) {
					debug(ObscuraConstants.TAG,"bmp is null");
				
				}
				else
				{
					// Only dealing with 90 and 270 degree rotations, might need to check for others
					if (originalImageOrientation == ExifInterface.ORIENTATION_ROTATE_90) 
					{
						debug(ObscuraConstants.TAG,"Rotating Bitmap 90");
						Matrix rotateMatrix = new Matrix();
						rotateMatrix.postRotate(90);
						loadedBitmap = Bitmap.createBitmap(loadedBitmap,0,0,loadedBitmap.getWidth(),loadedBitmap.getHeight(),rotateMatrix,false);
					}
					else if (originalImageOrientation == ExifInterface.ORIENTATION_ROTATE_270) 
					{
						debug(ObscuraConstants.TAG,"Rotating Bitmap 270");
						Matrix rotateMatrix = new Matrix();
						rotateMatrix.postRotate(270);
						loadedBitmap = Bitmap.createBitmap(loadedBitmap,0,0,loadedBitmap.getWidth(),loadedBitmap.getHeight(),rotateMatrix,false);
					}

					setBitmap (loadedBitmap);
					
					boolean autodetect = true;

					if (autodetect)
					{
						// Do auto detect popup

						mProgressDialog = ProgressDialog.show(this, "", "Detecting faces...", true, true);
					
						new mAutoDetectTask().execute(1);
					}
				}				
			} catch (IOException e) {
				Log.e(ObscuraConstants.TAG, "error loading bitmap from Uri: " + e.getMessage(), e);
			}
			
			
			
		}
		
		bitmapCornerUL = BitmapFactory.decodeResource(getResources(),
                R.drawable.edit_region_corner_ul);
		bitmapCornerUR = BitmapFactory.decodeResource(getResources(),
                R.drawable.edit_region_corner_ur);
		bitmapCornerLL = BitmapFactory.decodeResource(getResources(),
                R.drawable.edit_region_corner_ll);
		bitmapCornerLR = BitmapFactory.decodeResource(getResources(),
                R.drawable.edit_region_corner_lr);
		 
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	private void setBitmap (Bitmap nBitmap)
	{
		imageBitmap = nBitmap;
		
		// Get the current display to calculate ratios
		Display currentDisplay = getWindowManager().getDefaultDisplay();

		float matrixWidthRatio = (float) currentDisplay.getWidth() / (float) imageBitmap.getWidth();
		float matrixHeightRatio = (float) currentDisplay.getHeight() / (float) imageBitmap.getHeight();

		// Setup the imageView and matrix for scaling
		float matrixScale = matrixHeightRatio;
		
		if (matrixWidthRatio < matrixHeightRatio) {
			matrixScale = matrixWidthRatio;
		} 
		
		imageView.setImageBitmap(imageBitmap);

		// Set the OnTouch and OnLongClick listeners to this (ImageEditor)
		imageView.setOnTouchListener(this);
		imageView.setOnClickListener(this);
		
		
		//PointF midpoint = new PointF((float)imageBitmap.getWidth()/2f, (float)imageBitmap.getHeight()/2f);
		matrix.postScale(matrixScale, matrixScale);

		// This doesn't completely center the image but it get's closer
		//int fudge = 42;
		matrix.postTranslate((float)((float)currentDisplay.getWidth()-(float)imageBitmap.getWidth()*(float)matrixScale)/2f,(float)((float)currentDisplay.getHeight()-(float)imageBitmap.getHeight()*matrixScale)/2f);
		
		imageView.setImageMatrix(matrix);
		
		
	}
	/*
	 * Call this to delete the original image, will ask the user
	 */
	private void showDeleteOriginalDialog() 
	{
		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setIcon(android.R.drawable.ic_dialog_alert);
		b.setTitle(getString(R.string.app_name));
		b.setMessage(getString(R.string.confirm_delete));
		b.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            	try
            	{
	                // User clicked OK so go ahead and delete
	        		deleteOriginal();
	            	viewImage(savedImageUri);
            	}
            	catch (IOException e)
            	{
            		Log.e(ObscuraConstants.TAG, "error saving", e);
            	}
            	finally
            	{
            		finish();
            	}
            }
        });
		b.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            	viewImage(savedImageUri);
            }
        });
		b.show();
	}
	
	/*
	 * Actual deletion of original
	 */
	private void deleteOriginal() throws IOException
	{
		
		if (originalImageUri != null)
		{
			if (originalImageUri.getScheme().equals("file"))
			{
				String origFilePath = originalImageUri.getPath();
				File fileOrig = new File(origFilePath);

				String[] columnsToSelect = { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA };
				
				/*
				ExifInterface ei = new ExifInterface(origFilePath);
				long dateTaken = new Date(ei.getAttribute(ExifInterface.TAG_DATETIME)).getTime();
				*/
				
				Uri[] uriBases = {MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.INTERNAL_CONTENT_URI};
				
				for (Uri uriBase : uriBases)
				{
					
			    	Cursor imageCursor = getContentResolver().query(uriBase, columnsToSelect, MediaStore.Images.Media.DATA + " = ?",  new String[] {origFilePath}, null );
					//Cursor imageCursor = getContentResolver().query(uriBase, columnsToSelect, MediaStore.Images.Media.DATE_TAKEN + " = ?",  new String[] {dateTaken+""}, null );
					
			        while (imageCursor.moveToNext())
			        {
			        
				       long _id = imageCursor.getLong(imageCursor.getColumnIndex(MediaStore.Images.Media._ID));
				    	   
				       getContentResolver().delete(ContentUris.withAppendedId(uriBase, _id), null, null);
				       
			    	}
				}
				
				if (fileOrig.exists())
					fileOrig.delete();
				
			}
			else
			{
				getContentResolver().delete(originalImageUri, null, null);
			}
		}
		
		originalImageUri = null;
	}
	
	
	private int doAutoDetection() {
		// This should be called via a pop-up/alert mechanism
		
		RectF[] autodetectedRects = runFaceDetection();
		for (int adr = 0; adr < autodetectedRects.length; adr++) {

			//debug(ObscuraApp.TAG,"AUTODETECTED imageView Width, Height: " + imageView.getWidth() + " " + imageView.getHeight());
			//debug(ObscuraApp.TAG,"UNSCALED RECT:" + autodetectedRects[adr].left + " " + autodetectedRects[adr].top + " " + autodetectedRects[adr].right + " " + autodetectedRects[adr].bottom);
			
			RectF autodetectedRectScaled = new RectF(autodetectedRects[adr].left, autodetectedRects[adr].top, autodetectedRects[adr].right, autodetectedRects[adr].bottom);
			
			//debug(ObscuraApp.TAG,"SCALED RECT:" + autodetectedRectScaled.left + " " + autodetectedRectScaled.top + " " + autodetectedRectScaled.right + " " + autodetectedRectScaled.bottom);

			// Probably need to map autodetectedRects to scaled rects
		//debug(ObscuraApp.TAG,"MAPPED RECT:" + autodetectedRects[adr].left + " " + autodetectedRects[adr].top + " " + autodetectedRects[adr].right + " " + autodetectedRects[adr].bottom);
			
			float faceBuffer = (autodetectedRectScaled.right-autodetectedRectScaled.left)/5;
			
			boolean isLast = false;
			if (adr == autodetectedRects.length - 1) {
				isLast = true;
			}
			createImageRegion(
					(autodetectedRectScaled.left-faceBuffer),
					(autodetectedRectScaled.top-faceBuffer),
					(autodetectedRectScaled.right+faceBuffer),
					(autodetectedRectScaled.bottom+faceBuffer),
					isLast,
					isLast);
		}	 				
		
		return autodetectedRects.length;
	}
	
	/*
	 * The actual face detection calling method
	 */
	private RectF[] runFaceDetection() {
		RectF[] possibleFaceRects;
		
		try {
			Bitmap bProc = toGrayscale(imageBitmap);
			GoogleFaceDetection gfd = new GoogleFaceDetection(bProc);
			int numFaces = gfd.findFaces();
	        debug(ObscuraConstants.TAG,"Num Faces Found: " + numFaces); 
	        possibleFaceRects = gfd.getFaces();
		} catch(NullPointerException e) {
			possibleFaceRects = null;
		}
		return possibleFaceRects;				
	}
	
	public Bitmap toGrayscale(Bitmap bmpOriginal)
	{        
	    int width, height;
	    height = bmpOriginal.getHeight();
	    width = bmpOriginal.getWidth();    

	    Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
	    Canvas c = new Canvas(bmpGrayscale);
	    Paint paint = new Paint();
	    ColorMatrix cm = new ColorMatrix();
	    cm.setSaturation(0);
	    
	    ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
	    
	    paint.setColorFilter(f);
	 
	    c.drawBitmap(bmpOriginal, 0, 0, paint);
	    
	    
	    
	    return bmpGrayscale;
	}
	
	public static Bitmap createContrast(Bitmap src, double value) {
		// image size
		int width = src.getWidth();
		int height = src.getHeight();
		// create output bitmap
		Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
		// color information
		int A, R, G, B;
		int pixel;
		// get contrast value
		double contrast = Math.pow((100 + value) / 100, 2);

		// scan through all pixels
		for(int x = 0; x < width; ++x) {
			for(int y = 0; y < height; ++y) {
				// get pixel color
				pixel = src.getPixel(x, y);
				A = Color.alpha(pixel);
				// apply filter contrast for every channel R, G, B
				R = Color.red(pixel);
				R = (int)(((((R / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
				if(R < 0) { R = 0; }
				else if(R > 255) { R = 255; }

				G = Color.red(pixel);
				G = (int)(((((G / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
				if(G < 0) { G = 0; }
				else if(G > 255) { G = 255; }

				B = Color.red(pixel);
				B = (int)(((((B / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
				if(B < 0) { B = 0; }
				else if(B > 255) { B = 255; }

				// set new pixel color to output bitmap
				bmOut.setPixel(x, y, Color.argb(A, R, G, B));
			}
		}

		// return final image
		return bmOut;
	}


	ImageRegion currRegion = null;
	
	/*
	 * Handles touches on ImageView
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) 
	{
		if (currRegion != null && (mode == ObscuraConstants.DRAG || currRegion.getBounds().contains(event.getX(), event.getY())))		
			return onTouchRegion(v, event, currRegion);	
		else
			return onTouchImage(v,event);
	}
	
	public ImageRegion findRegion (MotionEvent event)
	{
		ImageRegion result = null;
		Matrix iMatrix = new Matrix();
		matrix.invert(iMatrix);

		float[] points = {event.getX(), event.getY()};        	
    	iMatrix.mapPoints(points);
    	
		for (ImageRegion region : imageRegions)
		{

			if (region.getBounds().contains(points[0],points[1]))
			{
				result = region;
				
				break;
			}
			
		}
	
		
		return result;
	}
	
	public boolean onTouchRegion (View v, MotionEvent event, ImageRegion iRegion)
	{
		boolean handled = false;
		
		currRegion.setMatrix(matrix);
		
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				clearImageRegionsEditMode();
				currRegion.setSelected(true);	
				
				currRegion.setCornerMode(event.getX(),event.getY());
				
				mode = ObscuraConstants.DRAG;
				handled = iRegion.onTouch(v, event);

			break;
			
			case MotionEvent.ACTION_UP:
				mode = ObscuraConstants.NONE;
				handled = iRegion.onTouch(v, event);
				currRegion.setSelected(false);
				//if (handled)
					//currRegion = null;
			
			break;
			
			default:
				mode = ObscuraConstants.DRAG;
				handled = iRegion.onTouch(v, event);
			
		}
		
		return handled;
		
		
	}
	
	public boolean onTouchImage(View v, MotionEvent event) 
	{
		boolean handled = false;
		
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				// Single Finger down
				mode = ObscuraConstants.TAP;				
				ImageRegion newRegion = findRegion(event);
				
				if (newRegion != null)
				{
					currRegion = newRegion;
					return onTouchRegion(v,  event, currRegion);
				}
				else if (currRegion == null)
				{
					
					// 	Save the Start point. 
					startPoint.set(event.getX(), event.getY());
				}
				else
				{
					currRegion.setSelected(false);
					currRegion = null;

				}
				
				
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

				//Log.d(ObscuraApp.TAG, "Start Finger Spacing=" + startFingerSpacing);
				
				// Get the midpoint
				float xsum = event.getX(0) + event.getX(1);
				float ysum = event.getY(0) + event.getY(1);
				startFingerSpacingMidPoint.set(xsum / 2, ysum / 2);
				
				mode = ObscuraConstants.ZOOM;
				//Log.d(ObscuraApp.TAG, "mode=ZOOM");
				
				clearImageRegionsEditMode();
				
				break;
				
			case MotionEvent.ACTION_UP:
				// Single Finger Up
				
			    // Re-enable realtime preview
				doRealtimePreview = true;
				updateDisplayImage();
				
				//debug(ObscuraApp.TAG,"mode=NONE");

				break;
				
			case MotionEvent.ACTION_POINTER_UP:
				// Multiple Finger Up
				
			    // Re-enable realtime preview
				doRealtimePreview = true;
				updateDisplayImage();
				
				//Log.d(ObscuraApp.TAG, "mode=NONE");
				break;
				
			case MotionEvent.ACTION_MOVE:
				
				// Calculate distance moved
				float distance = (float) (Math.sqrt(Math.abs(startPoint.x - event.getX()) + Math.abs(startPoint.y - event.getY())));
				//debug(ObscuraApp.TAG,"Move Distance: " + distance);
				//debug(ObscuraApp.TAG,"Min Distance: " + minMoveDistance);
				
				// If greater than minMoveDistance, it is likely a drag or zoom
				if (distance > minMoveDistance) {
				
					if (mode == ObscuraConstants.TAP || mode == ObscuraConstants.DRAG) {
						mode = ObscuraConstants.DRAG;
						
						matrix.postTranslate(event.getX() - startPoint.x, event.getY() - startPoint.y);
						imageView.setImageMatrix(matrix);
//						// Reset the start point
						startPoint.set(event.getX(), event.getY());
	
						putOnScreen();
						//redrawRegions();
						
						handled = true;
	
					} else if (mode == ObscuraConstants.ZOOM) {
						
						// Save the current matrix so that if zoom goes to big, we can revert
						savedMatrix.set(matrix);
						
						// Get the spacing of the fingers, 2 fingers
						float ex = event.getX(0) - event.getX(1);
						float ey = event.getY(0) - event.getY(1);
						endFingerSpacing = (float) Math.sqrt(ex * ex + ey * ey);
	
						//Log.d(ObscuraApp.TAG, "End Finger Spacing=" + endFingerSpacing);
		
						// If we moved far enough
						if (endFingerSpacing > minMoveDistance) {
							
							// Ratio of spacing..  If it was 5 and goes to 10 the image is 2x as big
							float scale = endFingerSpacing / startFingerSpacing;
							// Scale from the midpoint
							matrix.postScale(scale, scale, startFingerSpacingMidPoint.x, startFingerSpacingMidPoint.y);
		
							// Make sure that the matrix isn't bigger than max scale/zoom
							float[] matrixValues = new float[9];
							matrix.getValues(matrixValues);
							
							if (matrixValues[0] > ObscuraConstants.MAX_SCALE) {
								matrix.set(savedMatrix);
							}
							imageView.setImageMatrix(matrix);
							
							putOnScreen();
							//redrawRegions();
	
							// Reset Start Finger Spacing
							float esx = event.getX(0) - event.getX(1);
							float esy = event.getY(0) - event.getY(1);
							startFingerSpacing = (float) Math.sqrt(esx * esx + esy * esy);
							//Log.d(ObscuraApp.TAG, "New Start Finger Spacing=" + startFingerSpacing);
							
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
			imageView.setImageBitmap(createObscuredBitmap(imageBitmap.getWidth(),imageBitmap.getHeight(), true));
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
		
		debug(ObscuraConstants.TAG,theRect.width() + " " + theRect.height());
		
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
		
		//debug(ObscuraApp.TAG,"Deltas:" + deltaX + " " + deltaY);
		
		matrix.postTranslate(deltaX,deltaY);
		imageView.setImageMatrix(matrix);
		updateDisplayImage();
		
	}
	
	/* 
	 * Put all regions into normal mode, out of edit mode
	 */
	public void clearImageRegionsEditMode()
	{
		Iterator<ImageRegion> itRegions = imageRegions.iterator();
		
		while (itRegions.hasNext())
		{
			itRegions.next().setSelected(false);
		}
		
	}
	
	/*
	 * Create new ImageRegion
	 */
	public void createImageRegion(float left, float top, float right, float bottom, boolean showPopup, boolean updateNow) {
		
		clearImageRegionsEditMode();
		
		ImageRegion imageRegion = new ImageRegion(
				this, 
				left, 
				top, 
				right, 
				bottom,
				matrix);

		imageRegions.add(imageRegion);
		
		if (updateNow)
		{
			mHandler.post(new Runnable ()
			{
				public void run() {
					putOnScreen();
				}
			});
		}
	}
	/*
	 * Associate the current log data to the image region's properties
	 */
	public void associateImageRegionData(ImageRegion ir) {
		sendBroadcast(new Intent()
			.setAction(InformaConstants.Keys.Service.SET_CURRENT)
			.putExtra(InformaConstants.Keys.CaptureEvent.MATCH_TIMESTAMP, (Long) ir.mRProc.getProperties().get(InformaConstants.Keys.ImageRegion.TIMESTAMP))
			.putExtra(InformaConstants.Keys.CaptureEvent.TYPE, InformaConstants.CaptureEvents.REGION_GENERATED));
	}
	
	/*
	 * Delete/Remove specific ImageRegion
	 */
	public void deleteRegion(ImageRegion ir)
	{
		imageRegions.remove(ir);
		//redrawRegions();
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
	 * Handles normal onClicks for buttons registered to this.
	 * Currently only the zoomIn and zoomOut buttons
	 */
	@Override
	public void onClick(View v) {
		
		if (currRegion != null)
		{
			currRegion.inflatePopup(false);
			currRegion = null;
		}			
		else if (v == zoomIn) 
		{
			float scale = 1.5f;
			
			PointF midpoint = new PointF(imageView.getWidth()/2, imageView.getHeight()/2);
			matrix.postScale(scale, scale, midpoint.x, midpoint.y);
			imageView.setImageMatrix(matrix);
			putOnScreen();
		} 
		else if (v == zoomOut) 
		{
			float scale = 0.75f;

			PointF midpoint = new PointF(imageView.getWidth()/2, imageView.getHeight()/2);
			matrix.postScale(scale, scale, midpoint.x, midpoint.y);
			imageView.setImageMatrix(matrix);
			
			putOnScreen();
		} 
		else if (v == btnNew)
		{
			newDefaultRegion();
		}
		else if (v == btnPreview)
		{
			showPreview();
		}
		else if (v == btnSave)
		{

			Intent keyChooser = new Intent(this, KeyChooser.class);
			startActivityForResult(keyChooser, InformaConstants.FROM_TRUSTED_DESTINATION_CHOOSER);
		}
		else if (v == btnShare)
		{
			// Share Image
      		shareImage();
		}
		else if (mode != ObscuraConstants.DRAG && mode != ObscuraConstants.ZOOM) 
		{
			float defaultSize = imageView.getWidth()/4;
			float halfSize = defaultSize/2;
			
			RectF newBox = new RectF();
			
			newBox.left = startPoint.x - halfSize;
			newBox.top = startPoint.y - halfSize;

			newBox.right = startPoint.x + halfSize;
			newBox.bottom = startPoint.y + halfSize;
			
			Matrix iMatrix = new Matrix();
			matrix.invert(iMatrix);
			iMatrix.mapRect(newBox);
						
			createImageRegion(newBox.left, newBox.top, newBox.right, newBox.bottom, true, true);
		}
		
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
	
	private void newDefaultRegion ()
	{
		// Set the Start point. 
		startPoint.set(imageView.getWidth()/2, imageView.getHeight()/2);
		
		float defaultSize = imageView.getWidth()/4;
		float halfSize = defaultSize/2;
		
		RectF newRegion = new RectF();
		
		newRegion.left = startPoint.x - halfSize;
		newRegion.top = startPoint.y - halfSize;

		newRegion.right =  startPoint.x + defaultSize;
		newRegion.left =  startPoint.y + defaultSize;
		
		Matrix iMatrix = new Matrix();
		matrix.invert(iMatrix);
		iMatrix.mapRect(newRegion);
		
		createImageRegion(newRegion.left,newRegion.top,newRegion.right,newRegion.bottom, false, true);
		
	}
    /*
     * Normal menu item selected method.  Uses menu items defined in XML: res/menu/image_editor_menu.xml
     */
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {

    	switch (item.getItemId()) {
    	
    		case R.id.menu_new_region:
    			
    			newDefaultRegion();

    			return true;
    			
        	case R.id.menu_save:

				Intent keyChooser = new Intent(this, KeyChooser.class);
				startActivityForResult(keyChooser, InformaConstants.FROM_TRUSTED_DESTINATION_CHOOSER);
        		
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
		
		StringBuffer msg = new StringBuffer();
		
		msg.append(getString(R.string.app_name));
		
        String versNum = "";
        
        try {
            String pkg = getPackageName();
            versNum = getPackageManager().getPackageInfo(pkg, 0).versionName;
        } catch (Exception e) {
        	versNum = "";
        }
        
        msg.append(" v" + versNum);
        msg.append('\n');
        msg.append('\n');
        
        msg.append(getString(R.string.about));
	        
        msg.append('\n');
        msg.append('\n');
        
        msg.append(getString(R.string.about2));
        
        msg.append('\n');
        msg.append('\n');
        
        msg.append(getString(R.string.about3));
        
		showDialog(msg.toString());
	}
	
	private void showDialog (String msg)
	{
		 new AlertDialog.Builder(this)
         .setTitle(getString(R.string.app_name))
         .setMessage(msg)
         .create().show();
	}
	
	/*
	 * Display preview image
	 */
	private void showPreview() {
		
		// Open Preview Activity
		Uri tmpImageUri = saveTmpImage();
		
		if (tmpImageUri != null)
		{
			Intent intent = new Intent(this, ImagePreview.class);
			intent.putExtra(ImagePreview.IMAGEURI, tmpImageUri.toString());
			startActivity(intent);				
		}
	}
	
	/*
	 * When the user selects the Share menu item
	 * Uses saveTmpImage (overwriting what is already there) and uses the standard Android Share Intent
	 */
    private void shareImage() {
    	Uri tmpImageUri;
    	
    	if ((tmpImageUri = saveTmpImage()) != null) {
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
	 * When the user selects the Share menu item
	 * Uses saveTmpImage (overwriting what is already there) and uses the standard Android Share Intent
	 */
    private void viewImage(Uri imgView) {
    	
    	Intent iView = new Intent(Intent.ACTION_VIEW);
    	iView.setType(ObscuraConstants.MIME_TYPE_JPEG);
    	iView.putExtra(Intent.EXTRA_STREAM, imgView);
    	iView.setDataAndType(imgView, ObscuraConstants.MIME_TYPE_JPEG);

    	startActivity(Intent.createChooser(iView, "View Image"));    	
	
    }
    
    
    /*
     * Goes through the regions that have been defined and creates a bitmap with them obscured.
     * This may introduce memory issues and therefore have to be done in a different manner.
     */
    private Bitmap createObscuredBitmap(int width, int height, boolean showBorders) 
    {
    	if (imageBitmap == null)
    		return null;
    	
    	if (obscuredBmp == null || (obscuredBmp.getWidth() != width))
    	{
    		// Create the bitmap that we'll output from this method
    		obscuredBmp = Bitmap.createBitmap(width, height,imageBitmap.getConfig());
    	
    		// Create the canvas to draw on
    		obscuredCanvas = new Canvas(obscuredBmp); 
    	}
    	
    	// Create the paint used to draw with
    	obscuredPaint = new Paint();   
    	// Create a default matrix
    	Matrix obscuredMatrix = new Matrix();    	
    	// Draw the scaled image on the new bitmap
    	obscuredCanvas.drawBitmap(imageBitmap, obscuredMatrix, obscuredPaint);
    	
    	// Iterate through the regions that have been created
    	Iterator<ImageRegion> i = imageRegions.iterator();
	    while (i.hasNext()) 
	    {
	    	ImageRegion currentRegion = i.next();
	    	RegionProcesser om = currentRegion.getRegionProcessor();

            RectF regionRect = new RectF(currentRegion.getBounds());
	    	om.processRegion(regionRect, obscuredCanvas, obscuredBmp);

	    	if (showBorders)
	    	{
		    	if (currentRegion.isSelected())
		    		obscuredPaint.setColor(Color.GREEN);
		    	else
		    		obscuredPaint.setColor(Color.WHITE);
		    	
		    	obscuredPaint.setStyle(Style.STROKE);
		    	obscuredPaint.setStrokeWidth(10f);
		    	obscuredCanvas.drawRect(regionRect, obscuredPaint);
		    	
		    	float cSize = CORNER_SIZE;
		    	
		    	if (currentRegion.isSelected())
		    	{
		    		obscuredCanvas.drawBitmap(bitmapCornerUL, regionRect.left-cSize, regionRect.top-cSize, obscuredPaint);
		    		obscuredCanvas.drawBitmap(bitmapCornerLL, regionRect.left-cSize, regionRect.bottom-(cSize/2), obscuredPaint);
		    		obscuredCanvas.drawBitmap(bitmapCornerUR, regionRect.right-(cSize/2), regionRect.top-cSize, obscuredPaint);
		    		obscuredCanvas.drawBitmap(bitmapCornerLR, regionRect.right-(cSize/2), regionRect.bottom-(cSize/2), obscuredPaint);

		    	}
		    	
	    	}
		}

	    return obscuredBmp;
    }
    
    private boolean canDoNative ()
    {
    	if (originalImageUri == null)
    		return false;
    				
    	// Iterate through the regions that have been created
    	Iterator<ImageRegion> i = imageRegions.iterator();
	    while (i.hasNext()) 
	    {
	    	ImageRegion iRegion = i.next();
	    }
	    
	    return true;

    }
    
    private void copy (Uri uriSrc, File fileTarget) throws IOException
    {
    	InputStream is = getContentResolver().openInputStream(uriSrc);
		
		OutputStream os = new FileOutputStream (fileTarget);
			
		copyStreams (is, os);

    	
    }
    
    private void copy (Uri uriSrc, Uri uriTarget) throws IOException
    {
    	
    	InputStream is = getContentResolver().openInputStream(uriSrc);
		
		OutputStream os = getContentResolver().openOutputStream(uriTarget);
			
		copyStreams (is, os);

    	
    }
    
    private static void copyStreams(InputStream input, OutputStream output) throws IOException {
        // if both are file streams, use channel IO
        if ((output instanceof FileOutputStream) && (input instanceof FileInputStream)) {
          try {
            FileChannel target = ((FileOutputStream) output).getChannel();
            FileChannel source = ((FileInputStream) input).getChannel();

            source.transferTo(0, Integer.MAX_VALUE, target);

            source.close();
            target.close();

            return;
          } catch (Exception e) { /* failover to byte stream version */
          }
        }

        byte[] buf = new byte[8192];
        while (true) {
          int length = input.read(buf);
          if (length < 0)
            break;
          output.write(buf, 0, length);
        }

        try {
          input.close();
        } catch (IOException ignore) {
        }
        try {
          output.close();
        } catch (IOException ignore) {
        }
      }
    /*
     * Save a temporary image for sharing only
     */
    private Uri saveTmpImage() {
    	
    	String storageState = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(storageState)) {
        	Toast t = Toast.makeText(this,"External storage not available", Toast.LENGTH_SHORT); 
    		t.show();
    		return null;
    	}
    	
    	// Create the bitmap that will be saved
    	// Perhaps this should be smaller than screen size??
    	int w = imageBitmap.getWidth();
    	int h = imageBitmap.getHeight();
    	Bitmap obscuredBmp = createObscuredBitmap(w,h, false);
    	
    	// Create the Uri - This can't be "private"
    	File tmpFileDirectory = new File(Environment.getExternalStorageDirectory().getPath() + ObscuraConstants.TMP_FILE_DIRECTORY);
    	File tmpFile = new File(tmpFileDirectory, ObscuraConstants.TMP_FILE_NAME);
    	debug(ObscuraConstants.TAG, tmpFile.getPath());
    	
		try {
	    	if (!tmpFileDirectory.exists()) {
	    		tmpFileDirectory.mkdirs();
	    	}
	    	Uri tmpImageUri = Uri.fromFile(tmpFile);
	    	
			OutputStream imageFileOS;

			int quality = 75;
			imageFileOS = getContentResolver().openOutputStream(tmpImageUri);
			obscuredBmp.compress(CompressFormat.JPEG, quality, imageFileOS);

			return tmpImageUri;
		} catch (FileNotFoundException e) {
			mProgressDialog.cancel();
			e.printStackTrace();
			return null;
		}
    }
    
    /*
     * TODO: handle the deletion of the original image
     * according to preferences
     */
    private void handleDelete() {
    	// delete temp file created.
    }
    
    /*
     * The method that actually saves the altered image.  
     * This in combination with createObscuredBitmap could/should be done in another, more memory efficient manner. 
     */
    private void generateInformaImage(String informaPath, String informaMetadata, int numExpected) {
    	try {
			
			if(canDoNative()) {
				Log.d(InformaConstants.TAG, "running redaction on " + informaPath);
				
				File tmp = new File(InformaConstants.DUMP_FOLDER, ObscuraConstants.TMP_FILE_NAME);
				if(tmp.exists())
					tmp.delete();
				copy(originalImageUri, tmp);
				
				ImageConstructor ic = new ImageConstructor(
						tmp.getAbsolutePath(),
						informaPath,
						informaMetadata);
				
			}
			
			numInformaImagesGenerated++;
			if(numInformaImagesGenerated == numExpected) {
				mProgressDialog.cancel();
				handleDelete();
				unregisterReceiver(ibr);
				Log.d(ObscuraConstants.TAG, "goodbye br!");
				viewImage(savedImageUri);
			}
		} catch (Exception e) {
			Log.e(ObscuraConstants.TAG, "error doing redact",e);
			
		}	
    }
    
    /*
     * Pass the path of the original to informa, 
     * along with the metadata and intended destinations
     */
    private boolean saveImage(String imageRegionObject, long[] encryptList) throws FileNotFoundException 
    {
    	
    	SimpleDateFormat dateFormat = new SimpleDateFormat(ObscuraConstants.EXPORT_DATE_FORMAT);
    	Date date = new Date();
    	String dateString = dateFormat.format(date);
    	
    	ContentValues cv = new ContentValues();
    	cv.put(Images.Media.DATE_ADDED, dateString);
    	cv.put(Images.Media.DATE_TAKEN, dateString);
    	cv.put(Images.Media.DATE_MODIFIED, dateString);
    	cv.put(Images.Media.DESCRIPTION, ObscuraConstants.ExifValues.DESCRIPTION);
    	cv.put(Images.Media.TITLE, ObscuraConstants.ExifValues.TITLE);
    	
    	savedImageUri = getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, cv);
    	obscuredBmp = createObscuredBitmap(imageBitmap.getWidth(),imageBitmap.getHeight(), false);

		OutputStream imageFileOS;
	
		int quality = 100; //lossless?  good question - still a smaller version
		imageFileOS = getContentResolver().openOutputStream(savedImageUri);
		obscuredBmp.compress(CompressFormat.JPEG, quality, imageFileOS);
		
		//TODO: handle the exif wiping...
		
		// force mediascanner to update file
		MediaScannerConnection.scanFile(
				this,
				new String[] {pullPathFromUri(savedImageUri).getAbsolutePath()},
				new String[] {ObscuraConstants.MIME_TYPE_JPEG},
				null);
			
		sendBroadcast(new Intent()
			.setAction(InformaConstants.Keys.Service.SET_CURRENT)
			.putExtra(InformaConstants.Keys.CaptureEvent.MATCH_TIMESTAMP, System.currentTimeMillis())
			.putExtra(InformaConstants.Keys.CaptureEvent.TYPE, InformaConstants.CaptureEvents.MEDIA_SAVED));
		
    	Intent informa = new Intent()
			.setAction(InformaConstants.Keys.Service.SEAL_LOG)
			.putExtra(InformaConstants.Keys.ImageRegion.DATA, imageRegionObject)
			.putExtra(InformaConstants.Keys.Image.LOCAL_MEDIA_PATH, pullPathFromUri(originalImageUri).getAbsolutePath());
			
		if(encryptList[0] != 0)
			informa.putExtra(InformaConstants.Keys.Intent.ENCRYPT_LIST, encryptList);
		
    	sendBroadcast(informa);
    	
    	
		return true;
    }
    
    private void launchInforma(final String imageRegionObject, final long[] encryptList) {
    	
    	
    	//Why does this not show?
    	mProgressDialog = ProgressDialog.show(this, "", "Saving...", true, true);

		mHandler.postDelayed(new Runnable() {
			  @Override
			  public void run() {
			    // this will be done in the Pipeline Thread
	        		try {
						saveImage(imageRegionObject, encryptList);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
			  }
			},500);
    }
    
    public File pullPathFromUri(Uri originalUri) {

    	String originalImageFilePath = null;

    	if (originalUri.getScheme() != null && originalUri.getScheme().equals("file"))
    	{
    		originalImageFilePath = originalUri.toString();
    	}
    	else
    	{
	    	String[] columnsToSelect = { MediaStore.Images.Media.DATA };
	    	Cursor imageCursor = getContentResolver().query(originalUri, columnsToSelect, null, null, null );
	    	if ( imageCursor != null && imageCursor.getCount() == 1 ) {
		        imageCursor.moveToFirst();
		        originalImageFilePath = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
	    	}
    	}

    	return new File(originalImageFilePath);
    }
    
    /*
     * Handling screen configuration changes ourselves, we don't want the activity to restart on rotation
     */
    @Override
    public void onConfigurationChanged(Configuration conf) 
    {
        super.onConfigurationChanged(conf);
    
        Thread thread = new Thread ()
        {
        	public void run ()
        	{        
        		mHandler.postDelayed(new Runnable () { public void run () { putOnScreen();}},100);        		
        	}
        };
        
        
        thread.start();
    }    
    
    public void launchTagger(ImageRegion ir) {
    	Intent informa = new Intent(this, Tagger.class);
    	informa.putExtra(ObscuraConstants.ImageRegion.PROPERTIES, ir.getRegionProcessor().getProperties());
    	informa.putExtra(InformaConstants.Keys.ImageRegion.INDEX, imageRegions.indexOf(ir));
    	
    	ir.getRegionProcessor().processRegion(new RectF(ir.getBounds()), obscuredCanvas, obscuredBmp);
    	
    	if(ir.getRegionProcessor().getBitmap() != null) {
    		Bitmap b = ir.getRegionProcessor().getBitmap();
    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    		b.compress(Bitmap.CompressFormat.JPEG, 50, baos);
    		informa.putExtra(InformaConstants.Keys.ImageRegion.THUMBNAIL, baos.toByteArray());
    	}
    	
    	startActivityForResult(informa, InformaConstants.FROM_INFORMA_TAGGER);
    	
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(resultCode == Activity.RESULT_OK) {
    		if(requestCode == InformaConstants.FROM_INFORMA_TAGGER) {
    			// replace corresponding image region
    			@SuppressWarnings("unchecked")
				HashMap<String, Object> informaReturn = 
					(HashMap<String, Object>) data.getSerializableExtra(InformaConstants.Keys.ImageRegion.TAGGER_RETURN);    			
    			Properties mProp = imageRegions.get(data.getIntExtra(InformaConstants.Keys.ImageRegion.INDEX, 0))
    					.getRegionProcessor().getProperties();
    			
    			// iterate through returned hashmap and place these new properties in it.
    			for(Map.Entry<String, Object> entry : informaReturn.entrySet())
    				mProp.setProperty(entry.getKey(), entry.getValue().toString());
    			
    			imageRegions.get(data.getIntExtra(InformaConstants.Keys.ImageRegion.INDEX, 0))
    				.getRegionProcessor().setProperties(mProp);
    			    			
    		} else if(requestCode == InformaConstants.FROM_TRUSTED_DESTINATION_CHOOSER) {
    			Log.d(ObscuraConstants.TAG, "hello br!");
    			registerReceiver(ibr, new IntentFilter(InformaConstants.Keys.Service.GENERATE_IMAGE));
    			
    			JSONArray imageRegionObject = new JSONArray();
    			try {
	    			for(ImageRegion ir : imageRegions)
	    				imageRegionObject.put(ir.getRepresentation());
    			} catch(JSONException e) {
    				Log.d(InformaConstants.TAG, e.toString());
    			}
    			
    			long[] encryptList = new long[] {0L};
    			if(data.hasExtra(InformaConstants.Keys.Intent.ENCRYPT_LIST))
    				encryptList = data.getLongArrayExtra(InformaConstants.Keys.Intent.ENCRYPT_LIST);
    			
    			launchInforma(imageRegionObject.toString(), encryptList);
    		}
    	}
    }

	@Override
	protected void onPostResume() {
		super.onPostResume();
	}
	
	public Paint getPainter ()
	{
		return obscuredPaint;
	}
	
	private void debug (String tag, String message)
	{
		Log.d(tag, message);
	}
	

	public ImageView getImageView() {
		return imageView;
	}
	
	@Override
	public void onAttachedToWindow() {
	    super.onAttachedToWindow();
	    Window window = getWindow();
	    window.setFormat(PixelFormat.RGBA_8888);
	    window.getDecorView().getBackground().setDither(true);

	}

}
