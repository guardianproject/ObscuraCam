package org.witness.sscphase1;

import android.graphics.Bitmap;
import android.graphics.Rect;

public interface FaceDetection {
	void setBitmap(Bitmap bmp);
	int findFaces(); // returns number of faces
	Rect[] getFaces(); // returns array of rectangles of found faces
}
