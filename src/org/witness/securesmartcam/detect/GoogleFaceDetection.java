package org.witness.securesmartcam.detect;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;

public class GoogleFaceDetection implements FaceDetection {

	public static final String LOGTAG = "GoogleFaceDetection"; 
	
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

	public RectF[] getFaces() {
		RectF[] faceRects = new RectF[numFaces];
		
	    if (numFaces > 0) {
	        for (int i = 0; i < faces.length; i++) {
	        	if (faces[i] != null) {
	            	PointF midPoint = new PointF();
	            	
	            	float eyeDistance = faces[i].eyesDistance();
	            	faces[i].getMidPoint(midPoint);

	           // 	Log.v(LOGTAG,"eyeDistance: " + eyeDistance);
	           // 	Log.v(LOGTAG,"midPoint: " + midPoint.x + " " + midPoint.y);
	            	
	            	// Create Rectangle
	            	faceRects[i] = new RectF((midPoint.x-eyeDistance), 
	            			(midPoint.y-eyeDistance), 
	            			(midPoint.x+eyeDistance), 
	            			(midPoint.y+eyeDistance+eyeDistance));
	            	
	            //	Log.v(LOGTAG,"faceRect: left: " + faceRects[i].left 
	            	//		+ " top: " + faceRects[i].top 
	            		//	+ " right: " + faceRects[i].right
	            			//+ " bottom: " + faceRects[i].bottom);
	        	}
	        }
	    }	
	    return faceRects;
	}

	public void setBitmap(Bitmap _bmp) {
		bmp = _bmp;
	}				
}
