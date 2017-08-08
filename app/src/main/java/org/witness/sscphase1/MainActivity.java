package org.witness.sscphase1;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.witness.securesmartcam.AlbumLayoutManager;
import org.witness.securesmartcam.AlbumsActivity;
import org.witness.securesmartcam.ImageEditor;
import org.witness.securesmartcam.adapters.AskForPermissionAdapter;
import org.witness.securesmartcam.adapters.GalleryCursorRecyclerViewAdapter;
import org.witness.ssc.video.VideoEditor;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GalleryCursorRecyclerViewAdapter.GalleryCursorRecyclerViewAdapterListener {
	    
	public final static String TAG = "SSC";

	private static final int READ_EXTERNAL_STORAGE_PERMISSION_REQUEST = 1;
	private static final int CAPTURE_IMAGE_REQUEST = 2;
	private static final int SELECT_FROM_ALBUMS_REQUEST = 3;

	private static final String CAMERA_CAPTURE_DIRNAME = "camera";
	private static final String CAMERA_CAPTURE_FILENAME = "cameracapture";

	final static int CAMERA_RESULT = 0;
	final static int GALLERY_RESULT = 1;
	final static int IMAGE_EDITOR = 2;
	final static int VIDEO_EDITOR = 3;
	final static int ABOUT = 0;
	
	private RecyclerView recyclerViewPhotos;
	private View layoutGalleryInfo;
	private AlbumLayoutManager layoutManagerPhotos;

	@Override
	protected void onDestroy() 
	{
		super.onDestroy();	
		deleteTmpFile();
		
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
        deleteTmpFile();
        Eula.show(this);

		layoutGalleryInfo = findViewById(R.id.gallery_info);
		Button btnOk = (Button) layoutGalleryInfo.findViewById(R.id.btnGalleryInfoOk);
		btnOk.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//TODO App.getInstance().getSettings().setSkipGalleryInfo(true);
				layoutGalleryInfo.setVisibility(View.GONE);
			}
		});
		layoutGalleryInfo.setVisibility(View.GONE);

		recyclerViewPhotos = (RecyclerView) findViewById(R.id.recycler_view_albums);
		int colWidth = getResources().getDimensionPixelSize(R.dimen.photo_column_size);
		layoutManagerPhotos = new AlbumLayoutManager(this, colWidth);
		recyclerViewPhotos.setLayoutManager(layoutManagerPhotos);

		setCurrentMode();
    }
    
    @Override
	protected void onResume() {

		super.onResume();

	}

	private void setLayout() {
        setContentView(R.layout.activity_main);
		recyclerViewPhotos = (RecyclerView) findViewById(R.id.recycler_view_albums);
    }

