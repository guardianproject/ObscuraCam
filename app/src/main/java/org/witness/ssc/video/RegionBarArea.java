package org.witness.ssc.video;

import java.util.Vector;

import org.witness.sscphase1.ObscuraApp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class RegionBarArea extends ImageView {
	
	public static final String LOGTAG = ObscuraApp.TAG;
	
	public static final int COLOR_SELECTED = Color.YELLOW;
	public static final int COLOR_NOT_SELECTED = Color.BLUE;
	
	int backgroundColor = COLOR_NOT_SELECTED;
	
	int width = 0;
	int height = 0;
	
	Vector<ObscureRegion> obscureRegions;
	
	Bitmap RBABmp;
    Canvas RBACanvas;
	Paint RBAPaint;	
	
	public RegionBarArea(Context context) {
		super(context);
	}

	public RegionBarArea(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RegionBarArea(Context context, AttributeSet attrs, int defStyle) {
		super(context,attrs,defStyle);
	}

	public void init() {
		this.setBackgroundColor(backgroundColor);
	}
	
    @Override
    protected void onDraw(Canvas canvas) {
    	super.onDraw(canvas);
    	
    	Paint paint = new Paint();
        paint.setColor(0xFF668800);
        paint.setStyle(Paint.Style.FILL);

        int numRegions = obscureRegions.size();
        if (numRegions > 0) {
	        int regionHeight = getHeight()/numRegions;
	        int count = 0;
	        /*TODO fix this
			for (ObscureRegion region:obscureRegions) {
				canvas.drawRect(region.startTime/region.endTime*getWidth(), 
								count*regionHeight, 
								region.endTime/region.mediaDuration*getWidth(), 
								(count+1)*regionHeight, paint);
				count++;
			}
			*/
        }
        //canvas.drawRect(left, top, right, bottom, paint);
        //(float)startTime/(float)1000 + "," + (float)endTime/(float)1000
        //canvas.drawRect(10,10,width-10,height-10,paint);
        
        
        //Log.v(LOGTAG,"Just drew " + width + " " + height);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	width = widthMeasureSpec;
    	height = heightMeasureSpec;
    	
        setMeasuredDimension(widthMeasureSpec,heightMeasureSpec);     
    }	
        
    @Override
    public boolean onTouchEvent(MotionEvent me) {
    	
    	return true;
    }
	
	
}
