package org.witness.sscphase1;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class ImageRegion extends FrameLayout implements OnTouchListener {

	public static final String LOGTAG = "[Camera Obscura : ImageRegion]";

	// The unscaled coordinates
	float startX;
	float startY;
	float endX;
	float endY;
	
	// Start point for touch events
	PointF startPoint = new PointF();

	// The unscaled image dimensions that this ImageRegion is on
	int imageWidth;
	int imageHeight;
	
	// The distance around a corner which will still represent a corner touch event
	public static final int CORNER_TOUCH_TOLERANCE = 35;
		
	// Our current mode
	public static final int NORMAL_MODE = 0;
	public static final int EDIT_MODE = 1;
	int mode = EDIT_MODE;
	
	// The current touch event mode
	public final static int NONE = 0;
	public final static int MOVE = 1;
	public final static int TOP_LEFT = 2;
	public final static int BOTTOM_LEFT = 3;
	public final static int TOP_RIGHT = 4;
	public final static int BOTTOM_RIGHT = 5;
	int whichEditMode = NONE;
	
	// What should be done to this region
	public static final int NOTHING = 0;
	public static final int OBSCURE = 1;
	int whatToDo = NOTHING;

	/*
	 * Add each ObscureMethod to this list and update the 
	 * createObscuredBitmap method in ImageEditor
	 */
	public static final int BLUR = 0; // BlurObscure
	public static final int ANON = 1; // AnonObscure
	public static final int SOLID = 2; // PaintSquareObscure
	public static final int PIXELIZE = 3; // PixelizeObscure
	int obscureType = BLUR;

	// The ImageEditor object that contains us
	ImageEditor imageEditor;
	
	// Popup menu for region 
	// What's the license for this?
	QuickAction qa;
	
	// Rect for this when scaled
	Rect scaledRect;
	
	/*
	 * Views for region, handles for scaling, moving
	 */
	View topLeftCorner;
	View topRightCorner;
	View bottomLeftCorner;
	View bottomRightCorner;
	View moveRegion;
	
	/*
	 * ActionItems for pop-up
	 */
	ActionItem editAction;
	ActionItem idAction;
	ActionItem encryptAction;
	ActionItem destroyAction;
	ActionItem removeRegionAction;
	
	/*
	 * minMoveDistance to determine if we should count this as a move or not
	 * minMoveDistance is calculated later based on screen density
	 */
	float minMoveDistanceDP = 2f;
	float minMoveDistance;	

	/*
	 * For touch events, whether or not to show the menu
	 */
	boolean doMenu = false;

				
	public ImageRegion(
			ImageEditor _imageEditor, 
			int _scaledStartX, int _scaledStartY, 
			int _scaledEndX, int _scaledEndY, 
			int _scaledImageWidth, int _scaledImageHeight, 
			int _imageWidth, int _imageHeight, 
			int _backgroundColor) 
	{
		super(_imageEditor);
		
		// Set the imageEditor that this region belongs to to the one passed in
		imageEditor = _imageEditor;

		// Calculate the minMoveDistance using the screen density
		float scale = this.getResources().getDisplayMetrics().density;
		minMoveDistance = minMoveDistanceDP * scale + 0.5f;
		
		// Set the unscaled image coordinates
		startX = (float)_imageWidth/(float)_scaledImageWidth * (float)_scaledStartX;
		startY = (float)_imageHeight/(float)_scaledImageHeight * (float)_scaledStartY;
		endX = (float)_imageWidth/(float)_scaledImageWidth * (float)_scaledEndX;
		endY = (float)_imageHeight/(float)_scaledImageHeight * (float)_scaledEndY;
		
		Log.v(LOGTAG,"startX: " + startX);
		Log.v(LOGTAG,"startY: " + startY);
		Log.v(LOGTAG,"endX: " + endX);
		Log.v(LOGTAG,"endY: " + endY);
				
		// Set the image width and height (these are unscaled)
		imageWidth = _imageWidth;
		imageHeight = _imageHeight;
		
		// Set the background color, this is based on the type of region it is,
		// probably should be self determined rather than passed in
		setBackgroundColor(_backgroundColor);
		
		// This preps the QuickAction menu 
		inflatePopup();
		
		// Inflate Layout
		// imageregioninner is a FrameLayout
		LayoutInflater inflater = LayoutInflater.from(imageEditor);        
		inflater.inflate(R.layout.imageregioninner, this, true);
		
		// Views for elements within the imageregion
        topLeftCorner = findViewById(R.id.TopLeftCorner);
        topRightCorner = findViewById(R.id.TopRightCorner);
        bottomLeftCorner = findViewById(R.id.BottomLeftCorner);
        bottomRightCorner = findViewById(R.id.BottomRightCorner);
        moveRegion = findViewById(R.id.MoveRegion);
                
        // Setting the onTouchListener for the moveRegion
        // Might also want to do this for the other views (corners)
        moveRegion.setOnTouchListener(this);
                
        // This doesn't work with the touch listener always returning true.  
        // In some cases touch listener returns false and this gets triggered
        moveRegion.setOnClickListener(new OnClickListener (){

			// @Override
			public void onClick(View v)
			{
				Log.v(LOGTAG,"onClick");
				
				inflatePopup();
				qa.show();
			}
			
		});
	}
			
	void inflatePopup() {
		
			qa = new QuickAction(this);
			
			editAction = new ActionItem();
			editAction.setTitle("Edit");
			editAction.setIcon(this.getResources().getDrawable(R.drawable.ic_context_edit));
			editAction.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					qa.dismiss();
					ImageRegion.this.changeMode(EDIT_MODE);
				}
			});
			qa.addActionItem(editAction);
				
			destroyAction = new ActionItem();
			destroyAction.setTitle("Obscure");
			destroyAction.setIcon(this.getResources().getDrawable(R.drawable.ic_context_destroy));
			destroyAction.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					qa.dismiss();
					whatToDo = OBSCURE;
				}
			});
			qa.addActionItem(destroyAction);
			
			removeRegionAction = new ActionItem();
			removeRegionAction.setTitle("Remove Region");
			removeRegionAction.setIcon(this.getResources().getDrawable(R.drawable.ic_context_destroy));
			removeRegionAction.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					qa.dismiss();
	            	imageEditor.deleteRegion(ImageRegion.this);
				}
			});
			qa.addActionItem(removeRegionAction);
	}
	
	public void changeMode(int newMode) {
		
		if (newMode == EDIT_MODE && mode == EDIT_MODE)
		{
			changeMode(NORMAL_MODE);
		} 
		else 
		{
			mode = newMode;
			if (mode == EDIT_MODE) {
				topLeftCorner.setVisibility(View.VISIBLE);
				topRightCorner.setVisibility(View.VISIBLE);
				bottomLeftCorner.setVisibility(View.VISIBLE);
				bottomRightCorner.setVisibility(View.VISIBLE);
			} else if (mode == NORMAL_MODE) {
				topLeftCorner.setVisibility(View.GONE);
				topRightCorner.setVisibility(View.GONE);
				bottomLeftCorner.setVisibility(View.GONE);
				bottomRightCorner.setVisibility(View.GONE);
			}
		}
	}
	
	public Rect getScaledRect(int _scaledImageWidth, int _scaledImageHeight) {

		float scaledStartX = (float)startX * (float)_scaledImageWidth/(float)imageWidth;
		float scaledStartY = (float)startY * (float)_scaledImageHeight/(float)imageHeight;
		float scaledEndX = (float)endX * (float)_scaledImageWidth/(float)imageWidth;
		float scaledEndY = (float)endY * (float)_scaledImageHeight/(float)imageHeight;
		
		return new Rect((int)scaledStartX, (int)scaledStartY, (int)scaledEndX, (int)scaledEndY);
	}
	
	
	public boolean onTouch(View v, MotionEvent event) {

		Log.v(LOGTAG,"onTouch");
		
		if (mode == NORMAL_MODE)
		{
			// Just a click, return false
			return false;
		}
		else if (mode == EDIT_MODE) {
			Log.v(LOGTAG,"onTouch mode EDIT");

			switch (event.getAction() & MotionEvent.ACTION_MASK) {
				
				case MotionEvent.ACTION_DOWN:
					
					Log.v(LOGTAG,"ACTION_DOWN");
					
					scaledRect = getScaledRect((int)imageEditor.getScaleOfImage().width(), (int)imageEditor.getScaleOfImage().height());					
					
					startPoint = new PointF(event.getX(),event.getY());
					Log.v(LOGTAG,"startPoint: " + startPoint.x + " " + startPoint.y);
										
					if (v == topLeftCorner || (
							event.getX() < CORNER_TOUCH_TOLERANCE &&
							event.getY() < CORNER_TOUCH_TOLERANCE
							)) {
						whichEditMode = TOP_LEFT;
						Log.v(LOGTAG,"TOP_LEFT");
						
					} else if (v == topRightCorner || (
							event.getX() > this.getWidth() - CORNER_TOUCH_TOLERANCE &&
							event.getY() < CORNER_TOUCH_TOLERANCE
					)) {
						whichEditMode = TOP_RIGHT;
						Log.v(LOGTAG,"TOP_RIGHT");
						
					} else if (v == bottomLeftCorner || (
							event.getX() < CORNER_TOUCH_TOLERANCE &&
							event.getY() > this.getHeight() - CORNER_TOUCH_TOLERANCE
					)) {
						whichEditMode = BOTTOM_LEFT;
						Log.v(LOGTAG,"BOTTOM_LEFT");
						
					} else if (v == bottomRightCorner || (
							event.getX() > this.getWidth() - CORNER_TOUCH_TOLERANCE &&
							event.getY() > this.getHeight() - CORNER_TOUCH_TOLERANCE
					)) {
						whichEditMode = BOTTOM_RIGHT;
						Log.v(LOGTAG,"BOTTOM_RIGHT");
						
					} else if (v == moveRegion || (
							event.getX() < this.getWidth() - CORNER_TOUCH_TOLERANCE &&
							event.getX() > CORNER_TOUCH_TOLERANCE &&
							event.getY() < this.getHeight() - CORNER_TOUCH_TOLERANCE &&
							event.getY() > CORNER_TOUCH_TOLERANCE						
					)) {
						whichEditMode = MOVE;
						Log.v(LOGTAG,"MOVE");
						
					} else {
						whichEditMode = NONE;
						Log.v(LOGTAG,"NONE");
						
					}
					
					doMenu = true;
					return false;
				
				case MotionEvent.ACTION_UP:
					Log.v(LOGTAG,"ACTION_UP");

					whichEditMode = NONE;
					if (doMenu) {
						Log.v(LOGTAG,"doMenu");
						doMenu = false;

						// Treat like a click
						return false;
					}
					Log.v(LOGTAG,"don't doMenu");
					return true;
				
				case MotionEvent.ACTION_MOVE:
					Log.v(LOGTAG,"ACTION MOVE");
				
					float distance = (float) (Math.sqrt(Math.abs(startPoint.x - event.getX()) + Math.abs(startPoint.y - event.getY())));
					Log.v(LOGTAG,"Move Distance: " + distance);
					Log.v(LOGTAG,"Min Distance: " + minMoveDistance);
					
					if (distance > minMoveDistance) {
						doMenu = false;
					
						if (scaledRect == null)
							break;
					
						Log.v(LOGTAG,"BEFORE MOVE: Left: " + scaledRect.left + " Right: " + scaledRect.right + 
								" Top: " + scaledRect.top + " Bottom: " + scaledRect.bottom);
						
						float xdist = startPoint.x - event.getX();
						float ydist = startPoint.y - event.getY();
						
						Log.v(LOGTAG,"event.getX(): " + event.getX());
						Log.v(LOGTAG,"event.getY(): " + event.getY());
						Log.v(LOGTAG,"xdist: " + xdist);
						Log.v(LOGTAG,"ydist: " + ydist);
												
						if (whichEditMode == TOP_LEFT) {
							// Here we expand
							Log.v(LOGTAG,"TOP LEFT CORNER");
							scaledRect.left = scaledRect.left - (int)xdist;
							scaledRect.top = scaledRect.top - (int)ydist;
							
					    	// Reset start point
							startPoint = new PointF(event.getX(),event.getY());
						
						} else if (whichEditMode == TOP_RIGHT) {
							// Here we expand
							Log.v(LOGTAG,"TOP RIGHT CORNER");
							scaledRect.top = scaledRect.top - (int)ydist;
							scaledRect.right = scaledRect.right - (int)xdist;
							
					    	// Reset start point
							startPoint = new PointF(event.getX(),event.getY());

						} else if (whichEditMode == BOTTOM_LEFT) {
							// Here we expand
							Log.v(LOGTAG,"BOTTOM LEFT CORNER");
							scaledRect.left = scaledRect.left - (int)xdist;
							scaledRect.bottom = scaledRect.bottom - (int)ydist;			
							
					    	// Reset start point
							startPoint = new PointF(event.getX(),event.getY());

						} else if (whichEditMode == BOTTOM_RIGHT) {
							// Here we expand
							/*
							Log.v(LOGTAG,"BOTTOM RIGHT CORNER");
							Log.v(LOGTAG, "scaledRect.right - xdist:" + scaledRect.right + " " + (int)xdist);
							Log.v(LOGTAG, "scaledRect.bottom - ydist" + scaledRect.bottom + " " + (int)ydist);
							*/
							scaledRect.right = scaledRect.right - (int)xdist;
							scaledRect.bottom = scaledRect.bottom - (int)ydist;
							
					    	// Reset start point
							startPoint = new PointF(event.getX(),event.getY());
							
						} else if (whichEditMode == MOVE) {
							Log.v(LOGTAG,"MOVE REGION " + xdist + " " + ydist);
							scaledRect.left = scaledRect.left - (int)xdist;
							scaledRect.top = scaledRect.top - (int)ydist;
							scaledRect.right = scaledRect.right - (int)xdist;
							scaledRect.bottom = scaledRect.bottom - (int)ydist;
						
						}

						Log.v(LOGTAG,"AFTER MOVE: Left: " + scaledRect.left + " Right: " + scaledRect.right + 
								" Top: " + scaledRect.top + " Bottom: " + scaledRect.bottom);
						
						// Update unscaled variables
						startX = (float)scaledRect.left * (float)imageWidth/(float)imageEditor.getScaleOfImage().width();
						startY = (float)scaledRect.top * (float)imageHeight/(float)imageEditor.getScaleOfImage().height();
						endX = (float)scaledRect.right * (float)imageWidth/(float)imageEditor.getScaleOfImage().width();
						endY = (float)scaledRect.bottom * (float)imageHeight/(float)imageEditor.getScaleOfImage().height();
						
						// Update LayoutParams
						RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) this.getLayoutParams();
				    	lp.leftMargin = (int)scaledRect.left;
				    	lp.topMargin = (int)scaledRect.top;
				    	lp.width = scaledRect.width();
				    	lp.height = scaledRect.height();
				    	this.setLayoutParams(lp);
				    	
				    	return true;
					}	
					return false;
					
				case MotionEvent.ACTION_OUTSIDE:
					Log.v(LOGTAG,"ACTION_OUTSIDE");
					whichEditMode = NONE;
					doMenu = false;
					return true;
					
				case MotionEvent.ACTION_CANCEL:
					Log.v(LOGTAG,"ACTION_CANCEL");
					whichEditMode = NONE;
					doMenu = false;
					return true;
					
				default:
					Log.v(LOGTAG, "DEFAULT: " + (event.getAction() & MotionEvent.ACTION_MASK));
					whichEditMode = NONE;
					doMenu = false;
					return true;
			}
		}
		return true;
	}	
}
