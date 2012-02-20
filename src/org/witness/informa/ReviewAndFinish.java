package org.witness.informa;

import info.guardianproject.database.sqlcipher.SQLiteDatabase;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.witness.informa.utils.InformaConstants;
import org.witness.informa.utils.InformaConstants.Keys;
import org.witness.informa.utils.InformaConstants.Keys.Tables;
import org.witness.informa.utils.io.DatabaseHelper;
import org.witness.informa.utils.secure.Apg;
import org.witness.securesmartcam.utils.ObscuraConstants;
import org.witness.sscphase1.ObscuraApp;
import org.witness.sscphase1.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ReviewAndFinish extends Activity implements OnClickListener {
	Button confirmView, confirmQuit, confirmTakeAnother;
	Uri savedImageUri;
	Handler finish;
	Apg apg;
	SharedPreferences _sp;
	
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
		
		_sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		finish = new Handler();
		
		new Thread(new Runnable() {
			ArrayList<File> imagesRegionFiles = new ArrayList<File>();
			String matchPath = pullPathFromUri(savedImageUri).getName();
			FileOutputStream fos;
			ZipOutputStream zos;
			
			private DatabaseHelper dh;
			private SQLiteDatabase db;			
			
			private void addToZip(File file) throws IOException {
				byte[] buffer = new byte[(int) file.length()];
				
				FileInputStream fis = new FileInputStream(file);
				zos.putNextEntry(new ZipEntry(file.getAbsolutePath()));
				
				int bytesRead = 0;
				while((bytesRead = fis.read(buffer)) > 0)
					zos.write(buffer, 0, bytesRead);
				zos.closeEntry();
				fis.close();
			}
			
			@Override
			public void run() {
				// get the files that match the uri
				Log.d(InformaConstants.TAG, "here is the original path:" + matchPath);
				dh = new DatabaseHelper(getApplicationContext());
				db = dh.getReadableDatabase(_sp.getString(Keys.Settings.HAS_DB_PASSWORD, ""));
				
				int count = 0;
				
				dh.setTable(db, Tables.IMAGE_REGIONS);
				Cursor ir = dh.getValue(db, new String[] {Keys.ImageRegion.DATA}, Keys.ImageRegion.BASE, matchPath);
				if(ir != null) {
					
					ir.moveToFirst();
					while(!ir.isAfterLast()) {
						byte[] regionData = ir.getBlob(ir.getColumnIndex(Keys.ImageRegion.DATA));
						Log.d(InformaConstants.TAG, "got bytes: " + regionData.length);
						
						try {
							File byteFile = new File(InformaConstants.DUMP_FOLDER, "ir" + count + ".informaRegion");
							FileOutputStream fos = new FileOutputStream(byteFile);
							fos.write(regionData);
							fos.close();
							imagesRegionFiles.add(byteFile);
							count++;
						} catch (FileNotFoundException e) {
							Log.d(InformaConstants.TAG, "file not found: " + e);
						} catch (IOException e) {
							Log.d(InformaConstants.TAG, "IOError: " + e);
						}
						ir.moveToNext();
					}
				} else
					throw new NullPointerException("sorry, npe");
				
				ir.close();
				
				dh.setTable(db, Tables.IMAGES);
				Cursor c = dh.getValue(db, new String[] {Keys.Intent.Destination.EMAIL, Keys.Image.LOCATION_OF_OBSCURED_VERSION}, Keys.Image.LOCATION_OF_ORIGINAL, pullPathFromUri(savedImageUri).getAbsolutePath());
				if(c != null) {
					c.moveToFirst();
					while(!c.isAfterLast()) {
						final String email = c.getString(c.getColumnIndex(Keys.Intent.Destination.EMAIL));
						String filePath = c.getString(c.getColumnIndex(Keys.Image.LOCATION_OF_OBSCURED_VERSION));
						File sourceImage = new File(filePath);
						
						try {
							final File informa = new File(filePath.substring(0, filePath.length() - 4) + ".informa");
							fos = new FileOutputStream(informa);
							zos = new ZipOutputStream(fos);
							
							addToZip(sourceImage);
							
							for(File f : imagesRegionFiles) {
								addToZip(f);
							}
							
							zos.close();
							fos.close();
							
							finish.post(new Runnable() {
								@Override
								public void run() {
									runEncrypt(informa, email);
								}
							});
							
						} catch (FileNotFoundException e) {
							Log.d(InformaConstants.TAG, "file not found: " + e);
						} catch (IOException e) {
							Log.d(InformaConstants.TAG, "IOError: " + e);
						}
						
						c.moveToNext();
					}
				} else
					throw new NullPointerException("sorry, npe");
				
				c.close();
				
				db.close();
				dh.close();
			}
			
		}).start();
		
	}
	
	private void runEncrypt(File file, String email) {
		apg = Apg.getInstance();
		apg.setSignatureKeyId(_sp.getLong(Keys.Owner.SIG_KEY_ID, 0));
		
		apg.setEncryptionKeys(apg.getSecretKeyIdsFromEmail(getApplicationContext(), email));
		apg.encryptFile(this, file);
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
    
    private void doCleanup() {
    	File informaDir = new File(InformaConstants.DUMP_FOLDER);
    	FileFilter cleanup = new FileFilter() {
    		String[] extensions = new String[] {"jpg","informaregion"};
			@Override
			public boolean accept(File file) {
				Log.d(InformaConstants.TAG, file.getName().toLowerCase());
				
				for(String e : extensions)
					if(file.getName().toLowerCase().endsWith(e))
						return true;
						
				return false;
			}
    		
    	};
    	
    	for(File f : informaDir.listFiles(cleanup)) {
    		f.delete();
    	}
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	Log.d(InformaConstants.TAG, "APG CODE: " + requestCode);
    	if(requestCode == Apg.ENCRYPT_MESSAGE) {
    		apg.onActivityResult(this, requestCode, resultCode, data);
    		doCleanup();
    	}
    }

	@Override
	public void onClick(View v) {
		if(v == confirmView) {
			viewImage();
		} else if(v == confirmQuit) {			
			startActivity(new Intent(this, ObscuraApp.class).putExtra(Keys.Service.FINISH_ACTIVITY, "die"));
			finish();
		} else if(v == confirmTakeAnother) {
			startActivity(new Intent(this, ObscuraApp.class).putExtra(Keys.Service.START_SERVICE, "go"));
		}
		
	}
}
