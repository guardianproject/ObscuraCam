package org.witness.obscuracam.photo.detect;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;

public class AndroidFaceDetection implements FaceDetection {

	public static final String LOGTAG = "AndroidFaceDetection";
	
	public static int MAX_FACES = 10;

	Face[] faces = new Face[MAX_FACES];   	
	FaceDetector faceDetector;

	int numFaces = 0;

	public final static float CONFIDENCE_FILTER = .15f;
	
	public AndroidFaceDetection(int width, int height) {
		
		faceDetector = new FaceDetector(width, height, MAX_FACES);
	
	}

	public void release ()
	{
		faceDetector = null;
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
