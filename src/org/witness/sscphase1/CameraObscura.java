package org.witness.sscphase1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.CompressFormat;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class CameraObscura extends Activity implements OnClickListener, OnTouchListener {
    
	final static String LOGTAG = "[Camera Obscura : Camera Obscura] ***************************";
		
	final static int CAMERA_RESULT = 0;
	final static int GALLERY_RESULT = 1;
		
	final static int OBSCURE_SQUARE = 0;
	final static int OBSCURE_BLUR = 1;
	
	int obscureMethod = OBSCURE_SQUARE;
	
	public final static int PREFERENCES_MENU_ITEM = 0;
	public final static int PANIC_MENU_ITEM = 1;

	ImageView imv;
	Button choosePictureButton, takePictureButton;
	Button savePictureButton, cancelButton;
	Button eraseMetaDataButton;

	Uri originalImageUri;
	
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
	
	GoogleFaceDetection gfd;
	
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
    	
    	eraseMetaDataButton = (Button) this.findViewById(R.id.EraseMetaDataButton);
    	eraseMetaDataButton.setOnClickListener(this);
    	
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
			eraseMetaDataButton.setVisibility(View.GONE);
			takePictureButton.setVisibility(View.VISIBLE);
			choosePictureButton.setVisibility(View.VISIBLE);
		} else if (v == cancelButton) {
			cancelButton.setVisibility(View.GONE);
			savePictureButton.setVisibility(View.GONE);
			eraseMetaDataButton.setVisibility(View.GONE);
			takePictureButton.setVisibility(View.VISIBLE);
			choosePictureButton.setVisibility(View.VISIBLE);
		} else if (v == eraseMetaDataButton) {
			if (originalImageUri != null) {
				try {
					EXIFWiper ew = new EXIFWiper(originalImageUri.getPath());
					ew.wipeIt();
					// Notify user
					Toast t = Toast.makeText(this, "MetaData Erased In Original", Toast.LENGTH_SHORT);
					t.show();
				} catch (IOException ioe) {
					Toast t = Toast.makeText(this, "Error", Toast.LENGTH_SHORT);
					t.show();
					ioe.printStackTrace();
				}
			}
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (resultCode == RESULT_OK) {
			if (requestCode == GALLERY_RESULT || requestCode == CAMERA_RESULT) 
			{
				Log.v(LOGTAG,"Coming back from :" + requestCode);
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
					gfd = new GoogleFaceDetection(bmp);
					int numFaces = gfd.findFaces();
		            Log.v(LOGTAG,"Num Faces Found: " + numFaces); 
		            Rect[] faceRects = gfd.getFaces();

		            // Using the interface
		            ObscureMethod om;
		            
		            // Load the appropriate class/method based on obscureMethod variable/constants
		            if (obscureMethod == OBSCURE_BLUR) {
		            	om = new BlurObscure(alteredBitmap);
		            } else {
		            	om = new PaintSquareObscure();		            	
		            }
		            
		            if (numFaces > 0) {
		                for (int i = 0; i < faceRects.length; i++) {				            	
			            	// Apply the obscure method
		                	om.obscureRect(faceRects[i], canvas);
		                }
		            }				
					
					// Display it
					imv.setImageBitmap(alteredBitmap);
		
					takePictureButton.setVisibility(View.GONE);
					choosePictureButton.setVisibility(View.GONE);
					savePictureButton.setVisibility(View.VISIBLE);
					cancelButton.setVisibility(View.VISIBLE);
					eraseMetaDataButton.setVisibility(View.VISIBLE);

				} catch (FileNotFoundException e) {
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
	
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuItem panicMenuItem = menu.add(Menu.NONE, PANIC_MENU_ITEM, Menu.NONE, "Panic");
        MenuItem preferencesMenuItem = menu.add(Menu.NONE, PREFERENCES_MENU_ITEM, Menu.NONE, "Preferences");
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
        	case PREFERENCES_MENU_ITEM:
        	 	// Load Preferences Activity
        		Intent intent = new Intent(this, PreferencesActivity.class);
        		startActivity(intent);	
        		return true;
        	case PANIC_MENU_ITEM:
        		// Look up preferences and do what is required
    		default:
    			return false;
    	}
    }
}