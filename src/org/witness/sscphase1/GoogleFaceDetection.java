package org.witness.sscphase1;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.util.Log;

public class GoogleFaceDetection implements FaceDetection {

	public static int MAX_FACES = 100;

	Face[] faces = new Face[MAX_FACES];   	
	FaceDetector faceDetector;

	Bitmap bmp;
	int numFaces = 0;

	public GoogleFaceDetection(Bitmap _bmp) {
		setBitmap(_bmp);
		faceDetector = new FaceDetector(bmp.getWidth(), bmp.getHeight(), MAX_FACES);
	}

	public int findFaces() {
		numFaces = faceDetector.findFaces(bmp, faces);
		return numFaces;
	}

	public Rect[] getFaces() {
		Rect[] faceRects = new Rect[numFaces];
		
	    if (numFaces > 0) {
	        for (int i = 0; i < faces.length; i++) {
	        	if (faces[i] != null) {
	            	PointF midPoint = new PointF();
	            	
	            	float eyeDistance = faces[i].eyesDistance();
	            	faces[i].getMidPoint(midPoint);
	            	
	            	// Create Rectangle
	            	faceRects[i] = new Rect((int)(midPoint.x-eyeDistance*2), (int)(midPoint.x-eyeDistance*2), (int)(midPoint.x+eyeDistance*2), (int)(midPoint.x+eyeDistance*2));
	        	}
	        }
	    }	
	    return faceRects;
	}

	public void setBitmap(Bitmap _bmp) {
		bmp = _bmp;
	}				
}
