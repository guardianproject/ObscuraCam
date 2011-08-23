package org.witness.sscphase1;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class ImageRegion extends FrameLayout implements OnTouchListener {

	public static final String LOGTAG = "[Camera Obscura ImageRegion]";
	
	// Rect for this when unscaled
	public RectF unscaledRect;
	
	// Rect for this when scaled
	public RectF scaledRect;
		
	// Start point for touch events
	PointF startPoint = new PointF();

	// The unscaled image dimensions that this ImageRegion is on
	int imageWidth;
	int imageHeight;
	
	// The distance around a corner which will still represent a corner touch event
	public static final int CORNER_TOUCH_TOLERANCE = 50;
		
	// Our current mode
	public static final int NORMAL_MODE = 0;
	public static final int EDIT_MODE = 1;
	int mode = EDIT_MODE;
	
	//other values
	public int backgroundColor;
	
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
	int whatToDo = OBSCURE;

	/*
	 * Add each ObscureMethod to this list and update the 
	 * createObscuredBitmap method in ImageEditor
	 */
	public static final int BG_PIXELIZE = 0; // BlurObscure
	public static final int ANON = 1; // MaskObscure
	public static final int SOLID = 2; // PaintSquareObscure
	public static final int PIXELIZE = 3; // PixelizeObscure
	int obscureType = PIXELIZE;

	// The ImageEditor object that contains us
	ImageEditor imageEditor;
	
	// Popup menu for region 
	// What's the license for this?
	QuickAction qa;
		
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
	
	ActionItem blurObscureAction;
	ActionItem anonObscureAction;
	ActionItem solidObscureAction;
	ActionItem pixelizeObscureAction;
	
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
		
		// Set the image width and height (these are unscaled, not original on disk)
		imageWidth = _imageWidth;
		imageHeight = _imageHeight;
		
		Log.v(LOGTAG,"unscaled width: " + imageWidth + " height: " + imageHeight);

		// Update unscaled variables
		float startX = (float)_scaledStartX * (float)imageWidth/(float)_scaledImageWidth;
		float startY = (float)_scaledStartY * (float)imageHeight/(float)_scaledImageHeight;
		float endX = (float)_scaledEndX * (float)imageWidth/(float)_scaledImageWidth;
		float endY = (float)_scaledEndY * (float)imageHeight/(float)_scaledImageHeight;
		
		unscaledRect = new RectF(startX, startY, endX, endY);
		scaledRect = new RectF(_scaledStartX, _scaledStartY, _scaledEndX, _scaledEndY);

		Log.v(LOGTAG,"unscaled startX: " + startX);
		Log.v(LOGTAG,"unscaled startY: " + startY);
		Log.v(LOGTAG,"unscaled endX: " + endX);
		Log.v(LOGTAG,"unscaled endY: " + endY);
		
		// Set the background color, this is based on the type of region it is,
		// probably should be self determined rather than passed in
		setBackgroundColor(_backgroundColor);
		backgroundColor = _backgroundColor;
				
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
				
				inflatePopup(false);
			}
			
		});
    }
	
	public void inflatePopup(boolean showDelayed) {
		
			qa = new QuickAction(this);
			
			editAction = new ActionItem();
			editAction.setTitle("Edit Tag");
			editAction.setIcon(this.getResources().getDrawable(R.drawable.ic_context_edit));
			editAction.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					qa.dismiss();
					toggleMode();
				}
			});
			qa.addActionItem(editAction);

			/*
			ActionItem blurObscureAction;
			ActionItem anonObscureAction;
			ActionItem solidObscureAction;
			ActionItem pixelizeObscureAction;
			
			public static final int BLUR = 0; // BlurObscure
			public static final int ANON = 1; // MaskObscure
			public static final int SOLID = 2; // PaintSquareObscure
			public static final int PIXELIZE = 3; // PixelizeObscure
			*/
			
			
			
			solidObscureAction = new ActionItem();
			solidObscureAction.setTitle("Redact");
			solidObscureAction.setIcon(this.getResources().getDrawable(R.drawable.ic_context_fill));
			solidObscureAction.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					qa.dismiss();
					whatToDo = OBSCURE;
					obscureType = SOLID;
					imageEditor.updateDisplayImage();
				}
			});
			qa.addActionItem(solidObscureAction);
			
			pixelizeObscureAction = new ActionItem();
			pixelizeObscureAction.setTitle("Pixelate");
			pixelizeObscureAction.setIcon(this.getResources().getDrawable(R.drawable.ic_context_pixelate));
			pixelizeObscureAction.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					qa.dismiss();
					whatToDo = OBSCURE;
					obscureType = PIXELIZE;
					imageEditor.updateDisplayImage();
				}
			});
			qa.addActionItem(pixelizeObscureAction);
			

			
			blurObscureAction = new ActionItem();
			blurObscureAction.setTitle("bgPixelate");
			blurObscureAction.setIcon(this.getResources().getDrawable(R.drawable.ic_context_blur));
			blurObscureAction.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					qa.dismiss();
					whatToDo = OBSCURE;
					obscureType = BG_PIXELIZE;
					imageEditor.updateDisplayImage();
				}
			});
			qa.addActionItem(blurObscureAction);

			anonObscureAction = new ActionItem();
			anonObscureAction.setTitle("Mask");
			anonObscureAction.setIcon(this.getResources().getDrawable(R.drawable.ic_context_mask));
			anonObscureAction.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					qa.dismiss();
					whatToDo = OBSCURE;
					obscureType = ANON;
					imageEditor.updateDisplayImage();
				}
			});
			
			qa.addActionItem(anonObscureAction);
						
			removeRegionAction = new ActionItem();
			removeRegionAction.setTitle("Delete Tag");
			removeRegionAction.setIcon(this.getResources().getDrawable(R.drawable.ic_context_delete));
			removeRegionAction.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					qa.dismiss();
	            	imageEditor.deleteRegion(ImageRegion.this);
				}
			});
			qa.addActionItem(removeRegionAction);

			if (showDelayed) {
				// We need layout to pass again, let's wait a second or two
				new Handler() {
					@Override
					 public void handleMessage(Message msg) {
						 qa.show();
				        }
				}.sendMessageDelayed(new Message(), 500);
			} else {
				qa.show();
			}

	}
	
	void toggleMode() {
		// Put this here as we don't want the massive recursion that would happen in changeMode
		imageEditor.clearImageRegionsEditMode();

		if (this.mode == EDIT_MODE) {
			changeMode(NORMAL_MODE);
		} else if (this.mode == NORMAL_MODE) {
			changeMode(EDIT_MODE);
		}
	}
			
	public void changeMode(int newMode) 
	{
		mode = newMode;
		if (mode == EDIT_MODE) {
			editAction.setTitle("Edit Complete");

			setBackgroundColor(backgroundColor);
			

			topLeftCorner.setVisibility(View.VISIBLE);
			topRightCorner.setVisibility(View.VISIBLE);
			bottomLeftCorner.setVisibility(View.VISIBLE);
			bottomRightCorner.setVisibility(View.VISIBLE);
		} else if (mode == NORMAL_MODE) {
			if (editAction != null) {
				editAction.setTitle("Edit");
			}

			//setBackgroundColor(0xffffffff);
			setBackgroundDrawable(imageEditor.getResources().getDrawable(R.drawable.border));
			
			topLeftCorner.setVisibility(View.GONE);
			topRightCorner.setVisibility(View.GONE);
			bottomLeftCorner.setVisibility(View.GONE);
			bottomRightCorner.setVisibility(View.GONE);
		}
	}
	
	public void updateScaledRect(int _scaledImageWidth, int _scaledImageHeight) 
	{
		float scaledStartX = (float)unscaledRect.left * (float)_scaledImageWidth/(float)imageWidth;
		float scaledStartY = (float)unscaledRect.top * (float)_scaledImageHeight/(float)imageHeight;
		float scaledEndX = (float)unscaledRect.right * (float)_scaledImageWidth/(float)imageWidth;
		float scaledEndY = (float)unscaledRect.bottom * (float)_scaledImageHeight/(float)imageHeight;
		
		updateScaledRect(scaledStartX, scaledStartY, scaledEndX, scaledEndY);
	}
		
	public void updateScaledRect(float scaledStartX, float scaledStartY, float scaledEndX, float scaledEndY) 
	{
		scaledRect = new RectF(scaledStartX, scaledStartY, scaledEndX, scaledEndY);
		
		float startX = (float)scaledStartX * (float)imageWidth/(float)imageEditor.getScaleOfImage().width();
		float startY = (float)scaledStartY * (float)imageHeight/(float)imageEditor.getScaleOfImage().height();
		float endX = (float)scaledEndX * (float)imageWidth/(float)imageEditor.getScaleOfImage().width();
		float endY = (float)scaledEndY * (float)imageHeight/(float)imageEditor.getScaleOfImage().height();
		
		unscaledRect = new RectF(startX, startY, endX, endY);	
		Log.v(LOGTAG,"unscaledRect: startX: " + startX);
		updateLayoutParams();
	}
		
	
	public Rect getAScaledRect(int scaledWidth, int scaledHeight) 
	{
		float scaledStartX = (float)unscaledRect.left * (float)scaledWidth/(float)imageWidth;
		float scaledStartY = (float)unscaledRect.top * (float)scaledHeight/(float)imageHeight;
		float scaledEndX = (float)unscaledRect.right * (float)scaledWidth/(float)imageWidth;
		float scaledEndY = (float)unscaledRect.bottom * (float)scaledHeight/(float)imageHeight;
		
		return new Rect((int)scaledStartX, (int)scaledStartY, (int)scaledEndX, (int)scaledEndY);
	}
	
	private void updateLayoutParams() {
		// Update LayoutParams
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) this.getLayoutParams();
		if (lp == null) {
			lp = new RelativeLayout.LayoutParams((int)scaledRect.width(), (int)scaledRect.height());
		}

		// I hate that this is what needs to be done
		// in order to put the region in the right place
		// after an image has been scaled but 
		// you gotta do what you gotta do
		// Get the scale matrix values from the imageEditor
		float[] matrixValues = new float[9];
		imageEditor.matrix.getValues(matrixValues);
		// Pull out the x and y values (actually the z index operating on each)
		int x = (int)matrixValues[2];
		int y = (int)matrixValues[5];
		
		Log.v(LOGTAG,"matrixValues[2]:" + x);
		Log.v(LOGTAG,"matrixValues[5]:" + y);
		
		// Change the layout margins to account for the scale changes
    	lp.leftMargin = (int)scaledRect.left + x;
    	lp.topMargin = (int)scaledRect.top + y;
    	
    	lp.width = (int)scaledRect.width();
    	lp.height = (int)scaledRect.height();
    	
    	this.setLayoutParams(lp);	
    	
    	//not working yet
    	//imageEditor.moveAndZoom(x, y, 2);
    	
    	Log.v(LOGTAG,"Left Margin: " + lp.leftMargin + ", Top Margin: " + lp.topMargin + " Width: " + lp.width + " Height: " + lp.height);
	}
	
	public boolean onTouch(View v, MotionEvent event) 
	{
		Log.v(LOGTAG,"onTouch");
		
		
		if (mode == NORMAL_MODE)
		{
			// Just a click, return false
			return false;
		}
		else if (mode == EDIT_MODE) 
		{
			Log.v(LOGTAG,"onTouch mode EDIT");

			switch (event.getAction() & MotionEvent.ACTION_MASK) {
				
				case MotionEvent.ACTION_DOWN:
					
					Log.v(LOGTAG,"ACTION_DOWN");
					
					imageEditor.doRealtimePreview = false;
					imageEditor.updateDisplayImage();
					
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

					imageEditor.doRealtimePreview = true;
					imageEditor.updateDisplayImage();
					
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
											
						float xdist = startPoint.x - event.getX();
						float ydist = startPoint.y - event.getY();
						
						Log.v(LOGTAG,"event.getX(): " + event.getX());
						Log.v(LOGTAG,"event.getY(): " + event.getY());
						Log.v(LOGTAG,"xdist: " + xdist);
						Log.v(LOGTAG,"ydist: " + ydist);
												
						if (whichEditMode == TOP_LEFT) {
							// Here we expand
							Log.v(LOGTAG,"TOP LEFT CORNER");

							updateScaledRect(scaledRect.left - (int)xdist,
									scaledRect.top = scaledRect.top - (int)ydist,
									scaledRect.right,
									scaledRect.bottom
							);
							
					    	// Reset start point
							startPoint = new PointF(event.getX(),event.getY());
						
						} else if (whichEditMode == TOP_RIGHT) {
							// Here we expand
							Log.v(LOGTAG,"TOP RIGHT CORNER");

							updateScaledRect(scaledRect.left,
									scaledRect.top = scaledRect.top - (int)ydist,
									scaledRect.right = scaledRect.right - (int)xdist,
									scaledRect.bottom
							);

					    	// Reset start point
							startPoint = new PointF(event.getX(),event.getY());

						} else if (whichEditMode == BOTTOM_LEFT) {
							// Here we expand
							Log.v(LOGTAG,"BOTTOM LEFT CORNER");

							updateScaledRect(scaledRect.left - (int)xdist,
									scaledRect.top,
									scaledRect.right,
									scaledRect.bottom = scaledRect.bottom - (int)ydist
							);

					    	// Reset start point
							startPoint = new PointF(event.getX(),event.getY());

						} else if (whichEditMode == BOTTOM_RIGHT) {
							Log.v(LOGTAG,"BOTTOM RIGHT CORNER");

							updateScaledRect(scaledRect.left,
									scaledRect.top,
									scaledRect.right = scaledRect.right - (int)xdist,
									scaledRect.bottom = scaledRect.bottom - (int)ydist
							);

					    	// Reset start point
							startPoint = new PointF(event.getX(),event.getY());
							
						} else if (whichEditMode == MOVE) {
							Log.v(LOGTAG,"MOVE REGION " + xdist + " " + ydist);
							
							updateScaledRect(scaledRect.left - (int)xdist,
									scaledRect.top = scaledRect.top - (int)ydist,
									scaledRect.right = scaledRect.right - (int)xdist,
									scaledRect.bottom = scaledRect.bottom - (int)ydist
							);
						}

						imageEditor.updateDisplayImage();
						
						Log.v(LOGTAG,"AFTER MOVE: Left: " + scaledRect.left + " Right: " + scaledRect.right + 
								" Top: " + scaledRect.top + " Bottom: " + scaledRect.bottom);
				    	
				    	return true;
					}	
					return false;
					
				case MotionEvent.ACTION_OUTSIDE:
					Log.v(LOGTAG,"ACTION_OUTSIDE");
					
					imageEditor.doRealtimePreview = true;
					imageEditor.updateDisplayImage();
					
					whichEditMode = NONE;
					doMenu = false;
					return true;
					
				case MotionEvent.ACTION_CANCEL:
					Log.v(LOGTAG,"ACTION_CANCEL");
					
					imageEditor.doRealtimePreview = true;
					imageEditor.updateDisplayImage();
					
					whichEditMode = NONE;
					doMenu = false;
					return true;
					
				default:
					Log.v(LOGTAG, "DEFAULT: " + (event.getAction() & MotionEvent.ACTION_MASK));
					whichEditMode = NONE;
					
					imageEditor.doRealtimePreview = true;
					imageEditor.updateDisplayImage();
					
					doMenu = false;
					return true;
			}
		}
		return true;
	}	
}
