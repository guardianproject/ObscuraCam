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
        	        
        setContentView(R.layout.mainmenu);
        
    	choosePictureButton = (Button) this.findViewById(R.id.ChoosePictureButton);
    	choosePictureButton.setOnClickListener(this);
    	
    	takePictureButton = (Button) this.findViewById(R.id.TakePictureButton);
    	takePictureButton.setOnClickListener(this);
    }

	public void onClick(View v) {
		if (v == choosePictureButton) {
			
			Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(i, GALLERY_RESULT);
			
		} else if (v == takePictureButton) {
			
			// Create the Uri, this should put it in the gallery, is this desired?
			imageFileUri = getContentResolver().insert(
					Media.EXTERNAL_CONTENT_URI, new ContentValues());
	    	
			Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
			i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageFileUri);
			startActivityForResult(i, CAMERA_RESULT);

			takePictureButton.setVisibility(View.VISIBLE);
			choosePictureButton.setVisibility(View.VISIBLE);
		} 
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (resultCode == RESULT_OK) {
			if (requestCode == GALLERY_RESULT || requestCode == CAMERA_RESULT) {
									
				if (requestCode == GALLERY_RESULT) {
					imageFileUri = intent.getData();
				}
				
				// This comes back null if we are rotated as the activity is restarted
				// Let's lock in portrait for now
				Log.v(LOGTAG,"Sending: " + imageFileUri.toString());					
				
				Intent passingIntent = new Intent(this,ImageEditor.class);
				passingIntent.setData(imageFileUri);
				startActivityForResult(passingIntent, IMAGE_EDITOR);					
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
     * Handling screen configuration changes ourselves, we don't want the activity to restart on rotation
     */
    @Override
    public void onConfigurationChanged(Configuration conf) 
    {
        super.onConfigurationChanged(conf);
    }
}
