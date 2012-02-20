package org.witness.informa;

import info.guardianproject.database.sqlcipher.SQLiteDatabase;

import java.io.File;
import java.util.ArrayList;

import org.witness.informa.utils.InformaConstants;
import org.witness.informa.utils.InformaConstants.Keys;
import org.witness.informa.utils.InformaConstants.Keys.Image;
import org.witness.informa.utils.InformaConstants.Keys.Tables;
import org.witness.informa.utils.io.DatabaseHelper;
import org.witness.informa.utils.secure.Apg;
import org.witness.securesmartcam.utils.ObscuraConstants;
import org.witness.sscphase1.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ReviewAndFinish extends Activity implements OnClickListener {
	Button confirmView, confirmQuit, confirmTakeAnother;
	Uri savedImageUri;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.reviewandfinish);
		savedImageUri = getIntent().getData();
		
		confirmView = (Button) findViewById(R.id.informaConfirm_btn_view);
		confirmView.setOnClickListener(this);
		
		confirmQuit = (Button) findViewById(R.id.informaConfirm_btn_quit);
		confirmQuit.setOnClickListener(this);
		
		confirmTakeAnother = (Button) findViewById(R.id.informaConfirm_btn_takeAnother);
		confirmTakeAnother.setOnClickListener(this);
		
		new Thread(new Runnable() {
			ArrayList<File> images = new ArrayList<File>();
			String matchPath = pullPathFromUri(savedImageUri).getName();
			private SharedPreferences _sp;
			private DatabaseHelper dh;
			private SQLiteDatabase db;
			private Apg apg;
			
			@Override
			public void run() {
				// get the files that match the uri
				Log.d(InformaConstants.TAG, "here is the original path:" + matchPath);
				_sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				dh = new DatabaseHelper(getApplicationContext());
				db = dh.getReadableDatabase(_sp.getString(Keys.Settings.HAS_DB_PASSWORD, ""));
				
				dh.setTable(db, Tables.IMAGE_REGIONS);
				Cursor ir = dh.getValue(db, new String[] {Keys.ImageRegion.DATA}, Keys.ImageRegion.BASE, matchPath);
				if(ir != null) {
					ir.moveToFirst();
					while(!ir.isAfterLast()) {
						byte[] regionData = ir.getBlob(ir.getColumnIndex(Keys.ImageRegion.DATA));
					}
				} else
					throw new NullPointerException("sorry, npe");
				
				dh.setTable(db, Tables.IMAGES);
				Cursor c = dh.getValue(db, new String[] {Keys.Intent.Destination.EMAIL, Keys.Image.LOCATION_OF_OBSCURED_VERSION}, Keys.Image.LOCATION_OF_ORIGINAL, pullPathFromUri(savedImageUri).getAbsolutePath());
				if(c != null) {
					c.moveToFirst();
					while(!c.isAfterLast()) {
						String email = c.getString(c.getColumnIndex(Keys.Intent.Destination.EMAIL));
						String filePath = c.getString(c.getColumnIndex(Keys.Image.LOCATION_OF_OBSCURED_VERSION));
						
						// encrypt them
						
						c.moveToNext();
					}
				} else
					throw new NullPointerException("sorry, npe");
					
				
				
				
			}
			
		}).start();
		
	}
	
    private void viewImage() {
    	
    	Intent iView = new Intent(Intent.ACTION_VIEW);
    	iView.setType(ObscuraConstants.MIME_TYPE_JPEG);
    	iView.putExtra(Intent.EXTRA_STREAM, savedImageUri);
    	iView.setDataAndType(savedImageUri, ObscuraConstants.MIME_TYPE_JPEG);

    	startActivity(Intent.createChooser(iView, "View Image"));
    	finish();
	
    }
    
    public File pullPathFromUri(Uri uri) {

    	String originalImageFilePath = null;

    	if (uri.getScheme() != null && uri.getScheme().equals("file"))
    	{
    		originalImageFilePath = uri.toString();
    	}
    	else
    	{
	    	String[] columnsToSelect = { MediaStore.Images.Media.DATA };
	    	Cursor imageCursor = getContentResolver().query(uri, columnsToSelect, null, null, null );
	    	if ( imageCursor != null && imageCursor.getCount() == 1 ) {
		        imageCursor.moveToFirst();
		        originalImageFilePath = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
	    	}
    	}

    	return new File(originalImageFilePath);
    }
    


	@Override
	public void onClick(View v) {
		if(v == confirmView) {
			viewImage();
		} else if(v == confirmQuit) {
			finish();
		} else if(v == confirmTakeAnother) {
			// relaunch informa...
		}
		
	}
}
