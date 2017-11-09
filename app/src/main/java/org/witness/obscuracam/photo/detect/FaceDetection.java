package org.witness.obscuracam.photo.detect;

import java.util.ArrayList;

import android.graphics.Bitmap;

public interface FaceDetection {
	int findFaces(Bitmap bmp); // returns number of faces
	ArrayList<DetectedFace> getFaces(int numberFound); // returns array of rectangles of found faces
	void release ();
}
