package org.witness.sscphase1;

import java.io.Serializable;
import java.util.ArrayList;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

/**
 * @author vanevery
 *
 */
public class ImageRegion extends FrameLayout implements OnTouchListener, OnClickListener, Serializable {

	private static final long serialVersionUID = -244965540057504061L;

	float startX;
	float startY;
	float endX;
	float endY;
	
	PointF startPoint = new PointF();

	int imageWidth;
	int imageHeight;
	
	public static final int CORNER_TOUCH_TOLERANCE = 35;
		
	public static final int NORMAL_MODE = 0;
	public static final int EDIT_MODE = 1;
	//public static final int ID_MODE = 2;
	int mode = EDIT_MODE;
	
	public final static int NONE = 0;
	public final static int MOVE = 1;
	public final static int TOP_LEFT = 2;
	public final static int BOTTOM_LEFT = 3;
	public final static int TOP_RIGHT = 4;
	public final static int BOTTOM_RIGHT = 5;
	
	int whichEditMode = NONE;
	
	
	public static final int NOTHING = 0;
	public static final int OBSCURE = 1;
	public static final int ENCRYPT = 2;
	int whattodo = NOTHING;
	
	ImageEditor imageEditor;
	
	private ArrayList<SSCSubject> knownSubjects;
	private ArrayList<String> knownKeys;
	
	QuickAction qa;
	
	public static final String SSC = "[Camera Obscura : ImageRegion] **************************** ";
	public static final String LOGTAG = SSC;

	RectF scaledImage;
	Rect scaledRect;
	
	View topLeftCorner;
	View topRightCorner;
	View bottomLeftCorner;
	View bottomRightCorner;
	View moveRegion;
	
	ActionItem editAction;
	ActionItem idAction;
	ActionItem encryptAction;
	ActionItem destroyAction;
	ActionItem removeRegionAction;
	
	float minMoveDistanceDP = 2f;
	float minMoveDistance;	
				
	public ImageRegion(
			ImageEditor _imageEditor, 
			int _scaledStartX, int _scaledStartY, 
			int _scaledEndX, int _scaledEndY, 
			int _scaledImageWidth, int _scaledImageHeight, 
			int _imageWidth, int _imageHeight, 
			int _backgroundColor) 
	{
		super(_imageEditor);
		
		imageEditor = _imageEditor;

		float scale = this.getResources().getDisplayMetrics().density;
		minMoveDistance = minMoveDistanceDP * scale + 0.5f;
		
		startX = (float)_imageWidth/(float)_scaledImageWidth * (float)_scaledStartX;
		startY = (float)_imageHeight/(float)_scaledImageHeight * (float)_scaledStartY;
		endX = (float)_imageWidth/(float)_scaledImageWidth * (float)_scaledEndX;
		endY = (float)_imageHeight/(float)_scaledImageHeight * (float)_scaledEndY;
		
		Log.v(LOGTAG,"startX: " + startX);
		Log.v(LOGTAG,"startY: " + startY);
		Log.v(LOGTAG,"endX: " + endX);
		Log.v(LOGTAG,"endY: " + endY);
				
		imageWidth = _imageWidth;
		imageHeight = _imageHeight;
		
		setBackgroundColor(_backgroundColor);
		
		inflatePopup();
		
		// Inflate Layout
		LayoutInflater inflater = LayoutInflater.from(imageEditor);        
		inflater.inflate(R.layout.imageregioninner, this, true);
		
        //View innerView = inflater.inflate(R.layout.imageregioninner, null);
        
        topLeftCorner = findViewById(R.id.TopLeftCorner);
        topRightCorner = findViewById(R.id.TopRightCorner);
        bottomLeftCorner = findViewById(R.id.BottomLeftCorner);
        bottomRightCorner = findViewById(R.id.BottomRightCorner);
        moveRegion = findViewById(R.id.MoveRegion);

        /*  Currently in EDIT mode
		topLeftCorner.setVisibility(View.INVISIBLE);
		topRightCorner.setVisibility(View.INVISIBLE);
		bottomLeftCorner.setVisibility(View.INVISIBLE);
		bottomRightCorner.setVisibility(View.INVISIBLE);
		*/
        

        this.knownSubjects = new ArrayList<SSCSubject>();
        
        //setOnTouchListener(this);
        moveRegion.setOnTouchListener(this);
        
        /*FrameLayout imageRegionInner = (FrameLayout) this.findViewById(R.id.ImageRegionInner);
        imageRegionInner.setOnTouchListener(this);*/
        
        // This doesn't work with the touch listener.  Being handled on action up instead
        moveRegion.setOnClickListener(new OnClickListener (){

			// @Override
			public void onClick(View v)
			{
				inflatePopup();
				qa.show();
			}
			
		});
        
        /*
    	topLeftCorner.setOnTouchListener(this);
    	topRightCorner.setOnTouchListener(this);
    	bottomLeftCorner.setOnTouchListener(this);
    	bottomRightCorner.setOnTouchListener(this);
		*/
        
        // Are we sure we want this since it is a menu item???
        /*
        moveRegion.setOnLongClickListener(new OnLongClickListener (){

			// @Override
			public boolean onLongClick(View v)
			{
				final AlertDialog.Builder b = new AlertDialog.Builder(imageEditor);
				b.setIcon(android.R.drawable.ic_dialog_alert);
				b.setTitle(imageEditor.getString(R.string.app_name));
				b.setMessage(imageEditor.getString(R.string.confirm_delete_region));
				b.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int whichButton) {
		            	imageEditor.deleteRegion(ImageRegion.this);
		            }
		        });
				b.setNegativeButton(android.R.string.no, null);
				b.show();
				return false;
			}
		});
		*/
		
	}
	
