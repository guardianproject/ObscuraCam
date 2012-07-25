package org.witness.securesmartcam.detect;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

public interface FaceDetection {
	int findFaces(Bitmap bmp); // returns number of faces
	ArrayList<DetectedFace> getFaces(int numberFound); // returns array of rectangles of found faces
}
