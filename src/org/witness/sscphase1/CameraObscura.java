package org.witness.sscphase1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Bitmap.CompressFormat;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class CameraObscura extends Activity implements OnClickListener, OnTouchListener {
    
	final static String LOGTAG = "CAMERA OBSCRUA";
	
	final static int MAX_FACES = 10;
	
	final static int CAMERA_RESULT = 0;
	final static int GALLERY_RESULT = 1;

	ImageView imv;
	Button choosePictureButton, takePictureButton;
	Button savePictureButton, cancelButton;

	// Be sure to delete this
	File tmpImageFile;
	//Uri.fromFile(tmpImageFile)

	Bitmap alteredBitmap;
	Canvas canvas;
	Paint paint; 
	
	float downx = 0;
	float downy = 0;
	float upx = 0;
	float upy = 0;
	
	FaceDetector faceDetector;
    Face[] faces = new Face[MAX_FACES];   	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		// Get a reference to the ImageView
		imv = (ImageView) findViewById(R.id.ImageView);
		imv.setOnTouchListener(this);

    	choosePictureButton = (Button) this.findViewById(R.id.ChoosePictureButton);
    	choosePictureButton.setOnClickListener(this);
    	
    	takePictureButton = (Button) this.findViewById(R.id.TakePictureButton);
    	takePictureButton.setOnClickListener(this);
    	
    	savePictureButton = (Button) this.findViewById(R.id.SavePictureButton);
    	savePictureButton.setOnClickListener(this);
    	
    	cancelButton = (Button) this.findViewById(R.id.CancelButton);
    	cancelButton.setOnClickListener(this);
    	
    	// Out in the open
    	tmpImageFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/myfavoritepicture.jpg");
    	
    	// Damn, can't write here from an intent
		//tmpImageFile = File.createTempFile("tmp", ".jpg", getDir("tmpimages", MODE_PRIVATE));
    }

	public void onClick(View v) {
		if (v == choosePictureButton) {			
			// Only from the Memory Card.
			// INTERNAL_CONTENT_URI from the normal memory space
			Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(i, GALLERY_RESULT);
		} else if (v == takePictureButton) {
			Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
			i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(tmpImageFile));
			startActivityForResult(i, CAMERA_RESULT);			
		} else if (v == savePictureButton) {
			if (alteredBitmap != null) {
				ContentValues contentValues = new ContentValues();

				Uri imageFileUri = getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, contentValues);

				try {
					OutputStream imageFileOS = getContentResolver().openOutputStream(imageFileUri);
					alteredBitmap.compress(CompressFormat.JPEG, 90, imageFileOS);
					Toast t = Toast.makeText(this, "Saved", Toast.LENGTH_SHORT);
					t.show();
				} catch (FileNotFoundException e) {
					Log.v("EXCEPTION", e.getMessage());
				}
			}			
			cancelButton.setVisibility(View.GONE);
			savePictureButton.setVisibility(View.GONE);
			takePictureButton.setVisibility(View.VISIBLE);
			choosePictureButton.setVisibility(View.VISIBLE);
		} else if (v == cancelButton) {
			cancelButton.setVisibility(View.GONE);
			savePictureButton.setVisibility(View.GONE);
			takePictureButton.setVisibility(View.VISIBLE);
			choosePictureButton.setVisibility(View.VISIBLE);
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (resultCode == RESULT_OK) {
			if (requestCode == GALLERY_RESULT || requestCode == CAMERA_RESULT) 
			{
				try {
					// Size the image for the screen/memory
					
					Display currentDisplay = getWindowManager().getDefaultDisplay();
					int dw = currentDisplay.getWidth();
					int dh = currentDisplay.getHeight();
					
					Uri imageFileUri = Uri.fromFile(tmpImageFile);
					
					if (requestCode == GALLERY_RESULT) {
						imageFileUri = intent.getData();
					}
					Log.v(LOGTAG,imageFileUri.toString());
		
					// Load up the image's dimensions not the image itself
					BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
					bmpFactoryOptions.inJustDecodeBounds = true;
					
					Bitmap	bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageFileUri), null, bmpFactoryOptions);
					
					int heightRatio = (int) Math.ceil(bmpFactoryOptions.outHeight / (float) dh);
					int widthRatio = (int) Math.ceil(bmpFactoryOptions.outWidth / (float) dw);
		
					Log.v("HEIGHTRATIO", "" + heightRatio);
					Log.v("WIDTHRATIO", "" + widthRatio);
		
					// If both of the ratios are greater than 1,
					// one of the sides of the image is greater than the screen
					if (heightRatio > 1 && widthRatio > 1) {
						if (heightRatio > widthRatio) {
							// Height ratio is larger, scale according to it
							bmpFactoryOptions.inSampleSize = heightRatio;
						} else {
							// Width ratio is larger, scale according to it
							bmpFactoryOptions.inSampleSize = widthRatio;
						}
					}
			
					// Decode it for real
					bmpFactoryOptions.inJustDecodeBounds = false;
					bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageFileUri), null, bmpFactoryOptions);
					
					if (bmp == null) {
						Log.v(LOGTAG,"bmp is null");
					}
					
					// Canvas for drawing
					alteredBitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
					canvas = new Canvas(alteredBitmap);
					paint = new Paint();
					canvas.drawBitmap(bmp, 0, 0, paint);
					
					// Face Detection
					faceDetector = new FaceDetector(bmp.getWidth(), bmp.getHeight(), MAX_FACES);
		            int numFaces = faceDetector.findFaces(bmp, faces);
		           
		            Log.v(LOGTAG,"Num Faces Found: " + numFaces); 
		            
		            if (numFaces > 0) {
		                paint.setColor(Color.BLUE);
		            	
		                for (int i = 0; i < faces.length; i++) {
		                	if (faces[i] != null) {
				            	PointF midPoint = new PointF();
				            	
				            	float eyeDistance = faces[i].eyesDistance();
				            	faces[i].getMidPoint(midPoint);
				            	
				            	// Paint over face
				            	canvas.drawRect(midPoint.x-eyeDistance*2, midPoint.x-eyeDistance*2, midPoint.x+eyeDistance*2, midPoint.x+eyeDistance*2, paint);
		                	}
		                }
		            }				
					
					// Display it
					imv.setImageBitmap(alteredBitmap);
		
					takePictureButton.setVisibility(View.GONE);
					choosePictureButton.setVisibility(View.GONE);
					savePictureButton.setVisibility(View.VISIBLE);
					cancelButton.setVisibility(View.VISIBLE);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}	

	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			downx = event.getX();
			downy = event.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			paint.setColor(Color.GREEN);
			canvas.drawRect(downx, downy, event.getX(), event.getY(), paint);
			imv.invalidate();
			break;
		case MotionEvent.ACTION_UP:
			paint.setColor(Color.GREEN);
			upx = event.getX();
			upy = event.getY();
			canvas.drawRect(downx, downy, upx, upy, paint);
			imv.invalidate();
			break;
		case MotionEvent.ACTION_CANCEL:
			break;
		default:
			break;
		}
		return true;
	}
}