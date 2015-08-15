package org.witness.ssc.video;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

import org.witness.sscphase1.ObscuraApp;
import org.witness.sscphase1.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class VideoCam extends Activity implements OnTouchListener, OnClickListener, MediaRecorder.OnInfoListener, 
															MediaRecorder.OnErrorListener, SurfaceHolder.Callback, Camera.PreviewCallback 
{ 	
	public static final int PLAY = 0;
	public static final int SHARE = 1;
	
	public static final String LOGTAG = ObscuraApp.TAG;
			
	private MediaRecorder recorder;
	private SurfaceHolder holder;
	private CamcorderProfile camcorderProfile;
	private Camera camera;	
	
	// Vector of ObscureRegion objects
	private Vector<ObscureRegion> obscureRegions = new Vector<ObscureRegion>();
	
	boolean recording = false;
	boolean usecamera = true;
	boolean previewRunning = false;
		
	long recordStartTime = 0;
	
	Button recordButton;
	
	Camera.Parameters p;
		
	File savePath;
	File recordingFile;
	File saveFile;
	
	File redactSettingsFile;
	
	File overlayImage; 

	ProgressDialog progressDialog;
	AlertDialog choiceDialog;
	
	
	Display display; 
	int screenWidth;
	int screenHeight;
	
	float calcDefaultXSize;
	float calcDefaultYSize;

	private void createCleanSavePath() {
		savePath = Environment.getExternalStorageDirectory();
		
		Log.v(LOGTAG,"savePath:" + savePath.getPath());
		if (savePath.exists()) {
			Log.v(LOGTAG,"savePath exists!");
		} else {
			Log.v(LOGTAG,"savePath DOES NOT exist!");
			savePath.mkdirs();

		}
		
		try {
			saveFile = File.createTempFile("output", ".mp4", savePath);
		} catch (IOException e) {
			e.printStackTrace();
		}

		/*
		File[] existingFiles = savePath.listFiles();
		if (existingFiles != null) {
			for (int i = 0; i < existingFiles.length; i++) {
				existingFiles[i].delete();
			}
		}
		*/
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	
		camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
	
		setContentView(R.layout.video_camera_view);
		
		recordButton = (Button) this.findViewById(R.id.RecordButton);
		recordButton.setOnClickListener(this);		
		
		SurfaceView cameraView = (SurfaceView) findViewById(R.id.CameraView);
		holder = cameraView.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			
		cameraView.setOnTouchListener(this);
		
		display = getWindowManager().getDefaultDisplay(); 
		screenWidth = display.getWidth();
		screenHeight = display.getHeight();		
		
		redactSettingsFile = new File(Environment.getExternalStorageDirectory(),"redact_unsort.txt");
		
	}
	
	private void prepareRecorder() {
	    recorder = new MediaRecorder();
		recorder.setPreviewDisplay(holder.getSurface());
		
		if (usecamera) {
			camera.unlock();
			recorder.setCamera(camera);
		}
		
		recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
	
		recorder.setProfile(camcorderProfile);
		
		calcDefaultXSize = (float)camcorderProfile.videoFrameWidth/(float)screenWidth * (float)ObscureRegion.DEFAULT_X_SIZE;
		calcDefaultYSize = (float)camcorderProfile.videoFrameHeight/(float)screenHeight * (float)ObscureRegion.DEFAULT_Y_SIZE;
		
		createCleanSavePath();
		
		try {
			
			// This is all very sloppy
			if (camcorderProfile.fileFormat == MediaRecorder.OutputFormat.THREE_GPP) {
				recordingFile = File.createTempFile("videocapture", ".3gp", savePath);
				Log.v(LOGTAG,"Recording at: " + recordingFile.getAbsolutePath());
				recorder.setOutputFile(recordingFile.getAbsolutePath());
			} else if (camcorderProfile.fileFormat == MediaRecorder.OutputFormat.MPEG_4) {
	    		recordingFile = File.createTempFile("videocapture", ".mp4", savePath);
				Log.v(LOGTAG,"Recording at: " + recordingFile.getAbsolutePath());
				recorder.setOutputFile(recordingFile.getAbsolutePath());
			} else {
	    		recordingFile = File.createTempFile("videocapture", ".mp4", savePath);
				Log.v(LOGTAG,"Recording at: " + recordingFile.getAbsolutePath());
				recorder.setOutputFile(recordingFile.getAbsolutePath());
			}
		//recorder.setMaxDuration(50000); // 50 seconds
		//recorder.setMaxFileSize(5000000); // Approximately 5 megabytes
		
			recorder.prepare();
		} catch (IOException e) {
			Log.v(LOGTAG,"Couldn't create file");
			e.printStackTrace();
			finish();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			finish();
		}
	}

	public void onClick(View v) {
		if (recording) {
			recorder.stop();
			if (usecamera) {
				try {
					camera.reconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}			
			// recorder.release();
			recording = false;
			Log.v(LOGTAG, "Recording Stopped");
			
			//TODO: this should now launch the VideoEditor
			
		} else {
			recording = true;
			recordStartTime = SystemClock.uptimeMillis();
			recorder.start();
			Log.v(LOGTAG, "Recording Started");
		}
	}
	
	public void surfaceCreated(SurfaceHolder holder) {
		Log.v(LOGTAG, "surfaceCreated");
		
		if (usecamera) {
			camera = Camera.open();
			
			try {
				camera.setPreviewDisplay(holder);
				camera.startPreview();
				previewRunning = true;
			}
			catch (IOException e) {
				Log.e(LOGTAG,e.getMessage());
				e.printStackTrace();
			}	
		}		
		
	}
	
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.v(LOGTAG, "surfaceChanged");
	
		if (!recording && usecamera) {
			if (previewRunning){
				camera.stopPreview();
			}
	
			try {
				Camera.Parameters p = camera.getParameters();
	
				 p.setPreviewSize(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);
			     p.setPreviewFrameRate(camcorderProfile.videoFrameRate);
				
				camera.setParameters(p);
				
				camera.setPreviewDisplay(holder);
				camera.startPreview();
				previewRunning = true;
			}
			catch (IOException e) {
				Log.e(LOGTAG,e.getMessage());
				e.printStackTrace();
			}	

			prepareRecorder();	
		}
	}
	
	
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.v(LOGTAG, "surfaceDestroyed");
		if (recording) {
			recorder.stop();
			recording = false;
		}
		recorder.release();
		if (usecamera) {
			previewRunning = false;
			camera.lock();
			camera.release();
		}
		finish();
	}

	public void onPreviewFrame(byte[] b, Camera c) {
		
	}
	
    @Override
    public void onConfigurationChanged(Configuration conf) 
    {
        super.onConfigurationChanged(conf);
    }	
	    
    private void createOverlayImage() {
		try {
			overlayImage = new File(savePath,"overlay.jpg");
			
	    	Bitmap overlayBitmap = Bitmap.createBitmap(720, 480, Bitmap.Config.RGB_565);
	    	Canvas obscuredCanvas = new Canvas(overlayBitmap);
	    	Paint obscuredPaint = new Paint();   
	    	Matrix obscuredMatrix = new Matrix();
	        
	    	obscuredCanvas.drawOval(new RectF(10,10,100,100), obscuredPaint);
	    	
	    	OutputStream overlayImageFileOS = new FileOutputStream(overlayImage);
			overlayBitmap.compress(CompressFormat.JPEG, 90, overlayImageFileOS);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    }
    
	
	private void showPlayShareDialog() {
		progressDialog.cancel();

		AlertDialog.Builder builder = new AlertDialog.Builder(VideoCam.this);
		builder.setMessage("Play or Share?")
			.setCancelable(true)
			.setPositiveButton("Play", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					playVideo();
				}
			})
			.setNegativeButton("Share", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	            	shareVideo();
	            }
		    });
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void playVideo() {
    	Intent intent = new Intent(android.content.Intent.ACTION_VIEW); 
   	 	Uri data = Uri.parse(savePath.getPath()+"/output.mp4");
   	 	intent.setDataAndType(data, "video/mp4"); 
   	 	startActivityForResult(intent,PLAY);
	}
	
	private void shareVideo() {
    	Intent share = new Intent(Intent.ACTION_SEND);
    	share.setType("video/mp4");
    	share.putExtra(Intent.EXTRA_STREAM, Uri.parse(savePath.getPath()+"/output.mp4"));
    	startActivityForResult(Intent.createChooser(share, "Share Video"),SHARE);     
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		showPlayShareDialog();
	}

	public void onInfo(MediaRecorder mr, int what, int extra) {
		
	}

	public void onError(MediaRecorder mr, int what, int extra) {
		
	}
	
	int startTime = 0;
	float startX = 0;
	float startY = 0;
	
	public boolean onTouch(View v, MotionEvent event) {
		
		boolean handled = false;

		float x = event.getX()/(float)screenWidth * (float)camcorderProfile.videoFrameWidth;
		float y = event.getY()/(float)screenHeight * (float)camcorderProfile.videoFrameHeight;

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		
			case MotionEvent.ACTION_DOWN:
				// Single Finger down
				
				if (recording) {
					startTime = (int)(SystemClock.uptimeMillis() - recordStartTime);
					startX = x;
					startY = y;
					
					//ObscureRegion singleFingerRegion = new ObscureRegion(SystemClock.uptimeMillis() - recordStartTime,x,y);
					//obscureRegions.add(singleFingerRegion);
				}

				handled = true;
				
				break;
				
			case MotionEvent.ACTION_POINTER_DOWN:
				
				break;
				
			case MotionEvent.ACTION_UP:
				// Single Finger Up
				
				if (recording) {					
					ObscureRegion singleFingerUpRegion = new ObscureRegion(startTime,x,y);
					obscureRegions.add(singleFingerUpRegion);
				}
				
				break;
				
			case MotionEvent.ACTION_POINTER_UP:
				
				break;
				
			case MotionEvent.ACTION_MOVE:
				// Calculate distance moved
				
				if (recording) {
					ObscureRegion oneFingerMoveRegion = new ObscureRegion(startTime,x,y);
					obscureRegions.add(oneFingerMoveRegion);
					
					startTime = (int)(SystemClock.uptimeMillis() - recordStartTime);
					startX = x;
					startY = y;
				}
				
				handled = true;

				break;
		}

		return handled; // indicate event was handled	
	}	
}