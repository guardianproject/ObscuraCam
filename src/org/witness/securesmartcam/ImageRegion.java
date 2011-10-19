package org.witness.securesmartcam;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.londatiga.android.QuickAction.OnActionItemClickListener;

import org.witness.securesmartcam.filters.BlurObscure;
import org.witness.securesmartcam.filters.CrowdPixelizeObscure;
import org.witness.securesmartcam.filters.MaskObscure;
import org.witness.securesmartcam.filters.PaintSquareObscure;
import org.witness.securesmartcam.filters.PixelizeObscure;
import org.witness.securesmartcam.filters.RegionProcesser;
import org.witness.sscphase1.ObscuraApp;
import org.witness.sscphase1.R;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

//public class ImageRegion extends FrameLayout implements OnTouchListener, OnActionItemClickListener {
public class ImageRegion implements OnActionItemClickListener 
{

	public static final String LOGTAG = "SSC.ImageRegion";
	
	// Rect for this when unscaled
	public RectF mBounds;
	public RectF mTmpBounds;
	
	// Start point for touch events
	PointF mStartPoint = null;

	// Our current mode
	public static final int NORMAL_MODE = 0;
	public static final int EDIT_MODE = 1;
	
// The current touch event mode
	public final static int NONE = 0;
	public final static int MOVE = 1;
	

	// What should be done to this region
	public static final int NOTHING = 0;
	public static final int OBSCURE = 1;
	
	public static final int REDACT = 0; // PaintSquareObscure
	public static final int PIXELATE = 1; // PixelizeObscure
	public static final int BG_PIXELATE = 2; // BlurObscure
	public static final int MASK = 3; // MaskObscure	
	//public static final int BLUR = 4; // PixelizeObscure
	public static final int CONSENT = 5; // PixelizeObscure
	
	boolean selected = false;
	
	/* Add each ObscureMethod to this list and update the 
	 * createObscuredBitmap method in ImageEditor
	 */
	int mObscureType = PIXELATE;

	private final static String[] mFilterLabels = {"Redact","Pixelate","CrowdPixel","Mask","Identify"};
	private final static int[] mFilterIcons = {R.drawable.ic_context_fill,R.drawable.ic_context_pixelate,R.drawable.ic_context_pixelate, R.drawable.ic_context_mask, R.drawable.ic_context_id};
	
	// The ImageEditor object that contains us
	ImageEditor mImageEditor;
	
	// Popup menu for region 
	// What's the license for this?
	QuickAction mPopupMenu;
		
	ActionItem[] mActionFilters;
	
	RegionProcesser mRProc;
	
	private final static float MIN_WIDTH = 10f;
	private final static float MIN_HEIGHT = 10f;
	
	
	public RegionProcesser getRegionProcessor() {
		return mRProc;
	}

	public void setRegionProcessor(RegionProcesser rProc) {
		mRProc = rProc;
	}

	
	/* For touch events, whether or not to show the menu
	 */
	boolean moved = false;
				
	Matrix mMatrix, iMatrix;

	int fingerCount = 0;
	
	public ImageRegion(
			ImageEditor imageEditor, 
			float left, float top, 
			float right, float bottom, Matrix matrix) 
	{
		//super(imageEditor);
		super();
		
		// Set the mImageEditor that this region belongs to to the one passed in
		mImageEditor = imageEditor;

		mMatrix = matrix;
		iMatrix = new Matrix();
    	mMatrix.invert(iMatrix);
		
		// Calculate the minMoveDistance using the screen density
		//float scale = this.getResources().getDisplayMetrics().density;
	//	minMoveDistance = minMoveDistanceDP * scale + 0.5f;
		
		
		mBounds = new RectF(left, top, right, bottom);	
		
				
		// Inflate Layout
		// imageregioninner is a FrameLayout
		/*
		LayoutInflater inflater = LayoutInflater.from(mImageEditor);        
		inflater.inflate(R.layout.imageregioninner, this, true);
		setBackgroundDrawable(mImageEditor.getResources().getDrawable(R.drawable.border));
		updateMatrix();

        mMoveRegion = (View)findViewById(R.id.MoveRegion);

        // Setting the onTouchListener for the moveRegion
        // Might also want to do this for the other views (corners)
        mMoveRegion.setOnTouchListener(this);
                
        initPopup();
        
        // This doesn't work with the touch listener always returning true.  
        // In some cases touch listener returns false and this gets triggered
       
        mMoveRegion.setOnClickListener(new OnClickListener (){

			// @Override
			public void onClick(View v)
			{
				Log.v(LOGTAG,"onClick");
				
				inflatePopup(false);
			}
			
		});*/
        
        //set default processor
        mRProc = new PixelizeObscure();
    }
	
	public void inflatePopup(boolean showDelayed) {

		if (mPopupMenu == null)
			initPopup();
		
		if (showDelayed) {
			// We need layout to pass again, let's wait a second or two
			new Handler() {
				@Override
				 public void handleMessage(Message msg) {
					 mPopupMenu.show(mImageEditor.getImageView());
			        }
			}.sendMessageDelayed(new Message(), 500);
		} else {			
			mPopupMenu.show(mImageEditor.getImageView());
		}

	}
	
	private void initPopup ()
	{
		mPopupMenu = new QuickAction(mImageEditor);
		
		/*
		editAction = new ActionItem();
		editAction.setTitle("Edit Tag");
		editAction.setIcon(this.getResources().getDrawable(R.drawable.ic_context_edit));
		
		qa.addActionItem(editAction);
		*/

		ActionItem aItem;
		
		for (int i = 0; i < mFilterLabels.length; i++)
		{
		
			aItem = new ActionItem();
			aItem.setTitle(mFilterLabels[i]);
			
			aItem.setIcon(mImageEditor.getResources().getDrawable(mFilterIcons[i]));			
			
			mPopupMenu.addActionItem(aItem);
		}
		

		aItem = new ActionItem();
		aItem.setTitle("Delete Tag");
		aItem.setIcon(mImageEditor.getResources().getDrawable(R.drawable.ic_context_delete));

		mPopupMenu.addActionItem(aItem);

		mPopupMenu.setOnActionItemClickListener(this);
	}
	
