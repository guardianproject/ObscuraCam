/*
 * 
 * To Do:
 * 
 * Handles on SeekBar - Not quite right
 * Editing region, not quite right
 * RegionBarArea will be graphical display of region tracks, no editing, just selecting
 * 
 */

package org.witness.ssc.video;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.londatiga.android.QuickAction.OnActionItemClickListener;

import org.witness.ssc.video.InOutPlayheadSeekBar.InOutPlayheadSeekBarChangeListener;
import org.witness.ssc.video.ShellUtils.ShellCallback;
import org.witness.sscphase1.ObscuraApp;
import org.witness.sscphase1.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

public class VideoEditor extends Activity implements
						OnCompletionListener, OnErrorListener, OnInfoListener,
						OnBufferingUpdateListener, OnPreparedListener, OnSeekCompleteListener,
						OnVideoSizeChangedListener, SurfaceHolder.Callback,
						MediaController.MediaPlayerControl, OnTouchListener, OnClickListener,
						InOutPlayheadSeekBarChangeListener, OnActionItemClickListener {

	public static final String LOGTAG = ObscuraApp.TAG;

	public static final int SHARE = 1;

    private final static float REGION_CORNER_SIZE = 26;
	
	ProgressDialog progressDialog;

	Uri originalVideoUri;

	File fileExternDir;
	File redactSettingsFile;
	File saveFile;
	File recordingFile;
	
	Display currentDisplay;

	VideoView videoView;
	SurfaceHolder surfaceHolder;
	MediaPlayer mediaPlayer;	
	
	ImageView regionsView;
	Bitmap obscuredBmp;
    Canvas obscuredCanvas;
	Paint obscuredPaint;
	Paint selectedPaint;
	
	Bitmap bitmapCornerUL;
	Bitmap bitmapCornerUR;
	Bitmap bitmapCornerLL;
	Bitmap bitmapCornerLR;
	
	InOutPlayheadSeekBar progressBar;
	//RegionBarArea regionBarArea;
	
	int videoWidth = 0;
	int videoHeight = 0;
	
	ImageButton playPauseButton;
	
	private Vector<ObscureRegion> obscureRegions = new Vector<ObscureRegion>();
	private ObscureRegion activeRegion;
	
	FFMPEGWrapper ffmpeg;
	
	private Handler mHandler = new Handler()
	{
		 public void handleMessage(Message msg) {
	            switch (msg.what) {
	                case 1: //status

	                       progressDialog.setMessage(msg.getData().getString("status"));
	                        
	                    break;
	               
	                case 2: //cancelled
	                	mCancelled = true;
	                		killVideoProcessor();
	                	
	                	break;
	                	
	                case 3: //completed

	        			if (!mCancelled)
	        				showPlayShareDialog();
	                	
	                	break;
	                default:
	                    super.handleMessage(msg);
	            }
	        }
	};
	
	private boolean mCancelled = false;
	
	QuickAction popupMenu;
	ActionItem[] popupMenuItems;
	
	public static final int CORNER_NONE = 0;
	public static final int CORNER_UPPER_LEFT = 1;
	public static final int CORNER_LOWER_LEFT = 2;
	public static final int CORNER_UPPER_RIGHT = 3;
	public static final int CORNER_LOWER_RIGHT = 4;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.videoeditor);
		
		fileExternDir = new File(Environment.getExternalStorageDirectory(),getString(R.string.app_name));
		if (!fileExternDir.exists())
			fileExternDir.mkdirs();

		regionsView = (ImageView) this.findViewById(R.id.VideoEditorImageView);
		regionsView.setOnTouchListener(this);
		createCleanSavePath();

		// Passed in from ObscuraApp
		originalVideoUri = getIntent().getData();
		recordingFile = new File(pullPathFromUri(originalVideoUri));

		videoView = (VideoView) this.findViewById(R.id.SurfaceView);
		
		surfaceHolder = videoView.getHolder();

		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnInfoListener(this);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnSeekCompleteListener(this);
		mediaPlayer.setOnVideoSizeChangedListener(this);
		mediaPlayer.setOnBufferingUpdateListener(this);

		mediaPlayer.setLooping(false);
		mediaPlayer.setScreenOnWhilePlaying(true);		
		
		try {
			mediaPlayer.setDataSource(originalVideoUri.toString());
		} catch (IllegalArgumentException e) {
			Log.v(LOGTAG, e.getMessage());
			finish();
		} catch (IllegalStateException e) {
			Log.v(LOGTAG, e.getMessage());
			finish();
		} catch (IOException e) {
			Log.v(LOGTAG, e.getMessage());
			finish();
		}
				
		progressBar = (InOutPlayheadSeekBar) this.findViewById(R.id.InOutPlayheadSeekBar);

		progressBar.setIndeterminate(false);
		progressBar.setSecondaryProgress(0);
		progressBar.setProgress(0);
		progressBar.setInOutPlayheadSeekBarChangeListener(this);
		progressBar.setThumbsInactive();
		progressBar.setOnTouchListener(this);

		playPauseButton = (ImageButton) this.findViewById(R.id.PlayPauseImageButton);
		playPauseButton.setOnClickListener(this);
		
		currentDisplay = getWindowManager().getDefaultDisplay();
				
		redactSettingsFile = new File(fileExternDir,"redact_unsort.txt");
		
		//regionBarArea = (RegionBarArea) this.findViewById(R.id.RegionBarArea);
		//regionBarArea.obscureRegions = obscureRegions;
		
		obscuredPaint = new Paint();   
        obscuredPaint.setColor(Color.WHITE);
	    obscuredPaint.setStyle(Style.STROKE);
	    obscuredPaint.setStrokeWidth(10f);
	    
	    selectedPaint = new Paint();
	    selectedPaint.setColor(Color.GREEN);
	    selectedPaint.setStyle(Style.STROKE);
	    selectedPaint.setStrokeWidth(10f);
	    
		bitmapCornerUL = BitmapFactory.decodeResource(getResources(), R.drawable.edit_region_corner_ul);
		bitmapCornerUR = BitmapFactory.decodeResource(getResources(), R.drawable.edit_region_corner_ur);
		bitmapCornerLL = BitmapFactory.decodeResource(getResources(), R.drawable.edit_region_corner_ll);
		bitmapCornerLR = BitmapFactory.decodeResource(getResources(), R.drawable.edit_region_corner_lr);
		
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.v(LOGTAG, "surfaceCreated Called");
		if (mediaPlayer != null)
		{
		
			mediaPlayer.setDisplay(surfaceHolder);
			
			try {
				mediaPlayer.prepareAsync();			
			} catch (Exception e) {
				Log.v(LOGTAG, "IllegalStateException " + e.getMessage());
				finish();
			}
		}
	
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.v(LOGTAG, "surfaceChanged Called");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.v(LOGTAG, "surfaceDestroyed Called");
		//if (mediaPlayer != null)
			//mediaPlayer.stop();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		Log.v(LOGTAG, "onCompletion Called");
	}
	
	@Override
	public boolean onError(MediaPlayer mp, int whatError, int extra) {
		Log.v(LOGTAG, "onError Called");
		if (whatError == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
			Log.v(LOGTAG, "Media Error, Server Died " + extra);
		} else if (whatError == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
			Log.v(LOGTAG, "Media Error, Error Unknown " + extra);
		}
		return false;
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int whatInfo, int extra) {
		if (whatInfo == MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING) {
			Log.v(LOGTAG, "Media Info, Media Info Bad Interleaving " + extra);
		} else if (whatInfo == MediaPlayer.MEDIA_INFO_NOT_SEEKABLE) {
			Log.v(LOGTAG, "Media Info, Media Info Not Seekable " + extra);
		} else if (whatInfo == MediaPlayer.MEDIA_INFO_UNKNOWN) {
			Log.v(LOGTAG, "Media Info, Media Info Unknown " + extra);
		} else if (whatInfo == MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING) {
			Log.v(LOGTAG, "MediaInfo, Media Info Video Track Lagging " + extra);
		} else if (whatInfo == MediaPlayer.MEDIA_INFO_METADATA_UPDATE) { 
			Log.v(LOGTAG, "MediaInfo, Media Info Metadata Update " + extra); 
		}
		
		return false;
	}

	public void onPrepared(MediaPlayer mp) {
		Log.v(LOGTAG, "onPrepared Called");

		updateVideoLayout ();
		start();	
	}

	public void onSeekComplete(MediaPlayer mp) {
		Log.v(LOGTAG, "onSeekComplete Called");
		
		
		if (!mediaPlayer.isPlaying()) {			
			mediaPlayer.start();
			mediaPlayer.pause();
		}
	}

	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		Log.v(LOGTAG, "onVideoSizeChanged Called");

		videoWidth = mp.getVideoWidth();
		videoHeight = mp.getVideoHeight();

		updateVideoLayout ();
		
	}
	
	private void updateVideoLayout ()
	{
		//Get the dimensions of the video
	    int videoWidth = mediaPlayer.getVideoHeight();
	    int videoHeight = mediaPlayer.getVideoWidth();

	    //Get the width of the screen
	    int screenWidth = getWindowManager().getDefaultDisplay().getWidth();

	    //Get the SurfaceView layout parameters
	    android.view.ViewGroup.LayoutParams lp = videoView.getLayoutParams();

	    //Set the width of the SurfaceView to the width of the screen
	    lp.width = screenWidth;

	    //Set the height of the SurfaceView to match the aspect ratio of the video 
	    //be sure to cast these as floats otherwise the calculation will likely be 0
	   
	    int videoScaledHeight = (int) (((float)videoHeight) / ((float)videoWidth) * (float)screenWidth);

	    lp.height = videoScaledHeight;
	   
	    //Commit the layout parameters
	    videoView.setLayoutParams(lp);     
	}

	public void onBufferingUpdate(MediaPlayer mp, int bufferedPercent) {
		Log.v(LOGTAG, "MediaPlayer Buffering: " + bufferedPercent + "%");
	}

	public boolean canPause() {
		return true;
	}

	public boolean canSeekBackward() {
		return true;
	}

	public boolean canSeekForward() {
		return true;
	}

	@Override
	public int getBufferPercentage() {
		return 0;
	}

	@Override
	public int getCurrentPosition() {
		return mediaPlayer.getCurrentPosition();
	}

	@Override
	public int getDuration() {
		Log.v(LOGTAG,"Calling our getDuration method");
		return mediaPlayer.getDuration();
	}

	@Override
	public boolean isPlaying() {
		Log.v(LOGTAG,"Calling our isPlaying method");
		return mediaPlayer.isPlaying();
	}

	@Override
	public void pause() {
		Log.v(LOGTAG,"Calling our pause method");
		if (mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
		}
	}

	@Override
	public void seekTo(int pos) {
		Log.v(LOGTAG,"Calling our seekTo method");
		mediaPlayer.seekTo(pos);
	}

	@Override
	public void start() {
		Log.v(LOGTAG,"Calling our start method");
		mediaPlayer.start();
		playPauseButton.setImageDrawable(this.getResources().getDrawable(android.R.drawable.ic_media_pause));
		mHandler.postDelayed(updatePlayProgress, 100);		

	}
	
	private Runnable updatePlayProgress = new Runnable() {
	   public void run() {
		   
		   try
		   {
			   if (mediaPlayer != null) {
				   if (mediaPlayer.isPlaying()) {
					   progressBar.setProgress((int)(((float)mediaPlayer.getCurrentPosition()/(float)mediaPlayer.getDuration())*100));
				   }   
				   updateRegionDisplay();
			   }
			   mHandler.postDelayed(this, 100);
		   }
		   catch (Exception e)
		   {
			   //must not be playing anymore
		   }
	   }
	};		
	
	private void updateRegionDisplay() {

		//Log.v(LOGTAG,"Position: " + mediaPlayer.getCurrentPosition());
		
		validateRegionView();
		clearRects();
				
		for (ObscureRegion region:obscureRegions) {
			if (region.existsInTime(mediaPlayer.getCurrentPosition())) {
				// Draw this region
				//Log.v(LOGTAG,mediaPlayer.getCurrentPosition() + " Drawing a region: " + region.getBounds().left + " " + region.getBounds().top + " " + region.getBounds().right + " " + region.getBounds().bottom);
				if (region != activeRegion) {
					displayRegion(region,false);
				}
			}
		}
		
		if (activeRegion != null && activeRegion.existsInTime(mediaPlayer.getCurrentPosition())) {
			displayRegion(activeRegion,true);
			//displayRect(activeRegion.getBounds(), selectedPaint);
		}
		
		regionsView.invalidate();
		//seekBar.invalidate();
	}
	
	private void validateRegionView() {
		if (obscuredBmp == null) {
			Log.v(LOGTAG,"obscuredBmp is null, creating it now");
			obscuredBmp = Bitmap.createBitmap(regionsView.getWidth(), regionsView.getHeight(), Bitmap.Config.ARGB_8888);
			obscuredCanvas = new Canvas(obscuredBmp); 
		    regionsView.setImageBitmap(obscuredBmp);			
		}
	}
	
	private void displayRegion(ObscureRegion region, boolean selected) {
					    	    	
    	if (selected) {

    		RectF paintingRect = new RectF();
        	paintingRect.set(region.getBounds());
        	paintingRect.inset(10,10);
        	
        	obscuredPaint.setStrokeWidth(10f);
    		obscuredPaint.setColor(Color.GREEN);
        	
    		obscuredCanvas.drawRect(paintingRect, obscuredPaint);
    		
        	obscuredCanvas.drawBitmap(bitmapCornerUL, paintingRect.left-REGION_CORNER_SIZE, paintingRect.top-REGION_CORNER_SIZE, obscuredPaint);
    		obscuredCanvas.drawBitmap(bitmapCornerLL, paintingRect.left-REGION_CORNER_SIZE, paintingRect.bottom-(REGION_CORNER_SIZE/2), obscuredPaint);
    		obscuredCanvas.drawBitmap(bitmapCornerUR, paintingRect.right-(REGION_CORNER_SIZE/2), paintingRect.top-REGION_CORNER_SIZE, obscuredPaint);
    		obscuredCanvas.drawBitmap(bitmapCornerLR, paintingRect.right-(REGION_CORNER_SIZE/2), paintingRect.bottom-(REGION_CORNER_SIZE/2), obscuredPaint);
    	    
    	} else {
    		obscuredPaint.setColor(Color.YELLOW);
    		obscuredCanvas.drawRect(region.getBounds(), obscuredPaint);
    	}
	}
	
	private void clearRects() {
		Paint clearPaint = new Paint();
		clearPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
		obscuredCanvas.drawPaint(clearPaint);
	}

	int currentNumFingers = 0;
	int regionCornerMode = 0;
	
	public static final int NONE = 0;
	public static final int DRAG = 1;
	//int mode = NONE;

	public ObscureRegion findRegion(MotionEvent event) 
	{
		ObscureRegion returnRegion = null;
		
		for (ObscureRegion region : obscureRegions)
		{
			if (region.getBounds().contains(event.getX(),event.getY()))
			{
				returnRegion = region;
				break;
			}
		}			
		return returnRegion;
	}
	
	/*
	long startTime = 0;
	float startX = 0;
	float startY = 0;
	*/

	boolean showMenu = false;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		
		boolean handled = false;

		if (v == progressBar) {
			// It's the progress bar/scrubber
			if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
			    mediaPlayer.start();
		    } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
		    	mediaPlayer.pause();
		    }
			
			Log.v(LOGTAG,"" + event.getX() + " " + event.getX()/progressBar.getWidth());
			Log.v(LOGTAG,"Seeking To: " + (int)(mediaPlayer.getDuration()*(float)(event.getX()/progressBar.getWidth())));
			mediaPlayer.seekTo((int)(mediaPlayer.getDuration()*(float)(event.getX()/progressBar.getWidth())));
			Log.v(LOGTAG,"MediaPlayer Position: " + mediaPlayer.getCurrentPosition());
			// Attempt to get the player to update it's view - NOT WORKING
			
			handled = false; // The progress bar doesn't get it if we have true here
		}
		else
		{
			// Region Related
			float x = event.getX()/(float)currentDisplay.getWidth() * videoWidth;
			float y = event.getY()/(float)currentDisplay.getHeight() * videoHeight;

			switch (event.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN:

					// Single Finger down
					currentNumFingers = 1;
					
					// If we have a region in creation/editing and we touch within it
					if (activeRegion != null && activeRegion.getRectF().contains(x, y)) {

						// Should display menu, unless they move
						showMenu = true;
						
						// Are we on a corner?
						regionCornerMode = getRegionCornerMode(activeRegion, x, y);
						
						Log.v(LOGTAG,"Touched activeRegion");
																		
					} else {
					
						showMenu = false;
						
						ObscureRegion previouslyActiveRegion = activeRegion;
						activeRegion = findRegion(event);
						
						if (activeRegion != null)
						{
							if (previouslyActiveRegion == activeRegion)
							{
								// Display menu unless they move
								showMenu = true;
								
								// Are we on a corner?
								regionCornerMode = getRegionCornerMode(activeRegion, x, y);
								
								// Show in and out points
								progressBar.setThumbsActive((int)(activeRegion.startTime/mediaPlayer.getDuration()*100), (int)(activeRegion.endTime/mediaPlayer.getDuration()*100));

								// They are interacting with the active region
								Log.v(LOGTAG,"Touched an active region");
							}
							else
							{
								// They are interacting with the active region
								Log.v(LOGTAG,"Touched an existing region, make it active");
							}
						}
						else 
						{
							
							activeRegion = new ObscureRegion(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition(),mediaPlayer.getDuration(),x,y);
							obscureRegions.add(activeRegion);
							
							Log.v(LOGTAG,"Creating a new activeRegion");
							
							Log.v(LOGTAG,"startTime: " + activeRegion.startTime + " duration: " + mediaPlayer.getDuration() + " math startTime/duration*100: " + (int)((float)activeRegion.startTime/(float)mediaPlayer.getDuration()*100));
							
							// Show in and out points
							progressBar.setThumbsActive((int)((double)activeRegion.startTime/(double)mediaPlayer.getDuration()*100), (int)((double)activeRegion.endTime/(double)mediaPlayer.getDuration()*100));

						}
					}

					handled = true;

					break;
					
				case MotionEvent.ACTION_UP:
					// Single Finger Up
					currentNumFingers = 0;
					
					if (showMenu) {
						
						Log.v(LOGTAG,"Touch Up: Show Menu - Really finalizing activeRegion");
						inflatePopup(false);
												
						showMenu = false;
					}
					
					break;
										
				case MotionEvent.ACTION_MOVE:
					// Calculate distance moved
					showMenu = false;
					
					if (activeRegion != null && mediaPlayer.getCurrentPosition() > activeRegion.startTime) {
						Log.v(LOGTAG,"Moving a activeRegion");
						
						long previousEndTime = activeRegion.endTime;
						activeRegion.endTime = mediaPlayer.getCurrentPosition();
						
						ObscureRegion lastRegion = activeRegion;
						activeRegion = null;
						
						if (regionCornerMode != CORNER_NONE) {
				
							//moveRegion(float _sx, float _sy, float _ex, float _ey)
							// Create new region with moved coordinates
							if (regionCornerMode == CORNER_UPPER_LEFT) {
								activeRegion = new ObscureRegion(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition(),previousEndTime,x,y,lastRegion.ex,lastRegion.ey);
								obscureRegions.add(activeRegion);
							} else if (regionCornerMode == CORNER_LOWER_LEFT) {
								activeRegion = new ObscureRegion(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition(),previousEndTime,x,lastRegion.sy,lastRegion.ex,y);
								obscureRegions.add(activeRegion);
							} else if (regionCornerMode == CORNER_UPPER_RIGHT) {
								activeRegion = new ObscureRegion(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition(),previousEndTime,lastRegion.sx,y,x,lastRegion.ey);
								obscureRegions.add(activeRegion);
							} else if (regionCornerMode == CORNER_LOWER_RIGHT) {
								activeRegion = new ObscureRegion(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition(),previousEndTime,lastRegion.sx,lastRegion.sy,x,y);
								obscureRegions.add(activeRegion);
							}
						} else {		
							// No Corner
							activeRegion = new ObscureRegion(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition(),previousEndTime,x,y);
							obscureRegions.add(activeRegion);
						}
						
						if (activeRegion != null) {
							// Show in and out points
							progressBar.setThumbsActive((int)(activeRegion.startTime/mediaPlayer.getDuration()*100), (int)(activeRegion.endTime/mediaPlayer.getDuration()*100));
						}
						
					} else if (activeRegion != null) {
						Log.v(LOGTAG,"Moving activeRegion start time");
						
						if (regionCornerMode != CORNER_NONE) {
							
							// Just move region, we are at begin time
							if (regionCornerMode == CORNER_UPPER_LEFT) {
								activeRegion.moveRegion(x,y,activeRegion.ex,activeRegion.ey);
							} else if (regionCornerMode == CORNER_LOWER_LEFT) {
								activeRegion.moveRegion(x,activeRegion.sy,activeRegion.ex,y);
							} else if (regionCornerMode == CORNER_UPPER_RIGHT) {
								activeRegion.moveRegion(activeRegion.sx,y,x,activeRegion.ey);
							} else if (regionCornerMode == CORNER_LOWER_RIGHT) {
								activeRegion.moveRegion(activeRegion.sx,activeRegion.sy,x,y);
							}
						} else {		
							// No Corner
							activeRegion.moveRegion(x, y);
						}
						
						// Show in and out points
						progressBar.setThumbsActive((int)(activeRegion.startTime/mediaPlayer.getDuration()*100), (int)(activeRegion.endTime/mediaPlayer.getDuration()*100));

					}
					
					handled = true;
					break;
			}
		}
		return handled; // indicate event was handled	
	}
	
	
	public int getRegionCornerMode(ObscureRegion region, float x, float y)
	{    			
    	if (Math.abs(region.getBounds().left-x)<REGION_CORNER_SIZE
    			&& Math.abs(region.getBounds().top-y)<REGION_CORNER_SIZE)
    	{
    		Log.v(LOGTAG,"CORNER_UPPER_LEFT");
    		return CORNER_UPPER_LEFT;
    	}
    	else if (Math.abs(region.getBounds().left-x)<REGION_CORNER_SIZE
    			&& Math.abs(region.getBounds().bottom-y)<REGION_CORNER_SIZE)
    	{
    		Log.v(LOGTAG,"CORNER_LOWER_LEFT");
    		return CORNER_LOWER_LEFT;
		}
    	else if (Math.abs(region.getBounds().right-x)<REGION_CORNER_SIZE
    			&& Math.abs(region.getBounds().top-y)<REGION_CORNER_SIZE)
    	{
        		Log.v(LOGTAG,"CORNER_UPPER_RIGHT");
    			return CORNER_UPPER_RIGHT;
		}
    	else if (Math.abs(region.getBounds().right-x)<REGION_CORNER_SIZE
        			&& Math.abs(region.getBounds().bottom-y)<REGION_CORNER_SIZE)
    	{
    		Log.v(LOGTAG,"CORNER_LOWER_RIGHT");
    		return CORNER_LOWER_RIGHT;
    	}
    	
		Log.v(LOGTAG,"CORNER_NONE");    	
    	return CORNER_NONE;
	}
	
	
	@Override
	public void onClick(View v) {
		if (v == playPauseButton) {
			if (mediaPlayer.isPlaying()) {
				mediaPlayer.pause();
				playPauseButton.setImageDrawable(this.getResources().getDrawable(android.R.drawable.ic_media_play));

			} else {
				start();
				

			}
		}
	}	

	public String pullPathFromUri(Uri originalUri) {
    	String originalVideoFilePath = null;
    	String[] columnsToSelect = { MediaStore.Video.Media.DATA };
    	Cursor videoCursor = getContentResolver().query(originalUri, columnsToSelect, null, null, null );
    	if ( videoCursor != null && videoCursor.getCount() == 1 ) {
	        videoCursor.moveToFirst();
	        originalVideoFilePath = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Images.Media.DATA));
    	}

    	return originalVideoFilePath;
    }
	
	private void createCleanSavePath() {
		
		try {
			saveFile = File.createTempFile("output", ".mp4", fileExternDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public final static int PLAY = 1;
	public final static int STOP = 2;
	public final static int PROCESS = 3;
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		
		String processString = "Process Video";
		
    	MenuItem processMenuItem = menu.add(Menu.NONE, PROCESS, Menu.NONE, processString);
    	processMenuItem.setIcon(R.drawable.ic_menu_save);
    	
    	return true;
	}
	
    public boolean onOptionsItemSelected(MenuItem item) {	
        switch (item.getItemId()) {
        	case PROCESS:
        		processVideo();
        		return true;
        		
        	default:
        		
        		return false;
        }
    }

    private void processVideo() {
    	
    	mCancelled = false;
    	
    	mediaPlayer.pause();
    	//mediaPlayer.release();
    	
    	progressDialog = ProgressDialog.show(this, "", "Processing. Please wait...", true);
    	progressDialog.setCancelable(true);
    	
    	 Message msg = mHandler.obtainMessage(2);
         msg.getData().putString("status","cancelled");
         progressDialog.setCancelMessage(msg);
    	
		// Convert to video
		Thread thread = new Thread (runProcessVideo);
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();
    }
    
	Runnable runProcessVideo = new Runnable () {
		
		public void run ()
		{

			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
			wl.acquire();
	        
			try
			{
				if (ffmpeg == null)
					ffmpeg = new FFMPEGWrapper(VideoEditor.this.getBaseContext());
	
				float sizeMult = .75f;
				int frameRate = 15;
				int bitRate = 300;
				String format = "mp4";
				
				ShellUtils.ShellCallback sc = new ShellUtils.ShellCallback ()
				{

					@Override
					public void shellOut(char[] shellout) {
						
						String line = new String(shellout);
						
						//progressDialog.setMessage(new String(msg));
						//Duration: 00:00:00.99,
						//time=00:00:00.00
						int idx1;
						String newStatus = null;
						
						if ((idx1 = line.indexOf("Duration:"))!=-1)
						{
							int idx2 = line.indexOf(",", idx1);
							newStatus = line.substring(idx1,idx2);
						}
						else if ((idx1 = line.indexOf("time="))!=-1)
						{
							int idx2 = line.indexOf(" ", idx1);
							newStatus = line.substring(idx1,idx2);
						}
						
						if (newStatus != null)
						{
						 Message msg = mHandler.obtainMessage(1);
				         msg.getData().putString("status",line);
				         mHandler.sendMessage(msg);
						}
					}
					
				};
				
				// Could make some high/low quality presets	
				ffmpeg.processVideo(redactSettingsFile, obscureRegions, recordingFile, saveFile, format, mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight(), frameRate, bitRate, sizeMult, sc);
			}
			catch (Exception e)
			{
				Log.e(LOGTAG,"error with ffmpeg",e);
			}
			
			wl.release();
		     
			Message msg = mHandler.obtainMessage(3);
	         msg.getData().putString("status","complete");
	         mHandler.sendMessage(msg);
	         
		}
		
		
	};
	
	private void showPlayShareDialog() {
		progressDialog.cancel();

		AlertDialog.Builder builder = new AlertDialog.Builder(VideoEditor.this);
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
   	 	Uri data = Uri.parse(saveFile.getPath());
   	 	
   	 	intent.setDataAndType(data, "video/mp4"); 
   	 	startActivityForResult(intent,PLAY);
	}
	
	private void shareVideo() {
    	Intent share = new Intent(Intent.ACTION_SEND);
    	share.setType("video/mp4");
    	share.putExtra(Intent.EXTRA_STREAM, Uri.parse(saveFile.getPath()));
    	startActivityForResult(Intent.createChooser(share, "Share Video"),SHARE);     
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		
	}

	@Override
	public void inOutValuesChanged(int thumbInValue, int thumbOutValue) {
		if (activeRegion != null) {
			activeRegion.startTime = thumbInValue;
			activeRegion.endTime = thumbOutValue;
		}
	}
	
	public void inflatePopup(boolean showDelayed) {
		if (popupMenu == null)
			initPopup();

		popupMenu.show(regionsView, (int)activeRegion.getBounds().centerX(), (int)activeRegion.getBounds().centerY());
	}
	
	private void initPopup ()
	{
		popupMenu = new QuickAction(this);

		popupMenuItems = new ActionItem[5];
		
		popupMenuItems[0] = new ActionItem();
		popupMenuItems[0].setTitle("Set In Point");
		//popupMenuItems[0].setIcon(getResources().getDrawable(R.drawable.icon));			

		popupMenuItems[1] = new ActionItem();
		popupMenuItems[1].setTitle("Set Out Point");
				
		popupMenuItems[2] = new ActionItem();
		popupMenuItems[2].setTitle("Remove Region");				

		for (int i=0; i < popupMenuItems.length; i++) {
			if (popupMenuItems[i] != null) {
				popupMenu.addActionItem(popupMenuItems[i]);
			}
		}
			
		popupMenu.setOnActionItemClickListener(this);
	}

	//Popup menu item clicked
	@Override
	public void onItemClick(int pos) {
		
		switch (pos) {
			case 0:
				// set in point
				activeRegion.startTime = mediaPlayer.getCurrentPosition();
				break;
			case 1:
				// set out point
				activeRegion.endTime = mediaPlayer.getCurrentPosition();
				activeRegion = null;
				
				// Hide in and out points
				progressBar.setThumbsInactive();
				
				break;
			case 2:
				// Remove region
				obscureRegions.remove(activeRegion);
				activeRegion = null;
				break;
		}
	}

	@Override
	protected void onPause() {

		super.onPause();
		mediaPlayer.reset();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}	
	
	private void killVideoProcessor ()
	{
		int killDelayMs = 300;

		String ffmpegBin = new File(getDir("bin",0),"ffmpeg").getAbsolutePath();

		int procId = -1;
		
		while ((procId = ShellUtils.findProcessId(ffmpegBin)) != -1)
		{
			
			Log.d(LOGTAG, "Found PID=" + procId + " - killing now...");
			
			String[] cmd = { ShellUtils.SHELL_CMD_KILL + ' ' + procId + "" };
			
			try { 
			ShellUtils.doShellCommand(cmd,new ShellCallback ()
			{

				@Override
				public void shellOut(char[] msg) {
					// TODO Auto-generated method stub
					
				}
				
			}, false, false);
			Thread.sleep(killDelayMs); }
			catch (Exception e){}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		
	}
	

}
