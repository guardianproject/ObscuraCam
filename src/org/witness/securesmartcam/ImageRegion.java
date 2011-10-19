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
public class ImageRegion {

	public static final String LOGTAG = "SSC.ImageRegion";
	
	// Rect for this when unscaled
	public RectF mBounds;
		
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
	
	
	int mTouchMode = EDIT_MODE;
	
	private View mMoveRegion;
	
	int mEditMode = NONE;
	
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
	
	public RegionProcesser getRegionProcessor() {
		return mRProc;
	}

	public void setRegionProcessor(RegionProcesser rProc) {
		mRProc = rProc;
	}

	
	/* For touch events, whether or not to show the menu
	 */
	boolean mShowMenu = false;
				
	Matrix mMatrix;
	
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

		//mPopupMenu.setOnActionItemClickListener(this);
	}
	
	void toggleMode() {
		// Put this here as we don't want the massive recursion that would happen in changeMode
		mImageEditor.clearImageRegionsEditMode();

		if (mTouchMode == EDIT_MODE) {
			changeMode(NORMAL_MODE);
		} else if (mTouchMode == NORMAL_MODE) {
			changeMode(EDIT_MODE);
		}
	}
			
	public void changeMode(int newMode) 
	{
		mTouchMode = newMode;
		if (mTouchMode == EDIT_MODE) {

			//setBackgroundDrawable(mImageEditor.getResources().getDrawable(R.drawable.bordergreen));
		
		} else if (mTouchMode == NORMAL_MODE) {
			
			//setBackgroundDrawable(mImageEditor.getResources().getDrawable(R.drawable.border));
		
		}
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
		scaleX = mValues[Matrix.MSCALE_X];
		scaleY = mValues[Matrix.MSCALE_Y];
		
		leftOffset = mValues[Matrix.MTRANS_X];
		topOffset = mValues[Matrix.MTRANS_Y];
		
		//updateLayout();
	}
	
	/*
	private void updateLayout ()
	{
		
		RectF lBounds = new RectF();
		lBounds.left = (mBounds.left * scaleX) + leftOffset;
		lBounds.top = (mBounds.top * scaleY) + topOffset;
		lBounds.right = (mBounds.right * scaleX) + leftOffset;
		lBounds.bottom = (mBounds.bottom * scaleY) + topOffset;
		
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)getLayoutParams();
		
		if (lp == null)
		{
			lp = new RelativeLayout.LayoutParams((int)lBounds.width(), (int)lBounds.height());
		}
		
		lp.leftMargin = (int)lBounds.left;
		lp.topMargin = (int)lBounds.top;
		lp.width = (int)lBounds.width();
		lp.height = (int)lBounds.height();
		
		setLayoutParams(lp);
	}*/
	
	
	
	public RectF getBounds ()
	{
		return mBounds;
	}
	
	int fingerCount = 0;
	
	public boolean onTouch(View v, MotionEvent event) 
	{
		
		fingerCount = event.getPointerCount();
		Log.v(LOGTAG,"onTouch: fingers=" + fingerCount);
		
		if (mTouchMode == NORMAL_MODE)
		{
			changeMode(EDIT_MODE);
			
			return true;
		}
		else if (mTouchMode == EDIT_MODE) 
		{

			switch (event.getAction() & MotionEvent.ACTION_MASK) {
				
				case MotionEvent.ACTION_DOWN:
					
					mImageEditor.doRealtimePreview = true;
					mImageEditor.updateDisplayImage();
					
					if (fingerCount == 1)
					{
						mStartPoint = new PointF(event.getX(),event.getY());
						Log.v(LOGTAG,"startPoint: " + mStartPoint.x + " " + mStartPoint.y);
					}
					
					mEditMode = MOVE;
					
					mShowMenu = true;
					return false;
					
				case MotionEvent.ACTION_UP:

					
					mImageEditor.doRealtimePreview = true;
					mImageEditor.updateDisplayImage();
					mStartPoint = null;
					
					mEditMode = NONE;
					if (mShowMenu) {
						mShowMenu = false;

						// Treat like a click
						return false;
					}
					return true;
				
				case MotionEvent.ACTION_MOVE:
				
					
	                if (fingerCount > 1)
	                {
	                	
	                	float x1 = event.getX(0);
	                	float x2 = event.getX(1);
	                	float y1 = event.getY(0);
	                	float y2 = event.getY(1);
	                	
	                	RectF newBox = new RectF();
	                	newBox.left = Math.min(x1, x2 );
	                	newBox.top = Math.min(y1, y2 );
	                	newBox.right = Math.max(x1, x2);
	                	newBox.bottom = Math.max(y1, y2);
	                	
	        			updateBounds(newBox.left, newBox.top, newBox.right, newBox.bottom);

	                }
	                else if (fingerCount == 1)
	                {
	                	
						PointF movePoint = new PointF(event.getX(),event.getY());
						float bW = mBounds.width()/2f;
						float bH = mBounds.height()/2f;
	                	
	                	updateBounds(movePoint.x-bW, movePoint.y-bH, movePoint.x+bW,movePoint.y+bH);
	                	
		            	
	                }

					mImageEditor.updateDisplayImage();
						
					return true;
					
				case MotionEvent.ACTION_OUTSIDE:
					Log.v(LOGTAG,"ACTION_OUTSIDE");
					
					mImageEditor.doRealtimePreview = true;
					mImageEditor.updateDisplayImage();
					
					mEditMode = NONE;
					mShowMenu = false;
					

					return true;
					
				case MotionEvent.ACTION_CANCEL:
					Log.v(LOGTAG,"ACTION_CANCEL");
					
					mImageEditor.doRealtimePreview = true;
					mImageEditor.updateDisplayImage();
					
					mEditMode = NONE;
					mShowMenu = false;
					return true;
					
				default:
					Log.v(LOGTAG, "DEFAULT: " + (event.getAction() & MotionEvent.ACTION_MASK));
					mEditMode = NONE;
					
					mImageEditor.doRealtimePreview = true;
					mImageEditor.updateDisplayImage();
					
					mShowMenu = false;
					return true;
			}
		}
		return true;
	}

	/*
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

	}	*/
	
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
