/*
 * This ObscureMethod paints a solid blue rectangle over the region
 */

package org.witness.securesmartcam.filters;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class PaintSquareObscure implements ObscureMethod {

	Paint paint;
	
	public PaintSquareObscure() {
		paint = new Paint();
        paint.setColor(Color.BLACK);
	}
 	
	public void obscureRect(Rect rect, Canvas canvas) {
		canvas.drawRect(rect, paint);
	}

}
