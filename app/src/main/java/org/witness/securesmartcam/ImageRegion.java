package org.witness.securesmartcam;

import android.content.res.Resources;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import org.witness.securesmartcam.filters.BlurObscure;
import org.witness.securesmartcam.filters.ConsentTagger;
import org.witness.securesmartcam.filters.CrowdPixelizeObscure;
import org.witness.securesmartcam.filters.MaskObscure;
import org.witness.securesmartcam.filters.PixelizeObscure;
import org.witness.securesmartcam.filters.RegionProcesser;
import org.witness.securesmartcam.filters.SolidObscure;
import org.witness.sscphase1.ObscuraApp;
import org.witness.sscphase1.R;

public class ImageRegion
{
	public static final String LOGTAG = "SSC.ImageRegion";
	
	// Rect for this when unscaled
	public RectF mBounds;
	
	// Start point for touch events
	PointF mStartPoint = null;
	
	public static final int REDACT = 0; // PaintSquareObscure
	public static final int PIXELATE = 1; // PixelizeObscure
	public static final int BG_PIXELATE = 2; // BlurObscure
	public static final int MASK = 3; // MaskObscure	
	public static final int CONSENT = 4; // PixelizeObscure
	public static final int BLUR = 5; // PixelizeObscure
	
	boolean selected = false;
	
	public static final int CORNER_UPPER_LEFT = 1;
	public static final int CORNER_LOWER_LEFT = 2;
	public static final int CORNER_UPPER_RIGHT = 3;
	public static final int CORNER_LOWER_RIGHT = 4;
	public static final int CORNER_LEFT = 5;
	public static final int CORNER_RIGHT = 6;
	public static final int CORNER_UPPER = 7;
	public static final int CORNER_LOWER = 8;
	public static final int CORNER_NONE = -1;

	/* Add each ObscureMethod to this list and update the 
	 * createObscuredBitmap method in ImageEditor
	 */
	int mObscureType = PIXELATE;

	public final Drawable unidentifiedBorder, identifiedBorder;
	public Drawable imageRegionBorder;
	
	// The ImageEditor object that contains us
	ImageEditor mImageEditor;
	RegionProcesser mRProc;
	
	private final static float MIN_MOVE = 5f;

	private final static float CORNER_MAX = 50f;
	private int handleTouchSlop;
	private int cornerMode = -1;
	
	public RegionProcesser getRegionProcessor() {
		return mRProc;
	}

	public void setRegionProcessor(RegionProcesser rProc) {
		mRProc = rProc;
	}
	
	public void setCornerMode (float x, float y)
	{
		float[] points = {x,y, mBounds.left, mBounds.top, mBounds.right, mBounds.bottom};
    	mImageEditor.getMatrixInverted().mapPoints(points);
		double radiusSquared = mImageEditor.getMatrixInverted().mapRadius(handleTouchSlop);
		radiusSquared = radiusSquared * radiusSquared;

//    	float cSize = CORNER_MAX;
//    	cSize = iMatrix.mapRadius(cSize);

		if (inLeftHandle(points[0], points[1], radiusSquared)) {
			cornerMode = CORNER_LEFT;
			return;
		} else if (inRightHandle(points[0], points[1], radiusSquared)) {
			cornerMode = CORNER_RIGHT;
			return;
		} else if (inTopHandle(points[0], points[1], radiusSquared)) {
			cornerMode = CORNER_UPPER;
			return;
		} else if (inBottomHandle(points[0], points[1], radiusSquared)) {
			cornerMode = CORNER_LOWER;
			return;
		}
//		else if (Math.abs(mBounds.left-points[0])<cSize
//    			&& Math.abs(mBounds.top-points[1])<cSize
//    			)
//    	{
//    		cornerMode = CORNER_UPPER_LEFT;
//    		return;
//    	}
//    	else if (Math.abs(mBounds.left-points[0])<cSize
//    			&& Math.abs(mBounds.bottom-points[1])<cSize
//    			)
//    	{
//    		cornerMode = CORNER_LOWER_LEFT;
//			return;
//		}
//    	else if (Math.abs(mBounds.right-points[0])<cSize
//    			&& Math.abs(mBounds.top-points[1])<cSize
//    			)
//    	{
//    			cornerMode = CORNER_UPPER_RIGHT;
//    			return;
//		}
//    	else if (Math.abs(mBounds.right-points[0])<cSize
//        			&& Math.abs(mBounds.bottom-points[1])<cSize
//        			)
//    	{
//    		cornerMode = CORNER_LOWER_RIGHT;
//    		return;
//    	}
//
    	cornerMode = CORNER_NONE;
	}