	void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	boolean isSelected ()
	{
		return selected;
	}
			
			
	private void updateBounds(float left, float top, float right, float bottom) 
	{
		Log.i(LOGTAG, "updateBounds: " + left + "," + top + "," + right + "," + bottom);
		mBounds.set(left, top, right, bottom);
		
		//updateLayout();
	}
	
	float scaleX, scaleY, leftOffset, topOffset;
	
	public void updateMatrix ()
	{
		float[] mValues = new float[9];
		mMatrix.getValues(mValues);		
    	mMatrix.invert(iMatrix);
		scaleX = mValues[Matrix.MSCALE_X];
		scaleY = mValues[Matrix.MSCALE_Y];
		
		leftOffset = mValues[Matrix.MTRANS_X];
		topOffset = mValues[Matrix.MTRANS_Y];
		
	}
	
	public RectF getBounds ()
	{
		return mBounds;
	}
	
	
	public boolean onTouch(View v, MotionEvent event) 
	{
		
		fingerCount = event.getPointerCount();
		Log.v(LOGTAG,"onTouch: fingers=" + fingerCount);
		
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			
			case MotionEvent.ACTION_DOWN:
				
				mImageEditor.doRealtimePreview = true;
				mImageEditor.updateDisplayImage();
				mTmpBounds = new RectF(mBounds);
				
				if (fingerCount == 1)
				{
					float[] points = {event.getX(), event.getY()};
                	
                	iMatrix.mapPoints(points);
					mStartPoint = new PointF(points[0],points[1]);
					Log.v(LOGTAG,"startPoint: " + mStartPoint.x + " " + mStartPoint.y);
				}
				
				moved = false;
				
				return false;
				
			case MotionEvent.ACTION_UP:

				mImageEditor.doRealtimePreview = true;
				mImageEditor.updateDisplayImage();
				mTmpBounds = null;

				return moved;
			
			case MotionEvent.ACTION_MOVE:
			
				
                if (fingerCount > 1)
                {
                	float[] points = {event.getX(0), event.getY(0), event.getX(1), event.getY(1)};                	
                	iMatrix.mapPoints(points);
                	
					mStartPoint = new PointF(points[0],points[1]);
					
                	RectF newBox = new RectF();
                	newBox.left = Math.min(points[0],points[2]);
                	newBox.top = Math.min(points[1],points[3]);
                	newBox.right = Math.max(points[0],points[2]);
                	newBox.bottom = Math.max(points[1],points[3]);
                	
                	if (newBox.width() < MIN_WIDTH)
                		newBox.right = newBox.left + MIN_WIDTH;
                	
                	if (newBox.height() < MIN_HEIGHT)
                		newBox.bottom = newBox.top + MIN_HEIGHT;
                	
                	moved = true;
                	
                	mTmpBounds = new RectF(newBox.left, newBox.top, newBox.right, newBox.bottom);                	                
                	updateBounds(newBox.left, newBox.top, newBox.right, newBox.bottom);
                	

                }
                else if (fingerCount == 1)
                {
                	
                	float[] points = {event.getX(), event.getY()};
                	
                	iMatrix.mapPoints(points);
                	
					PointF movePoint = new PointF(points[0],points[1]);
				
					float diffX = mStartPoint.x-movePoint.x;
					float diffY = mStartPoint.y-movePoint.y;
					
					if (diffX > MIN_WIDTH || diffY > MIN_HEIGHT)
					{
						moved = true;
						//updateBounds(movePoint.x-bW, movePoint.y-bH, movePoint.x+bW,movePoint.y+bH);
						updateBounds(mTmpBounds.left-diffX,mTmpBounds.top-diffY,mTmpBounds.right-diffX,mTmpBounds.bottom-diffY);
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

	
	@Override
	public void onItemClick(int pos) {
		
		if (pos == mFilterLabels.length) //meaing after the last one
		{
        	mImageEditor.deleteRegion(ImageRegion.this);
		}
		else
		{
        	if (mObscureType != pos)
        	{
        		mObscureType = pos;
        		updateRegionProcessor(mObscureType);
        	}
        		
		}

		mImageEditor.updateDisplayImage();

	}
	
	private void updateRegionProcessor (int obscureType)
	{
		switch (obscureType) {
		
		case ImageRegion.BG_PIXELATE:
			Log.v(ObscuraApp.TAG,"obscureType: BGPIXELIZE");
			mRProc = new CrowdPixelizeObscure();
		break;
		
		case ImageRegion.MASK:
			Log.v(ObscuraApp.TAG,"obscureType: ANON");
			mRProc = new MaskObscure(mImageEditor.getApplicationContext(), mImageEditor.getPainter());
			break;
			
		case ImageRegion.REDACT:
			Log.v(ObscuraApp.TAG,"obscureType: SOLID");
			mRProc = new PaintSquareObscure();
			break;
			
		case ImageRegion.PIXELATE:
			Log.v(ObscuraApp.TAG,"obscureType: PIXELIZE");
			mRProc = new PixelizeObscure();
			break;
			
		default:
			Log.v(ObscuraApp.TAG,"obscureType: NONE/BLUR");
			mRProc = new BlurObscure();
			break;
	}
	}

	
	
}
