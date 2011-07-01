package org.witness.sscphase1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CameraObscuraMainMenu extends Activity implements OnClickListener {
	    
	final static String LOGTAG = "[Camera Obscura : CameraObscuraMainMenu]";
		
	final static int CAMERA_RESULT = 0;
	final static int GALLERY_RESULT = 1;
	final static int IMAGE_EDITOR = 2;
	
	final static int ABOUT = 0;
	
	Button choosePictureButton, takePictureButton;		
	
	Uri imageFileUri;
					
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setLayout();
    }
    
    private void setLayout() {
        setContentView(R.layout.mainmenu);
        
    	choosePictureButton = (Button) this.findViewById(R.id.ChoosePictureButton);
    	choosePictureButton.setOnClickListener(this);
    	
    	takePictureButton = (Button) this.findViewById(R.id.TakePictureButton);
    	takePictureButton.setOnClickListener(this);
    }

    // We get here after someone clicked the choose/take picture buttons.
	public void onClick(View v) {
		if (v == choosePictureButton) {
			Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(i, GALLERY_RESULT);
		} else if (v == takePictureButton) {
			// Create the Uri, this should put it in the gallery, is this desired?
			imageFileUri = getContentResolver().insert(
					android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
	    	
			// The caller may pass an extra EXTRA_OUTPUT to control where this image will be written. 
			// If the EXTRA_OUTPUT is not present, then a small sized image is returned as a Bitmap object 
			// in the extra field. This is useful for applications that only need a small image. 
			// If the EXTRA_OUTPUT is present, then the full-sized image will be written to the Uri value of EXTRA_OUTPUT.
			// Note that there is a known bug that the image gets stored twice.
			// http://stackoverflow.com/questions/6341329/built-in-camera-using-the-extra-mediastore-extra-output-stores-pictures-twice-i
			// It's not clear how to get a handle to both of them so we can delete one and handle the other.
			Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
			i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageFileUri);
			startActivityForResult(i, CAMERA_RESULT);

			takePictureButton.setVisibility(View.VISIBLE);
			choosePictureButton.setVisibility(View.VISIBLE);
		} 
	}

	// On returning from choose/take a picture activity, we land here.
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (resultCode == RESULT_OK) {
			if (requestCode == GALLERY_RESULT || requestCode == CAMERA_RESULT) {
				if (requestCode == GALLERY_RESULT) {
					// If we didn't provide an extra_output to the camera app,
					// the below code will give us a small version of the image
					// http://code.google.com/p/android/issues/detail?id=1480
					imageFileUri = intent.getData();
				}
				
				// This comes back null if we are rotated as the activity is restarted
				// Let's lock in portrait for now
				if (imageFileUri != null)
				{
					Log.v(LOGTAG,"Sending: " + imageFileUri.toString());	
					Intent passingIntent = new Intent(this,ImageEditor.class);
					passingIntent.setData(imageFileUri);
					startActivityForResult(passingIntent, IMAGE_EDITOR);
				} else {
					Log.e(LOGTAG, "imageFileUri is null");										
				}
			}
		}
	}	

	/*
	 * Display the about screen
	 */
	private void displayAbout() {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse("https://guardianproject.info/apps/securecam/about-v1/"));
		startActivity(i);
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuItem aboutMenuItem = menu.add(Menu.NONE, ABOUT, Menu.NONE, "About");
    	aboutMenuItem.setIcon(R.drawable.ic_menu_about);
    	
    	return true;
	}
	
    public boolean onOptionsItemSelected(MenuItem item) {	
        switch (item.getItemId()) {
        	case ABOUT:
        		displayAbout();
        		return true;
        		
        	default:
        		
        		return false;
        }
    }
    
	
    /*
     * Handling screen configuration changes ourselves, 
     * we don't want the activity to restart on rotation
     */
    @Override
    public void onConfigurationChanged(Configuration conf) 
    {
        super.onConfigurationChanged(conf);
        // Reset the layout to use the landscape config
        setLayout();
    }
}
