package org.witness.sscphase1.detect;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

public interface FaceDetection {
	void setBitmap(Bitmap bmp);
	int findFaces(); // returns number of faces
	RectF[] getFaces(); // returns array of rectangles of found faces
}
