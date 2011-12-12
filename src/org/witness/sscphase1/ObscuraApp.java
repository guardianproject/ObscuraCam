package org.witness.sscphase1;


import java.io.File;
import java.util.Date;

import org.witness.informa.InformaPreferences;
import org.witness.informa.utils.SensorSucker;
import org.witness.informa.utils.SensorSucker.LocalBinder;
import org.witness.securesmartcam.ImageEditor;
import org.witness.sscphase1.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class ObscuraApp extends Activity implements OnClickListener {
	    
	public final static String TAG = "SSC";
	public final static String STOP_SUCKING = "stopSensors";
	public static final String SET_CURRENT = "setCurrent";
	public final static String SEAL_LOG = "sealLog";
	
	public final static class CAPTURE_EVENTS {
		public static final int MediaCaptured = 5;
		public static final int MediaSaved = 6;
		public static final int RegionGenerated = 7;
		public static final int ExifReported = 8;
	};
	
	public final static class LOCATION_TYPES {
		public static final int LocationOnMediaCaptured = 9;
		public static final int LocationOnMediaSaved = 10;
		public static final int LocationOnGeneration = 11;
	}
		
	final static int CAMERA_RESULT = 0;
	final static int GALLERY_RESULT = 1;
	final static int IMAGE_EDITOR = 2;
	
	final static int ABOUT = 0;
	
	final static String CAMERA_TMP_FILE = "ssctmp.jpg";
	
	SensorSucker mSensorSucker;

	private Button choosePictureButton, takePictureButton;		
	
	private Uri uriCameraImage = null;
	
	private ServiceConnection sc = new ServiceConnection() {
		public void onServiceConnected(ComponentName cn, IBinder binder) {
			LocalBinder lb = (LocalBinder) binder;
			mSensorSucker = lb.getService();
		}

		public void onServiceDisconnected(ComponentName cn) {}
	};
	
	private SharedPreferences _sp;
	
	@Override
	protected void onDestroy() 
	{
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setLayout();
        deleteTmpFile();
        
        _sp = PreferenceManager.getDefaultSharedPreferences(this);
        if(
        	_sp.getString("informaPref_dbpw", "").compareTo("") == 0 ||
        	_sp.getString("informaPref_destKeys", "").compareTo("") == 0
        )
        	startActivity(new Intent(this, InformaPreferences.class));	
    }
    
    @Override
	protected void onResume() {

		super.onResume();
		Intent startSensorSucker = new Intent(this,SensorSucker.class);
        bindService(startSensorSucker,sc,Context.BIND_AUTO_CREATE);
	
	}
    
    @Override
    protected void onPause() {
    	super.onPause();    	
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
				 setContentView(R.layout.mainloading);
				Intent intent = new Intent(Intent.ACTION_PICK);
				intent.setType("image/*"); //limit to image types for now
				startActivityForResult(intent, GALLERY_RESULT);
				
			}
			catch (Exception e)
			{
				Toast.makeText(this, "Unable to open Gallery app", Toast.LENGTH_LONG).show();
				Log.e(TAG, "error loading gallery app to choose photo: " + e.getMessage(), e);
			}
			
		} else if (v == takePictureButton) {
			
			setContentView(R.layout.mainloading);
			
			String storageState = Environment.getExternalStorageState();
	        if(storageState.equals(Environment.MEDIA_MOUNTED)) {

	          
	            ContentValues values = new ContentValues();
	          
	            values.put(MediaStore.Images.Media.TITLE, CAMERA_TMP_FILE);
	      
	            values.put(MediaStore.Images.Media.DESCRIPTION,"ssctmp");

	            File tmpFileDirectory = new File(Environment.getExternalStorageDirectory().getPath() + ImageEditor.TMP_FILE_DIRECTORY);
	            if (!tmpFileDirectory.exists())
	            	tmpFileDirectory.mkdirs();
	            
	            File tmpFile = new File(tmpFileDirectory,"cam" + ImageEditor.TMP_FILE_NAME);
	        	
	        	uriCameraImage = Uri.fromFile(tmpFile);
	            //uriCameraImage = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

	            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE );
	            intent.putExtra( MediaStore.EXTRA_OUTPUT, uriCameraImage);
	            
	            startActivityForResult(intent, CAMERA_RESULT);
	        }   else {
	            new AlertDialog.Builder(ObscuraApp.this)
	            .setMessage("External Storeage (SD Card) is required.\n\nCurrent state: " + storageState)
	            .setCancelable(true).create().show();
	        }
	        
			takePictureButton.setVisibility(View.VISIBLE);
			choosePictureButton.setVisibility(View.VISIBLE);
		} 
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		
		if (resultCode == RESULT_OK)
		{
			setContentView(R.layout.mainloading);
			
			if (requestCode == GALLERY_RESULT) 
			{
				if (intent != null)
				{
					Uri uriGalleryImage = intent.getData();
						
					if (uriGalleryImage != null)
					{
						Intent passingIntent = new Intent(this,ImageEditor.class);
						passingIntent.setData(uriGalleryImage);
						startActivityForResult(passingIntent,IMAGE_EDITOR);
						
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
				//Uri uriCameraImage = intent.getData();
				if (uriCameraImage != null)
				{
					sendBroadcast(new Intent()
						.setAction(ObscuraApp.SET_CURRENT)
						.putExtra("timestampToMatch", new Date().getTime())
						.putExtra("captureEvent", ObscuraApp.CAPTURE_EVENTS.MediaCaptured));
					
					Intent passingIntent = new Intent(this,ImageEditor.class);
					passingIntent.setData(uriCameraImage);
					startActivityForResult(passingIntent,IMAGE_EDITOR);
				}
				else
				{
					takePictureButton.setVisibility(View.VISIBLE);
					choosePictureButton.setVisibility(View.VISIBLE);
				}
			}
		}
		else
			setLayout();
		
		
		
	}	

	/*
	 * Display the about screen
	 */
	private void displayAbout() {
		
		StringBuffer msg = new StringBuffer();
		
		msg.append(getString(R.string.app_name));
		
        String versNum = "";
        
        try {
            String pkg = getPackageName();
            versNum = getPackageManager().getPackageInfo(pkg, 0).versionName;
        } catch (Exception e) {
        	versNum = "";
        }
        
        msg.append(" v" + versNum);
        msg.append('\n');
        msg.append('\n');
        
        msg.append(getString(R.string.about));
	        
        msg.append('\n');
        msg.append('\n');
        
        msg.append(getString(R.string.about2));
        
        msg.append('\n');
        msg.append('\n');
        
        msg.append(getString(R.string.about3));
        
		showDialog(msg.toString());
	}
	
	private void showDialog (String msg)
	{
		 new AlertDialog.Builder(this)
         .setTitle(getString(R.string.app_name))
         .setMessage(msg)
         .create().show();
	}


	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		
		String aboutString = "About ObscuraCam";
		
    	MenuItem aboutMenuItem = menu.add(Menu.NONE, ABOUT, Menu.NONE, aboutString);
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