	private boolean inLeftHandle(float x, float y, double radiusSquared) {
		float midY = mBounds.top + (mBounds.bottom - mBounds.top) / 2;
		double dx = Math.pow(mBounds.left - x, 2);
		double dy = Math.pow(midY - y, 2);
		return ((dx + dy) < radiusSquared);
	}

	private boolean inRightHandle(float x, float y, double radiusSquared) {
		float midY = mBounds.top + (mBounds.bottom - mBounds.top) / 2;
		double dx = Math.pow(mBounds.right - x, 2);
		double dy = Math.pow(midY - y, 2);
		return ((dx + dy) < radiusSquared);
	}

	private boolean inTopHandle(float x, float y, double radiusSquared) {
		float midX = mBounds.left + (mBounds.right - mBounds.left) / 2;
		double dx = Math.pow(midX - x, 2);
		double dy = Math.pow(mBounds.top - y, 2);
		return ((dx + dy) < radiusSquared);
	}

	private boolean inBottomHandle(float x, float y, double radiusSquared) {
		float midX = mBounds.left + (mBounds.right - mBounds.left) / 2;
		double dx = Math.pow(midX - x, 2);
		double dy = Math.pow(mBounds.bottom - y, 2);
		return ((dx + dy) < radiusSquared);
	}

	public boolean containsPoint(float x, float y) {
		float[] points = {x,y};
		mImageEditor.getMatrixInverted().mapPoints(points);
		double radiusSquared = mImageEditor.getMatrixInverted().mapRadius(handleTouchSlop);
		radiusSquared = radiusSquared * radiusSquared;
		x = points[0];
		y = points[1];
		return mBounds.contains(x, y) || inLeftHandle(x, y, radiusSquared) || inRightHandle(x, y, radiusSquared) || inTopHandle(x, y, radiusSquared) || inBottomHandle(x, y, radiusSquared);
	}

	
	/* For touch events, whether or not to show the menu
	 */
	boolean moved = false;

	int fingerCount = 0;
	
	public ImageRegion(
			ImageEditor imageEditor, 
			float left, float top, 
			float right, float bottom, Matrix matrix) 
	{
		super();

		// Handle
		Resources r = imageEditor.getResources();
		handleTouchSlop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ImageEditor.SELECTION_HANDLE_TOUCH_RADIUS, r.getDisplayMetrics());

		// Set the mImageEditor that this region belongs to to the one passed in
		mImageEditor = imageEditor;
		// set the borders for tags in Non-Edit mode
		identifiedBorder = imageEditor.getResources().getDrawable(R.drawable.border_idtag);
		unidentifiedBorder = imageEditor.getResources().getDrawable(R.drawable.border);

		mBounds = new RectF(left, top, right, bottom);	