/*	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		
		if (resultCode == RESULT_OK)
		{
			setContentView(R.layout.mainloading);
			
			if (requestCode == GALLERY_RESULT) 
			{
				if (intent != null)
				{
					Uri uriGalleryFile = intent.getData();
					
					try
						{
							if (uriGalleryFile != null)
							{
								Cursor cursor = managedQuery(uriGalleryFile, null, 
		                                null, null, null); 
								cursor.moveToNext(); 
								// Retrieve the path and the mime type 
								String path = cursor.getString(cursor 
								                .getColumnIndex(MediaStore.MediaColumns.DATA)); 
								String mimeType = cursor.getString(cursor 
								                .getColumnIndex(MediaStore.MediaColumns.MIME_TYPE));
								
								if (mimeType == null || mimeType.startsWith("image"))
								{
									Intent passingIntent = new Intent(this,ImageEditor.class);
									passingIntent.setData(uriGalleryFile);
									startActivityForResult(passingIntent,IMAGE_EDITOR);
								}
								else if (mimeType.startsWith("video"))
								{
		
									Intent passingIntent = new Intent(this,VideoEditor.class);
									passingIntent.setData(uriGalleryFile);
									startActivityForResult(passingIntent,VIDEO_EDITOR);
								}
							}
							else
							{
								Toast.makeText(this, "Unable to load media.", Toast.LENGTH_LONG).show();
			
							}
						}
					catch (Exception e)
					{
						Toast.makeText(this, "Unable to load media.", Toast.LENGTH_LONG).show();
						Log.e(TAG, "error loading media: " + e.getMessage(), e);

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
		
		
		
	}	*/

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

	private void setCurrentMode() {
		int permissionCheck = ContextCompat.checkSelfPermission(this,
				Manifest.permission.READ_EXTERNAL_STORAGE);
		if (Build.VERSION.SDK_INT <= 18)
			permissionCheck = PackageManager.PERMISSION_GRANTED; // For old devices we ask in the manifest!
		if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
			AskForPermissionAdapter adapter = new AskForPermissionAdapter(this);
			recyclerViewPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
			recyclerViewPhotos.setAdapter(adapter);
		} else {
			applyCurrentMode();
		}
	}

	public void askForReadExternalStoragePermission() {
		ActivityCompat.requestPermissions(this, new String[]{
					Manifest.permission.READ_EXTERNAL_STORAGE,
					Manifest.permission.WRITE_EXTERNAL_STORAGE
				},
				READ_EXTERNAL_STORAGE_PERMISSION_REQUEST);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case READ_EXTERNAL_STORAGE_PERMISSION_REQUEST: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 1
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED
						&& grantResults[1] == PackageManager.PERMISSION_GRANTED) {
					applyCurrentMode();
				}
			}
			break;
		}
	}

	private void applyCurrentMode() {
		//TODO layoutGalleryInfo.setVisibility(App.getInstance().getSettings().skipGalleryInfo() ? View.GONE : View.VISIBLE);
		setPhotosAdapter(null, true, true);
	}

	private void setPhotosAdapter(String album, boolean showCamera, boolean showAlbums) {
		recyclerViewPhotos.setLayoutManager(layoutManagerPhotos);
		GalleryCursorRecyclerViewAdapter adapter = new GalleryCursorRecyclerViewAdapter(this, album, showCamera, showAlbums);
		adapter.setListener(this);
		int colWidth = getResources().getDimensionPixelSize(R.dimen.photo_column_size);
		layoutManagerPhotos.setColumnWidth(colWidth);
		recyclerViewPhotos.setAdapter(adapter);
	}

	@Override
	public void onPhotoSelected(String photo, View thumbView) {
		final Uri uri = Uri.parse(photo);
		if (uri != null) {
			try {
				Intent passingIntent = new Intent(this, ImageEditor.class);
				passingIntent.setData(uri);
				startActivityForResult(passingIntent, IMAGE_EDITOR);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onVideoSelected(String photo, View thumbView) {
		final Uri uri = Uri.parse(photo);
		if (uri != null) {
			try {
				Intent passingIntent = new Intent(this, VideoEditor.class);
				passingIntent.setData(uri);
				startActivityForResult(passingIntent, VIDEO_EDITOR);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onCameraSelected() {
		takePicture();
	}

	@Override
	public void onAlbumsSelected() {
		Intent intentAlbums = new Intent(this, AlbumsActivity.class);
		startActivityForResult(intentAlbums, SELECT_FROM_ALBUMS_REQUEST);
	}

	private void takePicture() {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

			try {
				File storageDir = new File(getCacheDir(), CAMERA_CAPTURE_DIRNAME);
				if (!storageDir.exists()) {
					storageDir.mkdir();
				}
				File image = new File(storageDir, CAMERA_CAPTURE_FILENAME);
				if (image.exists()) {
					image.delete();
				}
				image.createNewFile();

				Uri photoURI = FileProvider.getUriForFile(this,
						"org.witness.securesmartcam.camera_capture",
						image);
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

				// Need to grant uri permissions, see here:
				// http://stackoverflow.com/questions/33650632/fileprovider-not-working-with-camera
				if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
					takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				}
				else if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN) {
					ClipData clip=
							ClipData.newUri(getContentResolver(), "A photo", photoURI);

					takePictureIntent.setClipData(clip);
					takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				}
				else {
					List<ResolveInfo> resInfoList=
							getPackageManager()
									.queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);

					for (ResolveInfo resolveInfo : resInfoList) {
						String packageName = resolveInfo.activityInfo.packageName;
						grantUriPermission(packageName, photoURI,
								Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
					}
				}

				startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST);
			} catch (IOException ignored) {
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode == RESULT_OK) {
			try {
				File storageDir = new File(getCacheDir(), CAMERA_CAPTURE_DIRNAME);
				File image = new File(storageDir, CAMERA_CAPTURE_FILENAME);
				if (image.exists()) {
					onPhotoSelected(image.getAbsolutePath(), null);
					//TODO image.delete();
				}
			} catch (Exception ignored) {
			}
		} else if (requestCode == SELECT_FROM_ALBUMS_REQUEST) {
			if (resultCode == RESULT_OK && data != null && data.hasExtra("uri")) {
				boolean isVideo = data.getBooleanExtra("video", false);
				if (isVideo) {
					onVideoSelected(data.getStringExtra("uri"), null);
				} else {
					onPhotoSelected(data.getStringExtra("uri"), null);
				}
			}
		}
	}

	private void deleteTmpFile ()
	{
		try {
			File storageDir = new File(getCacheDir(), CAMERA_CAPTURE_DIRNAME);
			if (!storageDir.exists()) {
				storageDir.mkdir();
			}
			File image = new File(storageDir, CAMERA_CAPTURE_FILENAME);
			if (image.exists()) {
				image.delete();
			}
		} catch (Exception ignored) {}
	}
}
