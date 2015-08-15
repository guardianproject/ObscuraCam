package org.witness.securesmartcam.detect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.SparseArray;


import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.util.ArrayList;

public class GooglePlayMVFaceDetection implements FaceDetection {

	public static final String LOGTAG = "GoogleFaceDetection";

	public static int MAX_FACES = 10;

	int numFaces = 0;

	public final static float CONFIDENCE_FILTER = .15f;
	FaceDetector detector = null;
	SparseArray<Face> faces = null;

	public GooglePlayMVFaceDetection(Context context, boolean tracking) {

		detector = new FaceDetector.Builder(context)
				.setTrackingEnabled(tracking)
				.setLandmarkType(FaceDetector.NO_LANDMARKS)
				.setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
				.setProminentFaceOnly(false)
				.setMode(FaceDetector.ACCURATE_MODE)
				.build();

	}


	public void release ()
	{
		detector.release();
	}


	public int findFaces(Bitmap bmp) {

		if (detector.isOperational()) {

			Frame frame = new Frame.Builder().setBitmap(bmp).build();
			faces = detector.detect(frame);
			return faces.size();
		}
		else
		{
			return 0;
		}

	}

	public ArrayList<DetectedFace> getFaces(int foundFaces) {
		
		ArrayList<DetectedFace> dFaces = new ArrayList<DetectedFace>();

        for (int i = 0; i < faces.size(); i++)
		{
				Face face = faces.valueAt(i);

		    	PointF midPoint = face.getPosition();


		    	RectF faceRect = new RectF((midPoint.x),
		    			(midPoint.y),
		    			(midPoint.x+face.getWidth()),
		    			(midPoint.y+face.getHeight()));
		    
		    	DetectedFace dFace = new DetectedFace();
		    	dFace.bounds = faceRect;
		    	dFace.midpoint = midPoint;
				dFace.eyeDistance = face.getWidth();
		    	dFaces.add(dFace);

        }
	   	
	    return dFaces;
	}

}