        //set default processor
        this.setRegionProcessor(new PixelizeObscure());
    }		

	boolean isSelected ()
	{
		return selected;
	}
	
	void setSelected (boolean _selected)
	{
		selected = _selected;
	}
			
			
	private void updateBounds(float left, float top, float right, float bottom) 
	{
		//Log.i(LOGTAG, "updateBounds: " + left + "," + top + "," + right + "," + bottom);
		mBounds.set(left, top, right, bottom);
		
		//updateLayout();
	}

	public RectF getBounds ()
	{
		return mBounds;
	}
	
	
	public boolean onTouch(View v, MotionEvent event) 
	{
		
		fingerCount = event.getPointerCount();
	//	Log.v(LOGTAG,"onTouch: fingers=" + fingerCount);
		
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			
			case MotionEvent.ACTION_DOWN:
				moved = false;
				mStartPoint = new PointF(event.getX(),event.getY());
				return true;
			case MotionEvent.ACTION_POINTER_UP:

				Log.v(LOGTAG, "second finger removed - pointer up!");

				return moved;
				
			case MotionEvent.ACTION_UP:
				mImageEditor.setRealtimePreview(true);
				mImageEditor.forceUpdateDisplayImage();
				//mTmpBounds = null;

				return moved;
			
			case MotionEvent.ACTION_MOVE:
			
				
                if (fingerCount > 1)
                {
                	float[] points = {event.getX(0), event.getY(0), event.getX(1), event.getY(1)};                	
                	mImageEditor.getMatrixInverted().mapPoints(points);
                	
					mStartPoint = new PointF(points[0],points[1]);
					
                	RectF newBox = new RectF();
                	newBox.left = Math.min(points[0],points[2]);
                	newBox.top = Math.min(points[1],points[3]);
                	newBox.right = Math.max(points[0],points[2]);
                	newBox.bottom = Math.max(points[1],points[3]);
                	
                	moved = true;
                	
                	if (newBox.left != newBox.right && newBox.top != newBox.bottom)
                	{
                	                
                		updateBounds(newBox.left, newBox.top, newBox.right, newBox.bottom);
                	}
                	

                }
                else if (fingerCount == 1)
                {
                	
                	
                	if (Math.abs(mStartPoint.x- event.getX()) > MIN_MOVE)
                	{
	                	moved = true;
	                	
	                	float[] points = {mStartPoint.x, mStartPoint.y, event.getX(), event.getY()};
	                	
	                	mImageEditor.getMatrixInverted().mapPoints(points);
	                	
	                	float diffX = points[0]-points[2];
	                	float diffY = points[1]-points[3];
	                	
	                	float left = 0, top = 0, right = 0, bottom = 0;
	                	
	                	if (cornerMode == CORNER_NONE)
	                	{
	                		left = mBounds.left-diffX;
	                		top = mBounds.top-diffY;
	                		right = mBounds.right-diffX;
	                		bottom = mBounds.bottom-diffY;
	                	} else {
							if (cornerMode == CORNER_LEFT || cornerMode == CORNER_UPPER_LEFT || cornerMode == CORNER_LOWER_LEFT) {
								left = mBounds.left - diffX;
							} else {
								left = mBounds.left;
							}

							if (cornerMode == CORNER_RIGHT || cornerMode == CORNER_UPPER_RIGHT || cornerMode == CORNER_LOWER_RIGHT) {
								right = mBounds.right - diffX;
							} else {
								right = mBounds.right;
							}

							if (cornerMode == CORNER_UPPER || cornerMode == CORNER_UPPER_LEFT || cornerMode == CORNER_UPPER_RIGHT) {
								top = mBounds.top-diffY;
							} else {
								top = mBounds.top;
							}

							if (cornerMode == CORNER_LOWER || cornerMode == CORNER_LOWER_LEFT || cornerMode == CORNER_LOWER_RIGHT) {
								bottom = mBounds.bottom-diffY;
							} else {
								bottom = mBounds.bottom;
							}
						}

                		if ((left+CORNER_MAX) > right || (top+CORNER_MAX) > bottom)
                			return false;
                		
	                	
	                	//updateBounds(Math.min(left, right), Math.min(top,bottom), Math.max(left, right), Math.max(top, bottom));
	                	updateBounds(left, top, right, bottom);
	                	
	                	mStartPoint = new PointF(event.getX(),event.getY());
                	}
                	else
                	{
                		moved = false;
                	}
	            	
                }

				mImageEditor.updateDisplayImage();
					
				return true;
			/*	
			case MotionEvent.ACTION_OUTSIDE:
				Log.v(LOGTAG,"ACTION_OUTSIDE");
				
				mImageEditor.doRealtimePreview = true;
				mImageEditor.updateDisplayImage();
				

				return true;
				
			case MotionEvent.ACTION_CANCEL:
				Log.v(LOGTAG,"ACTION_CANCEL");
				
				mImageEditor.doRealtimePreview = true;
				mImageEditor.updateDisplayImage();
				
				return true;
				
			default:
				Log.v(LOGTAG, "DEFAULT: " + (event.getAction() & MotionEvent.ACTION_MASK));
		
				mImageEditor.doRealtimePreview = true;
				mImageEditor.updateDisplayImage();
				
				return true;*/
				
		}
		
		return false;
		
	}

	public void setObscureType(int obscureType) {
		mObscureType = obscureType;
		updateRegionProcessor(obscureType);
	}

	private void updateRegionProcessor (int obscureType)
	{
		switch (obscureType) {
		
		case ImageRegion.BG_PIXELATE:
			Log.v(ObscuraApp.TAG,"obscureType: BGPIXELIZE");
			setRegionProcessor(new CrowdPixelizeObscure());
		break;
		
		case ImageRegion.MASK:
			Log.v(ObscuraApp.TAG,"obscureType: ANON");
			setRegionProcessor(new MaskObscure(mImageEditor.getApplicationContext(), mImageEditor.getPainter()));

			break;
			
		case ImageRegion.REDACT:
			Log.v(ObscuraApp.TAG,"obscureType: SOLID");
			setRegionProcessor(new SolidObscure());
			break;
			
		case ImageRegion.PIXELATE:
			Log.v(ObscuraApp.TAG,"obscureType: PIXELIZE");
			setRegionProcessor(new PixelizeObscure());
			break;
		case ImageRegion.BLUR:
			Log.v(ObscuraApp.TAG,"obscureType: NONE/BLUR");
			setRegionProcessor(new BlurObscure(mImageEditor.getPainter()));
			break;
		}
		
		if(getRegionProcessor().getClass() == ConsentTagger.class)
			imageRegionBorder = identifiedBorder;
		else
			imageRegionBorder = unidentifiedBorder;
	}
}
