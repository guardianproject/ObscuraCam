package org.witness.sscphase1;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class CameraObscuraMainMenu extends Activity implements OnClickListener {
	    
		final static String LOGTAG = "[Camera Obscura : CameraObscuraMainMenu] **********************************";
			
		final static int CAMERA_RESULT = 0;
		final static int GALLERY_RESULT = 1;
		final static int IMAGE_EDITOR = 2;
					
		public final static int PREFERENCES_MENU_ITEM = 0;
		public final static int PANIC_MENU_ITEM = 1;

		Button choosePictureButton, takePictureButton, datastoreButton;
		//Button , preferencesButton, panicButton;
		
		
		File tmpImageFile;
		int imageSource;
				
		boolean logBT,logGeo,logAcc;
		private final boolean ALLOWED = true;
		private final boolean NOT_ALLOWED = false;
		
		Intent ss;
		
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        	        
	        setContentView(R.layout.mainmenu);
	        
	    	choosePictureButton = (Button) this.findViewById(R.id.ChoosePictureButton);
	    	choosePictureButton.setOnClickListener(this);
	    	
	    	takePictureButton = (Button) this.findViewById(R.id.TakePictureButton);
	    	takePictureButton.setOnClickListener(this);
	    	
	    	datastoreButton = (Button) this.findViewById(R.id.DatastoreButton);
	    	datastoreButton.setOnClickListener(this);
	    	
	    	//we don't need this as they are menu options
	    	/*
	    	preferencesButton = (Button) this.findViewById(R.id.PreferencesButton);
	    	preferencesButton.setOnClickListener(this);
	    	
	    	panicButton = (Button) this.findViewById(R.id.PanicButton);
	    	panicButton.setOnClickListener(this);
	    	*/
	    	
	    	/*
	    	 * TODO: parse preference file for user's stance on sensor logging
	    	 * for now, i set them all as "allowed"
	    	 */
	    	logBT = logGeo = logAcc = ALLOWED;
	    	
	    	// start service for sensor reading
	    	ss = new Intent(this,SSCSensorSucker.class);
	    	ss.putExtra("logBT", logBT);
	    	ss.putExtra("logAcc", logAcc);
	    	ss.putExtra("logGeo", logGeo);
	    	//startService(ss);
	    }

		public void onClick(View v) {
			if (v == choosePictureButton) {
				
				// Only from the Memory Card.
				// INTERNAL_CONTENT_URI from the normal memory space
				Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(i, GALLERY_RESULT);
				
			} else if (v == takePictureButton) {
				
				// This should be obscured/hidden/encrypted etc.
		    	tmpImageFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/myfavoritepicture.jpg");

				// Add in choice to capture video
		    	
				Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
				i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(tmpImageFile));
				startActivityForResult(i, CAMERA_RESULT);

				takePictureButton.setVisibility(View.VISIBLE);
				choosePictureButton.setVisibility(View.VISIBLE);
				
			} else if (v == datastoreButton) {
				
				Toast dataStoreToast = Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT);
				dataStoreToast.show();
				
			} 
			/*else if (v == preferencesButton) {

				showPreferences();
				
			} else if (v == panicButton) {

				panic();				
			}*/
		}

		protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
			super.onActivityResult(requestCode, resultCode, intent);

			if (resultCode == RESULT_OK) {
				if (requestCode == GALLERY_RESULT || requestCode == CAMERA_RESULT) {
					Uri imageFileUri;
					if (requestCode == CAMERA_RESULT) {
						imageFileUri = Uri.fromFile(tmpImageFile);
						imageSource = 1;
						// TODO: Might we also have to pass some sensor data here? 
					} else { //if (requestCode == GALLERY_RESULT) {
						imageFileUri = intent.getData();
						imageSource = 2;
					}
					Log.v(LOGTAG,imageFileUri.toString());					
					
					Intent passingIntent = new Intent(this,ImageEditor.class);
					passingIntent.setData(imageFileUri);
					passingIntent.putExtra("imageSource",imageSource);
					startActivityForResult(passingIntent, IMAGE_EDITOR);					
				}
			}
		}	

	    @Override
	    public boolean onCreateOptionsMenu(Menu menu) {
	    	
	       	MenuInflater inflater = getMenuInflater();
	        inflater.inflate(R.menu.main_menu, menu);
	        /*
	    	MenuItem panicMenuItem = menu.add(Menu.NONE, PANIC_MENU_ITEM, Menu.NONE, "Panic");
	        MenuItem preferencesMenuItem = menu.add(Menu.NONE, PREFERENCES_MENU_ITEM, Menu.NONE, "Preferences");
	        */
	        return true;
	    }
	    
	    public boolean onOptionsItemSelected(MenuItem item) {
	    	switch (item.getItemId()) {
	        	case R.id.menu_prefs:
	        		// Load Preferences Activity
	        		showPreferences();
	        		return true;
	        	case R.id.menu_panic:
	        		// Look up preferences and do what is required
	        		panic();
	        		return true;
	        	default:
	    			return false;
	    	}
	    }
	    
	    @Override
	    protected void onDestroy() {
	    	super.onDestroy();
	    	stopService(ss);
	    }
	    
	    @Override
	    public void onRestoreInstanceState(Bundle savedInstanceState) {
	        if (savedInstanceState.containsKey("tmpImageFile")) {
	        	tmpImageFile = (File) savedInstanceState.getSerializable("tmpImageFile");
	        }
	    }
	    
	    @Override
	    public void onSaveInstanceState(Bundle savedInstanceState) {
	      if (tmpImageFile != null) {
	    	  savedInstanceState.putSerializable("tmpImageFile", tmpImageFile);
	      }
	      super.onSaveInstanceState(savedInstanceState);
	    }	   
	    
	    public void showPreferences() {
    	 	// Load Preferences Activity
    		Intent intent = new Intent(this, PreferencesActivity.class);
    		startActivity(intent);		    	
	    }
	    
	    public void panic() {
    		// Look up preferences and do what is required

			Toast panicButtonToast = Toast.makeText(this, "Not Implemented Yet", Toast.LENGTH_SHORT);
			panicButtonToast.show();
	    }
}