	public void addSubjectId(String subjectName, int subjectConsent) {
		SSCSubject subject = new SSCSubject(subjectName,subjectConsent);
		knownSubjects.add(subject);
	}
	
	public void addEncryptedKey(ArrayList<String> eKeys) {
		this.knownKeys = eKeys;
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
	
			idAction = new ActionItem();
			idAction.setTitle("ID");
			idAction.setIcon(this.getResources().getDrawable(R.drawable.ic_context_id));
			idAction.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					qa.dismiss();
					//ImageRegion.this.changeMode(ID_MODE);
					imageEditor.launchIdTagger(ImageRegion.this.toString());
				}
			});
			qa.addActionItem(idAction);
			
			encryptAction = new ActionItem();
			encryptAction.setTitle("Encrypt");
			encryptAction.setIcon(this.getResources().getDrawable(R.drawable.ic_context_encrypt));
			encryptAction.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					qa.dismiss();
					whattodo = ENCRYPT;
					imageEditor.launchEncryptTagger(ImageRegion.this.toString());
				}
			});
			qa.addActionItem(encryptAction);
			
			destroyAction = new ActionItem();
			destroyAction.setTitle("Redact");
			destroyAction.setIcon(this.getResources().getDrawable(R.drawable.ic_context_destroy));
			destroyAction.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					qa.dismiss();
					whattodo = OBSCURE;
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
		
	/*boolean doMenu = false;

	public boolean onTouch(View v, MotionEvent event) {
		boolean handled = false;

		Log.v(LOGTAG,"onTouch");
		
		if (mode == NORMAL_MODE)
		{
			//switch (event.getAction() & MotionEvent.ACTION_MASK) 
			//{
			//	case MotionEvent.ACTION_DOWN:
			//		qa.show();
			//	break;
			//}
			doMenu = true;
			handled = false;
		}
		else if (mode == EDIT_MODE) {
			Log.v(LOGTAG,"onTouch mode EDIT");
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
				
				case MotionEvent.ACTION_DOWN:
						startPoint = new PointF(event.getX(),event.getY());
	
						scaledImage = imageEditor.getScaleOfImage();
						scaledRect = getScaledRect((int)scaledImage.width(), (int)scaledImage.height());
						
						//Log.v(LOGTAG,"startPoint.x: " + startPoint.x + " startPoint.y: " + startPoint.y);
						//Log.v(LOGTAG,"scaledRect.left: " + scaledRect.left + " scaledRect.right: " + scaledRect.right);
						//Log.v(LOGTAG,"scaledRect.top: " + scaledRect.top + " scaledRect.bottom: " + scaledRect.bottom);
						//Log.v(LOGTAG,"moveRegion.left(): " + this.getLeft() + " moveRegion.right(): " + this.getRight());
						//Log.v(LOGTAG,"moveRegion.top()" + this.getTop() + " moveRegion.bottom()" + this.getBottom());
						
						
						if (v == topLeftCorner || (
								event.getX() < CORNER_TOUCH_TOLERANCE &&
								event.getY() < CORNER_TOUCH_TOLERANCE
								)) {
							whichEditMode = TOP_LEFT;
						} else if (v == topRightCorner || (
								event.getX() > this.getWidth() - CORNER_TOUCH_TOLERANCE &&
								event.getY() < CORNER_TOUCH_TOLERANCE
						)) {
							whichEditMode = TOP_RIGHT;
						} else if (v == bottomLeftCorner || (
								event.getX() < CORNER_TOUCH_TOLERANCE &&
								event.getY() > this.getHeight() - CORNER_TOUCH_TOLERANCE
						)) {
							whichEditMode = BOTTOM_LEFT;
						} else if (v == bottomRightCorner || (
								event.getX() > this.getWidth() - CORNER_TOUCH_TOLERANCE &&
								event.getY() > this.getHeight() - CORNER_TOUCH_TOLERANCE
						)) {
							whichEditMode = BOTTOM_RIGHT;
						} else if (v == moveRegion || (
								event.getX() < this.getWidth() - CORNER_TOUCH_TOLERANCE &&
								event.getX() > CORNER_TOUCH_TOLERANCE &&
								event.getY() < this.getHeight() - CORNER_TOUCH_TOLERANCE &&
								event.getY() > CORNER_TOUCH_TOLERANCE						
						)) {
							whichEditMode = MOVE;
						} else {
							whichEditMode = NONE;
						}
						
						handled = true;
						doMenu = true;
					break;
				
				case MotionEvent.ACTION_UP:
					whichEditMode = NONE;
					if (doMenu) {
						inflatePopup();
						qa.show();
						doMenu = false;
					}
					break;
				
				case MotionEvent.ACTION_MOVE:
					Log.v(LOGTAG,"Action Move");
					
					float distance = (float) (Math.sqrt(Math.abs(startPoint.x - event.getX()) + Math.abs(startPoint.y - event.getY())));
					Log.v(LOGTAG,"Move Distance: " + distance);
					Log.v(LOGTAG,"Min Distance: " + minMoveDistance);
					
					if (distance > minMoveDistance) {
						handled = true;
						doMenu = false;
					
						if (scaledRect == null)
							break;
					
						float xdist = startPoint.x - event.getX();
						float ydist = startPoint.y - event.getY();

						if (whichEditMode == TOP_LEFT) {
							// Here we expand
							Log.v(LOGTAG,"TOP LEFT CORNER");
							scaledRect.left = scaledRect.left - (int)xdist;
							scaledRect.top = scaledRect.top - (int)ydist;
						
						} else if (whichEditMode == TOP_RIGHT) {
							// Here we expand
							Log.v(LOGTAG,"TOP RIGHT CORNER");
							scaledRect.top = scaledRect.top - (int)ydist;
							scaledRect.right = scaledRect.right - (int)xdist;
							
						} else if (whichEditMode == BOTTOM_LEFT) {
							// Here we expand
							Log.v(LOGTAG,"BOTTOM LEFT CORNER");
							scaledRect.left = scaledRect.left - (int)xdist;
							scaledRect.bottom = scaledRect.bottom - (int)ydist;			
							
						} else if (whichEditMode == BOTTOM_RIGHT) {
							// Here we expand
							Log.v(LOGTAG,"BOTTOM RIGHT CORNER");
							scaledRect.right = scaledRect.right - (int)xdist;
							scaledRect.bottom = scaledRect.bottom - (int)ydist;
							
						} else if (whichEditMode == MOVE) {
							Log.v(LOGTAG,"MOVE REGION " + xdist + " " + ydist);
							scaledRect.left = scaledRect.left - (int)xdist;
							scaledRect.top = scaledRect.top - (int)ydist;
							scaledRect.right = scaledRect.right - (int)xdist;
							scaledRect.bottom = scaledRect.bottom - (int)ydist;
						
						}
						 
						startX = (float)scaledRect.left * (float)imageWidth/(float)scaledImage.width();
						startY = (float)scaledRect.top * (float)imageHeight/(float)scaledImage.height();
						endX = (float)scaledRect.right * (float)imageWidth/(float)scaledImage.width();
						endY = (float)scaledRect.bottom * (float)imageHeight/(float)scaledImage.height();
						
						imageEditor.updateRegionCoordinates(this);
						imageEditor.redrawRegions();
					}	
					break;
					
			}
		}
		return handled;
	}
	
	
	public boolean onTouchEvent(MotionEvent ev) {
		Log.v(LOGTAG,"onTouchEvent");
		Log.v(LOGTAG,ev.toString());

		float distance = (float) (Math.sqrt(Math.abs(startPoint.x - ev.getX()) + Math.abs(startPoint.y - ev.getY())));
		Log.v(LOGTAG,"Move Distance: " + distance);
		Log.v(LOGTAG,"Min Distance: " + minMoveDistance);

		if (distance > minMoveDistance) {
				
			float xdist = startPoint.x - ev.getX();
			float ydist = startPoint.y - ev.getY();

			if (whichEditMode == TOP_LEFT) {
				// Here we expand
				Log.v(LOGTAG,"TOP LEFT CORNER");
				scaledRect.left = scaledRect.left - (int)xdist;
				scaledRect.top = scaledRect.top - (int)ydist;
			
			} else if (whichEditMode == TOP_RIGHT) {
				// Here we expand
				Log.v(LOGTAG,"TOP RIGHT CORNER");
				scaledRect.top = scaledRect.top - (int)ydist;
				scaledRect.right = scaledRect.right - (int)xdist;
				
			} else if (whichEditMode == BOTTOM_LEFT) {
				// Here we expand
				Log.v(LOGTAG,"BOTTOM LEFT CORNER");
				scaledRect.left = scaledRect.left - (int)xdist;
				scaledRect.bottom = scaledRect.bottom - (int)ydist;			
				
			} else if (whichEditMode == BOTTOM_RIGHT) {
				// Here we expand
				Log.v(LOGTAG,"BOTTOM RIGHT CORNER");
				scaledRect.right = scaledRect.right - (int)xdist;
				scaledRect.bottom = scaledRect.bottom - (int)ydist;
				
			} else if (whichEditMode == MOVE) {
				Log.v(LOGTAG,"MOVE REGION " + xdist + " " + ydist);
				scaledRect.left = scaledRect.left - (int)xdist;
				scaledRect.top = scaledRect.top - (int)ydist;
				scaledRect.right = scaledRect.right - (int)xdist;
				scaledRect.bottom = scaledRect.bottom - (int)ydist;
			
			}
			 
			startX = (float)scaledRect.left * (float)imageWidth/(float)scaledImage.width();
			startY = (float)scaledRect.top * (float)imageHeight/(float)scaledImage.height();
			endX = (float)scaledRect.right * (float)imageWidth/(float)scaledImage.width();
			endY = (float)scaledRect.bottom * (float)imageHeight/(float)scaledImage.height();
			
			imageEditor.updateRegionCoordinates(this);
			imageEditor.redrawRegions();
		}		
		
		return true;
	}
	*/
	/*
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		Log.v(LOGTAG,"onIntercept");
		return false;
	}
	*/
	
	boolean doMenu = false;
	int scaledLeft = 0;
	int scaledTop = 0;
	int scaledRight = 0;
	int scaledBottom = 0;
	
	public boolean onTouch(View v, MotionEvent event) {

		Log.v(LOGTAG,"onTouch");
		
		if (mode == NORMAL_MODE)
		{
			switch (event.getAction() & MotionEvent.ACTION_MASK) 
			{
				case MotionEvent.ACTION_DOWN:
					doMenu = true;
				break;
				
				case MotionEvent.ACTION_UP:
					// Show the menu
					
					inflatePopup();
					qa.show();

					doMenu = false;
				break;
				
				default:
					doMenu = false;
				break;
			}
			return true;
		}
		else if (mode == EDIT_MODE) {
			Log.v(LOGTAG,"onTouch mode EDIT");
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
				
				case MotionEvent.ACTION_DOWN:
					
					Log.v(LOGTAG,"ACTION_DOWN");
					
					startPoint = new PointF(event.getX(),event.getY());

					scaledImage = imageEditor.getScaleOfImage();
					scaledRect = getScaledRect((int)scaledImage.width(), (int)scaledImage.height());
					
					scaledLeft = scaledRect.left;
					scaledRight = scaledRect.right;
					scaledTop = scaledRect.top;
					scaledBottom = scaledRect.bottom;						
					
					//Log.v(LOGTAG,"startPoint.x: " + startPoint.x + " startPoint.y: " + startPoint.y);
					//Log.v(LOGTAG,"scaledRect.left: " + scaledRect.left + " scaledRect.right: " + scaledRect.right);
					//Log.v(LOGTAG,"scaledRect.top: " + scaledRect.top + " scaledRect.bottom: " + scaledRect.bottom);
					//Log.v(LOGTAG,"moveRegion.left(): " + this.getLeft() + " moveRegion.right(): " + this.getRight());
					//Log.v(LOGTAG,"moveRegion.top()" + this.getTop() + " moveRegion.bottom()" + this.getBottom());
					
					if (v == topLeftCorner || (
							event.getX() < CORNER_TOUCH_TOLERANCE &&
							event.getY() < CORNER_TOUCH_TOLERANCE
							)) {
						whichEditMode = TOP_LEFT;
					} else if (v == topRightCorner || (
							event.getX() > this.getWidth() - CORNER_TOUCH_TOLERANCE &&
							event.getY() < CORNER_TOUCH_TOLERANCE
					)) {
						whichEditMode = TOP_RIGHT;
					} else if (v == bottomLeftCorner || (
							event.getX() < CORNER_TOUCH_TOLERANCE &&
							event.getY() > this.getHeight() - CORNER_TOUCH_TOLERANCE
					)) {
						whichEditMode = BOTTOM_LEFT;
					} else if (v == bottomRightCorner || (
							event.getX() > this.getWidth() - CORNER_TOUCH_TOLERANCE &&
							event.getY() > this.getHeight() - CORNER_TOUCH_TOLERANCE
					)) {
						whichEditMode = BOTTOM_RIGHT;
					} else if (v == moveRegion || (
							event.getX() < this.getWidth() - CORNER_TOUCH_TOLERANCE &&
							event.getX() > CORNER_TOUCH_TOLERANCE &&
							event.getY() < this.getHeight() - CORNER_TOUCH_TOLERANCE &&
							event.getY() > CORNER_TOUCH_TOLERANCE						
					)) {
						whichEditMode = MOVE;
					} else {
						whichEditMode = NONE;
					}
					
					doMenu = true;
					return true;
				
				case MotionEvent.ACTION_UP:
					Log.v(LOGTAG,"ACTION_UP");
					
					imageEditor.updateDisplayImage();
					
					whichEditMode = NONE;
					if (doMenu) {
						// Show the menu
						
						inflatePopup();
						qa.show();

						doMenu = false;
					}
					
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
					
						float xdist = startPoint.x - event.getX();
						float ydist = startPoint.y - event.getY();
						
						/*
						Log.v(LOGTAG,"startPoint.x:" + startPoint.x);
						Log.v(LOGTAG,"startPoint.y:" + startPoint.y);
						Log.v(LOGTAG,"event.getX():" + event.getX());
						Log.v(LOGTAG,"event.getY():" + event.getY());
						Log.v(LOGTAG,"xdist:" + xdist);
						Log.v(LOGTAG,"ydist:" + ydist);
						*/
						
						if (whichEditMode == TOP_LEFT) {
							// Here we expand
							Log.v(LOGTAG,"TOP LEFT CORNER");
							scaledRect.left = scaledLeft - (int)xdist;
							scaledRect.top = scaledTop - (int)ydist;
						
						} else if (whichEditMode == TOP_RIGHT) {
							// Here we expand
							Log.v(LOGTAG,"TOP RIGHT CORNER");
							scaledRect.top = scaledTop - (int)ydist;
							scaledRect.right = scaledRight - (int)xdist;
							
						} else if (whichEditMode == BOTTOM_LEFT) {
							// Here we expand
							Log.v(LOGTAG,"BOTTOM LEFT CORNER");
							scaledRect.left = scaledLeft - (int)xdist;
							scaledRect.bottom = scaledBottom - (int)ydist;			
							
						} else if (whichEditMode == BOTTOM_RIGHT) {
							// Here we expand
							/*
							Log.v(LOGTAG,"BOTTOM RIGHT CORNER");
							Log.v(LOGTAG, "scaledRect.right - xdist:" + scaledRect.right + " " + (int)xdist);
							Log.v(LOGTAG, "scaledRect.bottom - ydist" + scaledRect.bottom + " " + (int)ydist);
							*/
							scaledRect.right = scaledRight - (int)xdist;
							scaledRect.bottom = scaledBottom - (int)ydist;
							
						} else if (whichEditMode == MOVE) {
							Log.v(LOGTAG,"MOVE REGION " + xdist + " " + ydist);
							scaledRect.left = scaledRect.left - (int)xdist;
							scaledRect.top = scaledRect.top - (int)ydist;
							scaledRect.right = scaledRect.right - (int)xdist;
							scaledRect.bottom = scaledRect.bottom - (int)ydist;
						
						}

						Log.v(LOGTAG,"Left: " + scaledRect.left + " Right: " + scaledRect.right + 
								" Top: " + scaledRect.top + " Bottom: " + scaledRect.bottom);
						
						startX = (float)scaledRect.left * (float)imageWidth/(float)scaledImage.width();
						startY = (float)scaledRect.top * (float)imageHeight/(float)scaledImage.height();
						endX = (float)scaledRect.right * (float)imageWidth/(float)scaledImage.width();
						endY = (float)scaledRect.bottom * (float)imageHeight/(float)scaledImage.height();
						
						RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) this.getLayoutParams();
				    	lp.leftMargin = (int)scaledRect.left;
				    	lp.topMargin = (int)scaledRect.top;
				    	lp.width = scaledRect.width();
				    	lp.height = scaledRect.height();
				    	//lp.rightMargin = (int)scaledRect.right;
				    	//lp.bottomMargin = (int)scaledRect.bottom;
				    	this.setLayoutParams(lp);
						
						//imageEditor.redrawRegions();
					}	
					return true;
					
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
	
	public void onClick(View v) {
		Log.d(SSC,"CLICKED View " + v.toString());
		
		if (v == this || v == moveRegion) 
		{
			
			inflatePopup();
			//qa.setAnimStyle(QuickAction.ANIM_REFLECT);
			qa.show();
		}
	}
	
	public class SSCSubject {
		String subjectName;
		int subjectConsent;
		
		SSCSubject(String sn, int sc) {
			this.subjectName = sn;
			this.subjectConsent = sc;
		}
	}

}
