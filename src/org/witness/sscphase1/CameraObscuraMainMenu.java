package org.witness.sscphase1;

import java.io.File;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class CameraObscuraMainMenu extends Activity implements OnClickListener {
	    
	final static String LOGTAG = "[Camera Obscura : CameraObscuraMainMenu]";
		
	final static int CAMERA_RESULT = 0;
	final static int GALLERY_RESULT = 1;
	final static int IMAGE_EDITOR = 2;
	
	final static int ABOUT = 0;
	
	final static String CAMERA_TMP_FILE = "tmp.jpg";
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		deleteTmpFile();
		
	}
	
	private void deleteTmpFile ()
	{
		File fileDir = getExternalFilesDir(null);
		
		if (fileDir == null || !fileDir.exists())
			fileDir = getFilesDir();
		
		File tmpFile = new File(fileDir,CAMERA_TMP_FILE);
		if (tmpFile.exists())
			tmpFile.delete();
	}


	Button choosePictureButton, takePictureButton;		
	
	Uri imageFileUri;
					
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setLayout();
        
        deleteTmpFile();
    }
    
    private void setLayout() {
        setContentView(R.layout.mainmenu);
        
    	choosePictureButton = (Button) this.findViewById(R.id.ChoosePictureButton);
    	choosePictureButton.setOnClickListener(this);
    	
    	takePictureButton = (Button) this.findViewById(R.id.TakePictureButton);
    	takePictureButton.setOnClickListener(this);
    }

	public void onClick(View v) {
		if (v == choosePictureButton) 
		{
			
			try
			{
				Intent intent = new Intent(Intent.ACTION_PICK);
				intent.setType("image/*"); //limit to image types for now
				startActivityForResult(intent, GALLERY_RESULT);
				
			}
			catch (Exception e)
			{
				Toast.makeText(this, "Unable to open Gallery app", Toast.LENGTH_LONG).show();
				Log.e(LOGTAG, "error loading gallery app to choose photo: " + e.getMessage(), e);
			}
			
		} else if (v == takePictureButton) {
			
			File fileDir = getExternalFilesDir(null);
			
			if (fileDir == null || !fileDir.exists())
				fileDir = getFilesDir();
			
			imageFileUri = Uri.fromFile( new File(fileDir,CAMERA_TMP_FILE));

			Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
			//i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageFileUri);
			i.putExtra(MediaStore.EXTRA_OUTPUT, imageFileUri); // URI of the file where pic will be stored

			startActivityForResult(i, CAMERA_RESULT);

			takePictureButton.setVisibility(View.VISIBLE);
			choosePictureButton.setVisibility(View.VISIBLE);
		} 
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		
		if (requestCode == GALLERY_RESULT) 
		{
			if (intent != null)
			{
				imageFileUri = intent.getData();
					
				if (imageFileUri != null)
				{
					Intent passingIntent = new Intent(this,ImageEditor.class);
					passingIntent.setData(imageFileUri);
					startActivityForResult(passingIntent, IMAGE_EDITOR);
				}
				else
				{
					Toast.makeText(this, "Unable to load photo.", Toast.LENGTH_LONG).show();

				}
			}
			else
			{
				Toast.makeText(this, "Unable to load photo.", Toast.LENGTH_LONG).show();

			}
				
		}
		else if (requestCode == CAMERA_RESULT)
		{
			File fileDir = getExternalFilesDir(null);
			
			if (fileDir == null || !fileDir.exists())
				fileDir = getFilesDir();
			
			File fileTmp = new File(fileDir,CAMERA_TMP_FILE);
			
			if (fileTmp.exists())
			{
				imageFileUri = Uri.fromFile(fileTmp);
				Intent passingIntent = new Intent(this,ImageEditor.class);
				passingIntent.setData(imageFileUri);
				startActivity(passingIntent);
			}
			else
			{
				Toast.makeText(this, "Unable to load photo.", Toast.LENGTH_LONG).show();
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
