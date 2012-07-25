package org.witness.securesmartcam.detect;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.util.Log;

public class GoogleFaceDetection implements FaceDetection {

	public static final String LOGTAG = "GoogleFaceDetection"; 
	
	public static int MAX_FACES = 10;

	Face[] faces = new Face[MAX_FACES];   	
	FaceDetector faceDetector;

	int numFaces = 0;

	public final static float CONFIDENCE_FILTER = .15f;
	
	public GoogleFaceDetection(int width, int height) {
		
		faceDetector = new FaceDetector(width, height, MAX_FACES);
	
	}

	public int findFaces(Bitmap bmp) {
		
		numFaces = faceDetector.findFaces(bmp, faces);
		return numFaces;
	}

	public ArrayList<DetectedFace> getFaces(int foundFaces) {
		
		ArrayList<DetectedFace> dFaces = new ArrayList<DetectedFace>();
		
        for (int i = 0; i < foundFaces; i++) {
        	
        	if (faces[i].confidence() > CONFIDENCE_FILTER)
        	{
		    	PointF midPoint = new PointF();
		    	
		    	float eyeDistance = faces[i].eyesDistance();
		    	faces[i].getMidPoint(midPoint);
		
		    	// Create Rectangle
		    	/*
		    	float poseX = faces[i].pose(Face.EULER_X);
		    	float poseY = faces[i].pose(Face.EULER_Y);
		    	float poseZ = faces[i].pose(Face.EULER_Z);
		    	
		    	Log.i(LOGTAG,"euclid: " + poseX + "," + poseY + "," + poseZ);
		    	*/
		    	
		    	float widthBuffer = eyeDistance * 1.5f;
		    	float heightBuffer = eyeDistance * 2f;
		    	RectF faceRect = new RectF((midPoint.x-widthBuffer), 
		    			(midPoint.y-heightBuffer), 
		    			(midPoint.x+widthBuffer), 
		    			(midPoint.y+heightBuffer));
		    
		    	DetectedFace dFace = new DetectedFace();
		    	dFace.bounds = faceRect;
		    	dFace.midpoint = midPoint;
		    	dFace.eyeDistance = eyeDistance;
		    	
		    	dFaces.add(dFace);
        	}
        }
	   	
	    return dFaces;
	}

}
