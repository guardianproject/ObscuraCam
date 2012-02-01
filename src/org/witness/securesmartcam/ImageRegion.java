package org.witness.securesmartcam;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.londatiga.android.QuickAction.OnActionItemClickListener;

import org.witness.securesmartcam.filters.InformaTagger;
import org.witness.securesmartcam.filters.CrowdPixelizeObscure;
import org.witness.securesmartcam.filters.PixelizeObscure;
import org.witness.securesmartcam.filters.RegionProcesser;
import org.witness.securesmartcam.filters.SolidObscure;
import org.witness.sscphase1.ObscuraApp;
import org.witness.sscphase1.R;
import org.witness.securesmartcam.utils.ObscuraConstants;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class ImageRegion implements OnActionItemClickListener 
{

	public static final String LOGTAG = "SSC.ImageRegion";
	
	// Rect for this when unscaled
	public RectF mBounds;
	//public RectF mTmpBounds;
	
	// Start point for touch events
	PointF mStartPoint = null;
	PointF mNonMappedStartPoint = null;
	
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
	public static final int CONSENT = 4; // PixelizeObscure
	
	boolean selected = false;
	
	/* Add each ObscureMethod to this list and update the 
	 * createObscuredBitmap method in ImageEditor
	 */
	int mObscureType = PIXELATE;

	public final Drawable unidentifiedBorder, identifiedBorder;
	public Drawable imageRegionBorder;
	
	// The ImageEditor object that contains us
	ImageEditor mImageEditor;
	
	// Popup menu for region 
	// What's the license for this?
	QuickAction mPopupMenu;
		
	ActionItem[] mActionFilters;
	
	RegionProcesser mRProc;
	
	private final static float MIN_MOVE = 5f;

	private final static float CORNER_MAX = 50f;
	
	private int cornerMode = -1;
	
	public RegionProcesser getRegionProcessor() {
		return mRProc;
	}

	public void setRegionProcessor(RegionProcesser rProc) {
		mRProc = rProc;
	}
	
	
	
	public void setCornerMode (float x, float y)
	{
		float[] points = {x,y};        	
    	iMatrix.mapPoints(points);
    	
    	float cSize = CORNER_MAX;
    	
    	cSize = iMatrix.mapRadius(cSize);
    	
    	if (Math.abs(mBounds.left-points[0])<cSize
    			&& Math.abs(mBounds.top-points[1])<cSize
    			)
    	{
    		cornerMode = 1;
    		return;
    	}
    	else if (Math.abs(mBounds.left-points[0])<cSize
    			&& Math.abs(mBounds.bottom-points[1])<cSize
    			)
    	{
    		cornerMode = 2;
			return;
		}
    	else if (Math.abs(mBounds.right-points[0])<cSize
    			&& Math.abs(mBounds.top-points[1])<cSize
    			)
    	{
    			cornerMode = 3;
    			return;
		}
    	else if (Math.abs(mBounds.right-points[0])<cSize
        			&& Math.abs(mBounds.bottom-points[1])<cSize
        			)
    	{
    		cornerMode = 4;
    		return;
    	}
    	
    	cornerMode = -1;
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
		// set the borders for tags in Non-Edit mode
		identifiedBorder = imageEditor.getResources().getDrawable(R.drawable.border_idtag);
		unidentifiedBorder = imageEditor.getResources().getDrawable(R.drawable.border);

		mMatrix = matrix;
		iMatrix = new Matrix();
    	mMatrix.invert(iMatrix);
		
		// Calculate the minMoveDistance using the screen density
		//float scale = this.getResources().getDisplayMetrics().density;
	//	minMoveDistance = minMoveDistanceDP * scale + 0.5f;
		
		
		mBounds = new RectF(left, top, right, bottom);	
		
        
        //set default processor
        this.setRegionProcessor(new PixelizeObscure());
    }		
	
	public void setMatrix (Matrix matrix)
	{
		mMatrix = matrix;
		iMatrix = new Matrix();
    	mMatrix.invert(iMatrix);
	}
	
	public void inflatePopup(boolean showDelayed) {

		if (mPopupMenu == null)
			initPopup();
		
		
		if (showDelayed) {
			// We need layout to pass again, let's wait a second or two
			new Handler() {
				@Override
				 public void handleMessage(Message msg) {

					float[] points = {mBounds.centerX(), mBounds.centerY()};		
					mMatrix.mapPoints(points);
					mPopupMenu.show(mImageEditor.getImageView(), (int)points[0], (int)points[1]);
			        }
			}.sendMessageDelayed(new Message(), 500);
		} else {			

			float[] points = {mBounds.centerX(), mBounds.centerY()};		
			mMatrix.mapPoints(points);
			mPopupMenu.show(mImageEditor.getImageView(), (int)points[0], (int)points[1]);
		}
		

	}
	
	private void initPopup ()
	{
		mPopupMenu = new QuickAction(mImageEditor);

		ActionItem aItem;
		
		for (int i = 0; i < ObscuraConstants.mFilterLabels.length; i++)
		{
		
			aItem = new ActionItem();
			aItem.setTitle(ObscuraConstants.mFilterLabels[i]);
			
			aItem.setIcon(mImageEditor.getResources().getDrawable(ObscuraConstants.mFilterIcons[i]));			
			
			mPopupMenu.addActionItem(aItem);

		}
		
		aItem = new ActionItem();
		aItem.setTitle("Delete Tag");
		aItem.setIcon(mImageEditor.getResources().getDrawable(R.drawable.ic_context_delete));

		mPopupMenu.addActionItem(aItem);

		mPopupMenu.setOnActionItemClickListener(this);
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
	//	Log.v(LOGTAG,"onTouch: fingers=" + fingerCount);
		
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			
			case MotionEvent.ACTION_DOWN:
				
				mImageEditor.doRealtimePreview = true;
				mImageEditor.updateDisplayImage();
				//mTmpBounds = new RectF(mBounds);
				
				if (fingerCount == 1)
				{
					//float[] points = {event.getX(), event.getY()};                	
                	//iMatrix.mapPoints(points);
					//mStartPoint = new PointF(points[0],points[1]);
					mStartPoint = new PointF(event.getX(),event.getY());
					//Log.v(LOGTAG,"startPoint: " + mStartPoint.x + " " + mStartPoint.y);
				}
				
				moved = false;
				
				return false;
			case MotionEvent.ACTION_POINTER_UP:

				Log.v(LOGTAG, "second finger removed - pointer up!");

				return moved;
				
			case MotionEvent.ACTION_UP:

				mImageEditor.doRealtimePreview = true;
				mImageEditor.updateDisplayImage();
				//mTmpBounds = null;

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
	                	
	                	iMatrix.mapPoints(points);
	                	
	                	float diffX = points[0]-points[2];
	                	float diffY = points[1]-points[3];
	                	
	                	if (cornerMode == -1)
	                		updateBounds(mBounds.left-diffX,mBounds.top-diffY,mBounds.right-diffX,mBounds.bottom-diffY);
	                	else if (cornerMode == 1)
	                		updateBounds(mBounds.left-diffX,mBounds.top-diffY,mBounds.right,mBounds.bottom);
	                	else if (cornerMode == 2)
	                		updateBounds(mBounds.left-diffX,mBounds.top,mBounds.right,mBounds.bottom-diffY);
	                	else if (cornerMode == 3)
	                		updateBounds(mBounds.left,mBounds.top-diffY,mBounds.right-diffX,mBounds.bottom);
	                	else if (cornerMode == 4)
	                		updateBounds(mBounds.left,mBounds.top,mBounds.right-diffX,mBounds.bottom-diffY);
	                		
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

	
	@Override
	public void onItemClick(int pos) {
		
		if (pos == ObscuraConstants.mFilterLabels.length) //meaing after the last one
		{
        	mImageEditor.deleteRegion(ImageRegion.this);
		}
		else
		{
        	mObscureType = pos;
        	updateRegionProcessor(mObscureType);
        		
		}

		mImageEditor.updateDisplayImage();

	}
	
	private void updateRegionProcessor (int obscureType)
	{
		
		switch (obscureType) {
		
		case ImageRegion.BG_PIXELATE:
			Log.v(ObscuraConstants.TAG,"obscureType: BGPIXELIZE");
			setRegionProcessor(new CrowdPixelizeObscure());
		break;
			
		case ImageRegion.REDACT:
			Log.v(ObscuraConstants.TAG,"obscureType: SOLID");
			setRegionProcessor(new SolidObscure());
			break;
			
		case ImageRegion.PIXELATE:
			Log.v(ObscuraConstants.TAG,"obscureType: PIXELIZE");
			setRegionProcessor(new PixelizeObscure());
			break;
		case ImageRegion.CONSENT:
			Log.v(ObscuraConstants.TAG,"obscureType: CONSENTIFY!");
			// If the region processor is already a consent tagger, the user wants to edit.
			// so no need to change the region processor.
			if(!(getRegionProcessor() instanceof InformaTagger))
				setRegionProcessor(new InformaTagger());
			
			mImageEditor.launchInforma(this);
			break;
		default:
			Log.v(ObscuraConstants.TAG,"obscureType: NONE/PIXELIZE");
			setRegionProcessor(new PixelizeObscure());
			break;
		}
		
		if(getRegionProcessor().getClass() == InformaTagger.class)
			imageRegionBorder = identifiedBorder;
		else
			imageRegionBorder = unidentifiedBorder;
	}

	
	
}
