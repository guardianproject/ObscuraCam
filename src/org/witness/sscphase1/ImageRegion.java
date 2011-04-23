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
	public static final int ID_MODE = 2;
	int mode = EDIT_MODE;
	
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
        
        setOnTouchListener(this);
        
        moveRegion.setOnClickListener(new OnClickListener (){

			// @Override
			public void onClick(View v)
			{
				inflatePopup();
				qa.show();
				
				
			}
			
		});
        
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
					ImageRegion.this.changeMode(ID_MODE);
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
	
	public boolean onTouch(View v, MotionEvent event) {
		boolean handled = false;

		if (mode == NORMAL_MODE)
		{
			/*
			switch (event.getAction() & MotionEvent.ACTION_MASK) 
			{
			
				case MotionEvent.ACTION_DOWN:
					
					qa.show();
					
				break;
			}*/
			handled = false;
		}
		else if (mode == EDIT_MODE) {
			Log.v(LOGTAG,"onTouch mode EDIT");
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
				
				
				case MotionEvent.ACTION_DOWN:
						startPoint = new PointF(event.getX(),event.getY());
	
						scaledImage = imageEditor.getScaleOfImage();
						scaledRect = getScaledRect((int)scaledImage.width(), (int)scaledImage.height());
						
						// To do it with coordinates.. Things are just ever so slightly off
						
						Log.v(LOGTAG,"startPoint.x: " + startPoint.x + " startPoint.y: " + startPoint.y);
						Log.v(LOGTAG,"scaledRect.left: " + scaledRect.left + " scaledRect.right: " + scaledRect.right);
						Log.v(LOGTAG,"scaledRect.top: " + scaledRect.top + " scaledRect.bottom: " + scaledRect.bottom);
						Log.v(LOGTAG,"moveRegion.left(): " + this.getLeft() + " moveRegion.right(): " + this.getRight());
						Log.v(LOGTAG,"moveRegion.top()" + this.getTop() + " moveRegion.bottom()" + this.getBottom());
						handled = true;
					break;
				
				case MotionEvent.ACTION_UP:
						
					break;
				
				case MotionEvent.ACTION_MOVE:
					Log.v(LOGTAG,"Action Move");
					
					if (scaledRect == null)
						break;
					
						float xdist = startPoint.x - event.getX();
						float ydist = startPoint.y - event.getY();

						if (v == topLeftCorner || 
								event.getX() < CORNER_TOUCH_TOLERANCE &&
								event.getY() < CORNER_TOUCH_TOLERANCE
								) {
							// Here we expand
							Log.v(LOGTAG,"TOP LEFT CORNER");
							scaledRect.left = scaledRect.left - (int)xdist;
							scaledRect.top = scaledRect.top - (int)ydist;
						
						} else if (v == topRightCorner ||
								event.getX() > this.getWidth() - CORNER_TOUCH_TOLERANCE &&
								event.getY() < CORNER_TOUCH_TOLERANCE
						) {
							// Here we expand
							Log.v(LOGTAG,"TOP RIGHT CORNER");
							scaledRect.top = scaledRect.top - (int)ydist;
							scaledRect.right = scaledRect.right - (int)xdist;
							
						} else if (v == bottomLeftCorner || (
								event.getX() < CORNER_TOUCH_TOLERANCE &&
								event.getY() > this.getHeight() - CORNER_TOUCH_TOLERANCE
						)) {
							// Here we expand
							Log.v(LOGTAG,"BOTTOM LEFT CORNER");
							scaledRect.left = scaledRect.left - (int)xdist;
							scaledRect.bottom = scaledRect.bottom - (int)ydist;			
							
						} else if (v == bottomRightCorner || (
								event.getX() > this.getWidth() - CORNER_TOUCH_TOLERANCE &&
								event.getY() > this.getHeight() - CORNER_TOUCH_TOLERANCE
						)) {
							// Here we expand
							Log.v(LOGTAG,"BOTTOM RIGHT CORNER");
							scaledRect.right = scaledRect.right - (int)xdist;
							scaledRect.bottom = scaledRect.bottom - (int)ydist;
							
						} else if (v == moveRegion || (
								event.getX() < this.getWidth() - CORNER_TOUCH_TOLERANCE &&
								event.getX() > CORNER_TOUCH_TOLERANCE &&
								event.getY() < this.getHeight() - CORNER_TOUCH_TOLERANCE &&
								event.getY() > CORNER_TOUCH_TOLERANCE						
						)) {
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
						
					break;				
			}
		}
		return false;
	}
	
	/*
	public boolean onTouchEvent(MotionEvent ev) {
		Log.v(LOGTAG,"onTouchEvent");
		return false;
	}
	*/
	/*
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		Log.v(LOGTAG,"onIntercept");
		return false;
	}
	*/
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
