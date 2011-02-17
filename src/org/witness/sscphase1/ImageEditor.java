package org.witness.sscphase1;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;

public class ImageEditor extends Activity implements OnTouchListener, OnClickListener {

	final static String LOGTAG = "CAMERA OBSCRUA";

	// Image Matrix
	Matrix matrix = new Matrix();
	
	// Saved Matrix
	Matrix savedMatrix = new Matrix();

	// Initial Matrix
	Matrix baseMatrix = new Matrix();
	
	// We can be in one of these 3 states
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	int mode = NONE;

	static final float MAX_SCALE = 10f;

	// For Zooming
	float startFingerSpacing = 0f;
	float endFingerSpacing = 0f;
	PointF startFingerSpacingMidPoint = new PointF();
	
	// For Dragging
	PointF startPoint = new PointF();

	Button zoomIn, zoomOut;
	ImageView view;
	
	Bitmap realBmp;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        requestWindowFeature(Window.FEATURE_NO_TITLE);  
		setContentView(R.layout.imageviewer);
		view = (ImageView) findViewById(R.id.ImageView01);

		zoomIn = (Button) this.findViewById(R.id.ZoomIn);
		zoomOut = (Button) this.findViewById(R.id.ZoomOut);

		zoomIn.setOnClickListener(this);
		zoomOut.setOnClickListener(this);

		//Uri imageUri = getIntent().getData();

		try {
			//URL imageUrl = new URL(imageUri.toString());
			URL imageUrl = new URL("http://www.walking-productions.com/pictures/halloween2002/sandk.jpg");

			realBmp = BitmapFactory.decodeStream(imageUrl.openStream());

			Display display = getWindowManager().getDefaultDisplay();
			int width = display.getWidth();
			int height = display.getHeight();

			Log.v(LOGTAG, "Display Width: " + display.getWidth());
			Log.v(LOGTAG, "Display Height: " + display.getHeight());

			Log.v(LOGTAG, "BMP Width: " + realBmp.getWidth());
			Log.v(LOGTAG, "BMP Height: " + realBmp.getHeight());

			if (realBmp.getWidth() > width || realBmp.getHeight() > height) {

				float heightRatio = (float) height
						/ (float) realBmp.getHeight();
				float widthRatio = (float) width / (float) realBmp.getWidth();

				Log.v(LOGTAG, "heightRatio:" + heightRatio);
				Log.v(LOGTAG, "widthRatio: " + widthRatio);

				float scale = widthRatio;
				if (heightRatio < widthRatio) {
					scale = heightRatio;
				}

				matrix.setScale(scale, scale);
				Log.v(LOGTAG, "Scale: " + scale);
			} else {
				Log.v(LOGTAG, "NOTNOTNOT");
				matrix.setTranslate(1f, 1f);
			}

			view.setImageBitmap(realBmp);
			view.setImageMatrix(matrix);
			baseMatrix.set(matrix);
			view.setOnTouchListener(this);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean onTouch(View v, MotionEvent event) {

		ImageView view = (ImageView) v;

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:

				savedMatrix.set(matrix);

				// Save the Start point.  We have a single finger so it is drag
				startPoint.set(event.getX(), event.getY());
				mode = DRAG;
				Log.d(LOGTAG, "mode=DRAG");
				
				break;
				
			case MotionEvent.ACTION_POINTER_DOWN:
			
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
				// Nothing
				
			case MotionEvent.ACTION_POINTER_UP:
				
				mode = NONE;
				Log.d(LOGTAG, "mode=NONE");
				break;
				
			case MotionEvent.ACTION_MOVE:
				
				if (mode == DRAG) {

					matrix.set(savedMatrix);
					matrix.postTranslate(event.getX() - startPoint.x, event.getY() - startPoint.y);
					view.setImageMatrix(matrix);
					putOnScreen();
					
				} else if (mode == ZOOM) {

					// Get the spacing of the fingers, 2 fingers
					float ex = event.getX(0) - event.getX(1);
					float ey = event.getY(0) - event.getY(1);
					endFingerSpacing = (float) Math.sqrt(ex * ex + ey * ey);

					Log.d(LOGTAG, "End Finger Spacing=" + endFingerSpacing);
	
					if (endFingerSpacing > 10f) {
						matrix.set(savedMatrix);
						
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
						view.setImageMatrix(matrix);
						putOnScreen();
					}
				}
				break;
		}

		return true; // indicate event was handled
	}

	public void putOnScreen() {

		// Get Rectangle of Tranformed Image
		Matrix currentDisplayMatrix = new Matrix();
		currentDisplayMatrix.set(baseMatrix);
		currentDisplayMatrix.postConcat(matrix);
		
		RectF theRect = new RectF(0,0,realBmp.getWidth(), realBmp.getHeight());
		currentDisplayMatrix.mapRect(theRect);
		
		Log.v(LOGTAG,theRect.width() + " " + theRect.height());
		
		float deltaX = 0, deltaY = 0;
		if (theRect.width() < view.getWidth()) {
			deltaX = (view.getWidth() - theRect.width())/2 - theRect.left;
		} else if (theRect.left > 0) {
			deltaX = -theRect.left;
		} else if (theRect.right < view.getWidth()) {
			deltaX = view.getWidth() - theRect.right;
		}		
		
		if (theRect.height() < view.getHeight()) {
			deltaY = (view.getHeight() - theRect.height())/2 - theRect.top;
		} else if (theRect.top > 0) {
			deltaY = -theRect.top;
		} else if (theRect.bottom < view.getHeight()) {
			deltaY = view.getHeight() - theRect.bottom;
		}
		
		Log.v(LOGTAG,"Deltas:" + deltaX + " " + deltaY);
		
		matrix.postTranslate(deltaX,deltaY);
		view.setImageMatrix(matrix);
	}

	public void onClick(View v) {
		if (v == zoomIn) {
			float scale = 1.5f;
			
			PointF midpoint = new PointF(view.getWidth()/2, view.getHeight()/2);
			matrix.postScale(scale, scale, midpoint.x, midpoint.y);
			view.setImageMatrix(matrix);
			putOnScreen();
			
		} else if (v == zoomOut) {
			float scale = 0.75f;

			PointF midpoint = new PointF(view.getWidth()/2, view.getHeight()/2);
			matrix.postScale(scale, scale, midpoint.x, midpoint.y);
			view.setImageMatrix(matrix);
			putOnScreen();

		}
	}
}
