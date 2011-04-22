package org.witness.sscphase1;

import java.io.Serializable;
import java.util.ArrayList;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
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
	int mode = NORMAL_MODE;
	
	public static final int NOTHING = 0;
	public static final int OBSCURE = 1;
	public static final int ENCRYPT = 2;
	int whattodo = NOTHING;
	
	private ImageEditor imageEditor;
	
	private ArrayList<SSCSubject> knownSubjects;
	private ArrayList<String> knownKeys;
	
	QuickAction qa;
	
	public static final String SSC = "[Camera Obscura : ImageRegion] **************************** ";
	public static final String LOGTAG = SSC;
	//public ImageRegion(Context context, String jsonVersion) {
		// Implement this from JSON
		//this(context, _scaledStartX, _scaledStartY, _scaledEndX, _scaledEndY, _scaledImageWidth, _scaledImageHeight, _imageWidth, _imageHeight, _backgroundColor);	
	//}

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
				
	public ImageRegion(
			ImageEditor imageEditor, 
			int _scaledStartX, int _scaledStartY, 
			int _scaledEndX, int _scaledEndY, 
			int _scaledImageWidth, int _scaledImageHeight, 
			int _imageWidth, int _imageHeight, 
			int _backgroundColor) 
	{
		super(imageEditor);
		
		this.imageEditor = imageEditor;
		
		/*
		original 300
		current 100
		scaled x 20
		real x 60
		original/current * scaled = real
		
		scaled = real * current/original
		*/
		
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

		this.setOnClickListener(this);
		this.setOnTouchListener(this);

		// Inflate Layout
		LayoutInflater inflater = LayoutInflater.from(imageEditor);        
        View innerView = inflater.inflate(R.layout.imageregioninner, null);
        
        topLeftCorner = innerView.findViewById(R.id.TopLeftCorner);
        topRightCorner = innerView.findViewById(R.id.TopRightCorner);
        bottomLeftCorner = innerView.findViewById(R.id.BottomLeftCorner);
        bottomRightCorner = innerView.findViewById(R.id.BottomRightCorner);
        moveRegion = innerView.findViewById(R.id.MoveRegion);

		topLeftCorner.setVisibility(View.INVISIBLE);
		topRightCorner.setVisibility(View.INVISIBLE);
		bottomLeftCorner.setVisibility(View.INVISIBLE);
		bottomRightCorner.setVisibility(View.INVISIBLE);

		topLeftCorner.setOnTouchListener(this);
		topRightCorner.setOnTouchListener(this);
		bottomLeftCorner.setOnTouchListener(this);
		bottomRightCorner.setOnTouchListener(this);	
		
		/*
		moveRegion.setOnClickListener(this);
		moveRegion.setOnTouchListener(this);
		*/
        this.addView(innerView);
        
        this.knownSubjects = new ArrayList<SSCSubject>();
	}
	
	public void addSubjectId(String subjectName, int subjectConsent) {
		SSCSubject subject = new SSCSubject(subjectName,subjectConsent);
		knownSubjects.add(subject);
	}
	
	public void addEncryptedKey(ArrayList<String> eKeys) {
		this.knownKeys = eKeys;
	}
	
	private void inflatePopup() {
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
	}
	
	public void changeMode(int newMode) {
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
	
	public Rect getScaledRect(int _scaledImageWidth, int _scaledImageHeight) {
		
		float scaledStartX = (float)startX * (float)_scaledImageWidth/(float)imageWidth;
		float scaledStartY = (float)startY * (float)_scaledImageHeight/(float)imageHeight;
		float scaledEndX = (float)endX * (float)_scaledImageWidth/(float)imageWidth;
		float scaledEndY = (float)endY * (float)_scaledImageHeight/(float)imageHeight;
		
		return new Rect((int)scaledStartX, (int)scaledStartY, (int)scaledEndX, (int)scaledEndY);
	}
	
	public boolean onTouch(View v, MotionEvent event) {
		boolean handled = false;

		if (mode == EDIT_MODE) {
			
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
				
				case MotionEvent.ACTION_DOWN:
					/*
					if (v == moveRegion || v == topLeftCorner || v == topRightCorner ||
							v == bottomLeftCorner || v == bottomRightCorner || v == this) {
					*/
						startPoint = new PointF(event.getX(),event.getY());
	
						scaledImage = imageEditor.getScaleOfImage();
						scaledRect = getScaledRect((int)scaledImage.width(), (int)scaledImage.height());
	
						Log.v(LOGTAG,"startPoint.x: " + startPoint.x + " scaledRect.left: " + scaledRect.left);
						
						handled = true;
					/*
					}
					*/
					break;
				
				case MotionEvent.ACTION_UP:
					break;
				
				case MotionEvent.ACTION_MOVE:
					
					if (v == moveRegion || v == topLeftCorner || v == topRightCorner ||
							v == bottomLeftCorner || v == bottomRightCorner || v == this) {

						float xdist = startPoint.x - event.getX();
						float ydist = startPoint.y - event.getY();

						if (v == topLeftCorner) {
							// Here we expand
							Log.v(LOGTAG,"TOP LEFT CORNER");
							scaledRect.left = scaledRect.left - (int)xdist;
							scaledRect.top = scaledRect.top - (int)ydist;
							handled = true;
						} else if (v == topRightCorner) {
							// Here we expand
							Log.v(LOGTAG,"TOP RIGHT CORNER");
							scaledRect.top = scaledRect.top - (int)ydist;
							scaledRect.right = scaledRect.right - (int)xdist;
							handled = true;
						} else if (v == bottomLeftCorner) {
							// Here we expand
							Log.v(LOGTAG,"BOTTOM LEFT CORNER");
							scaledRect.left = scaledRect.left - (int)xdist;
							scaledRect.bottom = scaledRect.bottom - (int)ydist;			
							handled = true;
						} else if (v == bottomRightCorner) {
							// Here we expand
							Log.v(LOGTAG,"BOTTOM RIGHT CORNER");
							scaledRect.right = scaledRect.right - (int)xdist;
							scaledRect.bottom = scaledRect.bottom - (int)ydist;
							handled = true;
						} else if (v == moveRegion) {
							Log.v(LOGTAG,"MOVE REGION " + xdist + " " + ydist);
							scaledRect.left = scaledRect.left - (int)xdist;
							scaledRect.top = scaledRect.top - (int)ydist;
							scaledRect.right = scaledRect.right - (int)xdist;
							scaledRect.bottom = scaledRect.bottom - (int)ydist;
							handled = true;
						} else if (v == this) {
							Log.v(LOGTAG,"FULL " + xdist + " " + ydist);
							scaledRect.left = scaledRect.left - (int)xdist;
							scaledRect.top = scaledRect.top - (int)ydist;
							scaledRect.right = scaledRect.right - (int)xdist;
							scaledRect.bottom = scaledRect.bottom - (int)ydist;
							handled = true;
						}
						 
						if (handled == true) {
							startX = (float)scaledRect.left * (float)imageWidth/(float)scaledImage.width();
							startY = (float)scaledRect.top * (float)imageHeight/(float)scaledImage.height();
							endX = (float)scaledRect.right * (float)imageWidth/(float)scaledImage.width();
							endY = (float)scaledRect.bottom * (float)imageHeight/(float)scaledImage.height();
	
							imageEditor.redrawRegions();
						}
					}
					break;				
			}
		}
		return handled;
	}
	
	public void onClick(View v) {
		Log.d(SSC,"CLICKED View " + v.toString());
		if (v == this || v == moveRegion) {
			/*
			qa = new QuickAction(v);
			for(int x=0;x<aiList.size();x++) {
				qa.addActionItem(aiList.get(x));
			}
			*/
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
