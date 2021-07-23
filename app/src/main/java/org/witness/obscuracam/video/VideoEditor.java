/*
 * 
 * To Do:
 * 
 * Handles on SeekBar - Not quite right
 * Editing region, not quite right
 * RegionBarArea will be graphical display of region tracks, no editing, just selecting
 * 
 */

package org.witness.obscuracam.video;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import com.daasuu.gpuv.composer.FillMode;
import com.daasuu.gpuv.composer.GPUMp4Composer;
import com.daasuu.gpuv.composer.Rotation;
import com.daasuu.gpuv.egl.filter.GlBoxBlurFilter;
import com.daasuu.gpuv.egl.filter.GlFilter;
import com.daasuu.gpuv.egl.filter.GlFilterGroup;
import com.daasuu.gpuv.egl.filter.GlMonochromeFilter;
import com.daasuu.gpuv.egl.filter.GlPixelationFilter;
import com.daasuu.gpuv.egl.filter.GlPosterizeFilter;
import com.daasuu.gpuv.egl.filter.GlVignetteFilter;
import com.daasuu.gpuv.egl.filter.GlZoomBlurFilter;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.VideoView;


import org.apache.commons.io.IOUtils;
import org.witness.obscuracam.photo.detect.AndroidFaceDetection;
import org.witness.obscuracam.photo.detect.DetectedFace;
import org.witness.obscuracam.photo.filters.PixelizeObscure;
import org.witness.obscuracam.ObscuraApp;
import org.witness.sscphase1.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

import static android.os.Environment.DIRECTORY_MOVIES;

public class VideoEditor extends AppCompatActivity implements
        OnCompletionListener, OnErrorListener, OnInfoListener,
        OnBufferingUpdateListener, OnPreparedListener, OnSeekCompleteListener,
        OnVideoSizeChangedListener, SurfaceHolder.Callback,
        MediaController.MediaPlayerControl,
        InOutPlayheadSeekBar.InOutPlayheadSeekBarChangeListener {

    public static final String LOGTAG = ObscuraApp.TAG;

    public static final int SHARE = 1;

    private final static float REGION_CORNER_SIZE = 26;

    private final static String MIME_TYPE_MP4 = "video/mp4";
    private final static String MIME_TYPE_VIDEO = "video/*";

    private final static int FACE_TIME_BUFFER = 2000;

    private final static int HUMAN_OFFSET_BUFFER = 50;

    int completeActionFlag = 3;

    Uri originalVideoUri;
    Uri currentUri;
    boolean mIsPreview = false;

    File fileExternDir;
    File redactSettingsFile;
    File saveFile;
    File recordingFile;

    Display currentDisplay;

    VideoView videoView;
    SurfaceHolder surfaceHolder;
    MediaPlayer mediaPlayer;

    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    PixelizeObscure po = new PixelizeObscure();

    ImageView regionsView;
    Bitmap obscuredBmp;
    Canvas obscuredCanvas;
    Paint obscuredPaint;
    Paint selectedPaint;

    Bitmap bitmapPixel;

    InOutPlayheadSeekBar mVideoSeekbar;
    //RegionBarArea regionBarArea;

    int videoWidth = 0;
    int videoHeight = 0;

    ImageButton playPauseButton;

    private ArrayList<RegionTrail> obscureTrails = new ArrayList<RegionTrail>();
    private RegionTrail activeRegionTrail;
    private ObscureRegion activeRegion;

    boolean mAutoDetectEnabled = false;
    boolean eyesOnly = false;
    int autoDetectTimeInterval = 300; //ms

    boolean mCompressVideo = true;
    int mObscureBlurAmount = 0;
    int mObscureVideoAmount = 0;

    int timeNudgeOffset = 2;

    float vRatio;

    int outFrameRate = -1;
    int outBitRate = -1;
    String outFormat = null;
    String outAcodec = null;
    String outVcodec = null;
    int outVWidth = -1;
    int outVHeight = -1;

    private final static String DEFAULT_OUT_FPS = "15";
    private final static String DEFAULT_OUT_RATE = "300";
    private final static String DEFAULT_OUT_FORMAT = "mp4";
    private final static String DEFAULT_OUT_VCODEC = "libx264";
    private final static String DEFAULT_OUT_ACODEC = "copy";
    private final static String DEFAULT_OUT_WIDTH = "480";
    private final static String DEFAULT_OUT_HEIGHT = "320";

    private ProgressBar mProgressBar;


    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0: //status


                    break;
                case 1: //status


                    try {
                        if (msg.getData().containsKey("progress")) {
                            mProgressBar.setProgress((int)(msg.getData().getDouble("progress")*100f));
                        }
                    }
                    catch (Exception e)
                    {
                        //handle text parsing errors
                    }

                    break;

                case 2: //cancelled
                    mCancelled = true;
                    mAutoDetectEnabled = false;
                    killVideoProcessor();
                    mProgressBar.setVisibility(View.GONE);
                    break;

                case 3: //completed
                    askPostProcessAction();

                    mProgressBar.setVisibility(View.GONE);
                    break;

                case 5:

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    private boolean mCancelled = false;


    private int mDuration;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());


        setContentView(R.layout.videoeditor);


        if (getIntent() != null) {
            // Passed in from ObscuraApp
            originalVideoUri = getIntent().getData();

            if (originalVideoUri == null) {
                if (getIntent().hasExtra(Intent.EXTRA_STREAM)) {
                    originalVideoUri = (Uri) getIntent().getExtras().get(Intent.EXTRA_STREAM);
                }
            }

            if (originalVideoUri == null) {
                if (savedInstanceState.getString("path") != null) {
                    originalVideoUri = Uri.fromFile(new File(savedInstanceState.getString("path")));
                    recordingFile = new File(savedInstanceState.getString("path"));
                } else {
                    finish();
                    return;
                }
            } else {

                recordingFile = new File(pullPathFromUri(originalVideoUri));
            }
        }

        fileExternDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES);

        mAutoDetectEnabled = false; //first time do autodetect

        setPrefs();

        try {
            retriever.setDataSource(recordingFile.getAbsolutePath());

            bitmapPixel = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_context_pixelate);

        } catch (RuntimeException re) {
            Toast.makeText(this, "There was an error with the video file", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void resetMediaPlayer(Uri videoUri) {
        Log.i(LOGTAG, "releasing/loading media Player");

        mediaPlayer.release();

        loadMedia(videoUri);

        mediaPlayer.setDisplay(surfaceHolder);

        mediaPlayer.setScreenOnWhilePlaying(true);

        try {
            mediaPlayer.prepare();
            mDuration = mediaPlayer.getDuration();

            mVideoSeekbar.setMax(mDuration);

        } catch (Exception e) {
            Log.v(LOGTAG, "IllegalStateException " + e.getMessage());
            finish();
        }

        seekTo(0);

    }

    private void loadMedia(Uri uriVideo) {

        currentUri = uriVideo;

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnVideoSizeChangedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);

        mediaPlayer.setLooping(true);

        try {
            mediaPlayer.setDataSource(this, currentUri);

        } catch (IllegalArgumentException e) {
            Log.e(LOGTAG, originalVideoUri.toString() + ": " + e.getMessage());

        } catch (IllegalStateException e) {
            Log.e(LOGTAG, originalVideoUri.toString() + ": " + e.getMessage());

        } catch (IOException e) {
            Log.e(LOGTAG, originalVideoUri.toString() + ": " + e.getMessage());

        }

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.putString("path", recordingFile.getAbsolutePath());

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Log.v(LOGTAG, "surfaceCreated Called");
        if (mediaPlayer != null) {

            mediaPlayer.setDisplay(holder);
            mediaPlayer.setScreenOnWhilePlaying(true);

            try {
                mediaPlayer.prepare();
                mDuration = mediaPlayer.getDuration();

                mVideoSeekbar.setMax(mDuration);

            } catch (Exception e) {
                Log.v(LOGTAG, "IllegalStateException " + e.getMessage());
                finish();
            }


            updateVideoLayout();

        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        Log.i(LOGTAG, "SurfaceHolder changed");


    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        Log.i(LOGTAG, "SurfaceHolder destroyed");

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.i(LOGTAG, "onCompletion Called");

        playPauseButton.setImageDrawable(this.getResources().getDrawable(android.R.drawable.ic_media_play));
    }

    @Override
    public boolean onError(MediaPlayer mp, int whatError, int extra) {
        Log.e(LOGTAG, "onError Called");
        if (whatError == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            Log.e(LOGTAG, "Media Error, Server Died " + extra);



        } else if (whatError == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
            Log.e(LOGTAG, "Media Error, Error Unknown " + extra);
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
        //	Log.v(LOGTAG, "onPrepared Called");

        updateVideoLayout();
        mediaPlayer.seekTo(1);


    }

    public void onSeekComplete(MediaPlayer mp) {

        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            mediaPlayer.pause();
            playPauseButton.setImageDrawable(this.getResources().getDrawable(android.R.drawable.ic_media_play));
        }
    }

    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.v(LOGTAG, "onVideoSizeChanged Called");

        videoWidth = mp.getVideoWidth();
        videoHeight = mp.getVideoHeight();

        updateVideoLayout();

    }

    /*
     * Handling screen configuration changes ourselves, we don't want the activity to restart on rotation
     */
    @Override
    public void onConfigurationChanged(Configuration conf) {
        super.onConfigurationChanged(conf);


    }

    private boolean updateVideoLayout() {
        //Get the dimensions of the video
        int videoWidth = mediaPlayer.getVideoWidth();
        int videoHeight = mediaPlayer.getVideoHeight();
        //  Log.v(LOGTAG, "video size: " + videoWidth + "x" + videoHeight);

        if (videoWidth > 0 && videoHeight > 0) {
            //Get the width of the screen
            int screenWidth = getWindowManager().getDefaultDisplay().getWidth();

            //Get the SurfaceView layout parameters
            android.view.ViewGroup.LayoutParams lp = videoView.getLayoutParams();

            //Set the width of the SurfaceView to the width of the screen
            lp.width = screenWidth;

            //Set the height of the SurfaceView to match the aspect ratio of the video
            //be sure to cast these as floats otherwise the calculation will likely be 0

            int videoScaledHeight = (int) (((float) videoHeight) / ((float) videoWidth) * (float) screenWidth);

            lp.height = videoScaledHeight;

            //Commit the layout parameters
            videoView.setLayoutParams(lp);
//            regionsView.setLayoutParams(lp);

            // Log.v(LOGTAG, "view size: " + screenWidth + "x" + videoScaledHeight);

            vRatio = ((float) screenWidth) / ((float) videoWidth);

            //	Log.v(LOGTAG, "video/screen ration: " + vRatio);

            return true;
        } else
            return false;
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
        Log.v(LOGTAG, "Calling our getDuration method");
        return mediaPlayer.getDuration();
    }

    @Override
    public boolean isPlaying() {
        Log.v(LOGTAG, "Calling our isPlaying method");
        return mediaPlayer.isPlaying();
    }

    @Override
    public void pause() {
        Log.v(LOGTAG, "Calling our pause method");
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playPauseButton.setImageDrawable(this.getResources().getDrawable(android.R.drawable.ic_media_play));
        }
    }

    @Override
    public void seekTo(int pos) {
        mediaPlayer.seekTo(pos);

    }

    @Override
    public void start() {
        Log.v(LOGTAG, "Calling our start method");
        mediaPlayer.start();

        playPauseButton.setImageDrawable(this.getResources().getDrawable(android.R.drawable.ic_media_pause));

        mHandler.post(updatePlayProgress);


    }

    private Runnable updatePlayProgress = new Runnable() {
        public void run() {

            try {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int curr = mediaPlayer.getCurrentPosition();
                    mVideoSeekbar.setProgress(curr);

                    mHandler.post(this);
                }

            } catch (Exception e) {
                Log.e(LOGTAG, "autoplay errored out", e);
            }
        }
    };


    public String pullPathFromUri(Uri originalUri) {
        String originalVideoFilePath = originalUri.toString();
        String[] columnsToSelect = {MediaStore.Video.Media.DATA};
        Cursor videoCursor = getContentResolver().query(originalUri, columnsToSelect, null, null, null);
        if (videoCursor != null && videoCursor.getCount() == 1) {
            videoCursor.moveToFirst();
            originalVideoFilePath = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Images.Media.DATA));
        }

        return originalVideoFilePath;
    }


    public final static int PLAY = 1;
    public final static int STOP = 2;
    public final static int PROCESS = 3;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.video_editor_menu, menu);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {


            case R.id.menu_save:

                if (saveFile != null && saveFile.exists())
                    addVideoToGallery(saveFile);

                return true;

            case R.id.menu_share:

                shareVideo();

                return true;

            case android.R.id.home:
                // Pull up about screen
                finish();

                return true;


            default:
                return false;
        }
    }

    private GPUMp4Composer mVideoProc = null;

    private synchronized void processVideo() {

        if (mVideoProc != null)
        {
            mVideoProc.cancel();
            saveFile.delete();

        }

        mProgressBar.setIndeterminate(false);
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setProgress(0);

        try {
            saveFile = File.createTempFile("obscura",".mp4");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCancelled = false;

        mediaPlayer.pause();

            GlPixelationFilter filterPixel = null;

            if (mObscureVideoAmount > 0) {
                filterPixel = new GlPixelationFilter();
                filterPixel.setPixel(mObscureVideoAmount);
            }

            GlPosterizeFilter filterBlur = null;

            if (mObscureBlurAmount > 0) {
                filterBlur = new GlPosterizeFilter();
                //filterBlur.setBlurSize(mObscureBlurAmount);
                filterBlur.setColorLevels(mObscureBlurAmount);
            }

            GlFilterGroup filterGroup = null;


            if (filterPixel != null && filterBlur != null)
                filterGroup = new GlFilterGroup(filterPixel,filterBlur);
            else if (filterPixel != null)
                filterGroup = new GlFilterGroup(filterPixel);
            else if (filterBlur != null)
                filterGroup = new GlFilterGroup(filterBlur);

            boolean muteAudio = ((CheckBox)findViewById(R.id.cb_audio_mute)).isChecked();


        mVideoProc = new GPUMp4Composer(recordingFile.getAbsolutePath(), saveFile.getAbsolutePath())
                .fillMode(FillMode.PRESERVE_ASPECT_FIT)
                .filter(filterGroup)
                .mute(muteAudio)
                .listener(new GPUMp4Composer.Listener() {
                    @Override
                    public void onProgress(double progress) {
                     //   Log.d(LOGTAG, "onProgress = " + progress);


                        Message msg = mHandler.obtainMessage(1);
                        msg.getData().putDouble("progress", progress);
                        mHandler.sendMessage(msg);
                    }

                    @Override
                    public void onCompleted() {
                        Log.d(LOGTAG, "onCompleted()");
                        runOnUiThread(() -> {


                            Message msg = mHandler.obtainMessage(completeActionFlag);
                            msg.getData().putString("status", "complete");
                            mHandler.sendMessage(msg);
                        });
                    }

                    @Override
                    public void onCanceled() {
                        Log.d(LOGTAG, "onCanceled");
                    }

                    @Override
                    public void onFailed(Exception exception) {
                        Log.e(LOGTAG, "onFailed()", exception);
                    }
                });

        mVideoProc.start();

            // Could make some high/low quality presets
            /**
            ffmpeg.processVideo(obscureTrails, recordingFile, saveFile,
                    frameRate, startTime, duration, mCompressVideo, mObscureVideoAmount, mObscureAudioAmount,
                    new ExecuteBinaryResponseHandler() {

                        @Override
                        public void onStart() {
                        }

                        @Override
                        public void onProgress(String message) {

                            Log.i(getClass().getName(), "PROGRESS: " + message);
                            //frame=  144 fps=0.6 q=29.0 size=    1010kB time=00:00:05.01 bitrate=1649.7kbits/s dup=3 drop=0 speed=0.0192x

                            Message msg = mHandler.obtainMessage(1);
                            msg.getData().putString("status", message);

                            if (message.indexOf("time=")!=-1) {
                                int timeidx = message.indexOf("time=")+5;
                                String time = message.substring(timeidx,message.indexOf(" ",timeidx));
                                msg.getData().putString("time", time);
                            }

                            mHandler.sendMessage(msg);

                        }

                        @Override
                        public void onFailure(String message) {

                            Log.w(getClass().getName(), "FAILURED: " + message);

                        }

                        @Override
                        public void onSuccess(String message) {

                            Log.i(getClass().getName(), "SUCCESS: " + message);

                            if (!mIsPreview)
                                addVideoToGallery(saveFile);

                            Message msg = mHandler.obtainMessage(completeActionFlag);
                            msg.getData().putString("status", "complete");
                            mHandler.sendMessage(msg);
                        }

                        @Override
                        public void onFinish() {

                            Log.i(getClass().getName(), "FINISHED");


                        }
                    });
        } catch (Exception e) {
            Log.e(LOGTAG, "error with ffmpeg", e);
        }

            **/
    }

    private void addVideoToGallery(File videoToAdd) {

        if (mSnackbar != null)
        {
            mSnackbar.dismiss();
            mSnackbar = null;
        }

        File fileExport = saveVideoExternal(videoToAdd);

        if (fileExport != null) {
            mSnackbar = Snackbar.make(findViewById(R.id.frameRoot), R.string.processing_complete, Snackbar.LENGTH_LONG);
            mSnackbar.setAction(R.string.action_open, new OnClickListener() {
                @Override
                public void onClick(View view) {
                    playVideoExternal(fileExport);
                }
            });
            mSnackbar.show();

            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileExport)));
        }

    }

    public boolean isStoragePermissionGranted() {
        String TAG = "Storage Permission";
        if (Build.VERSION.SDK_INT >= 23) {
            if (this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }

    public File saveVideoExternal(File saveFile) {
        if (isStoragePermissionGranted()) { // check or ask permission
            File fileMediaExport = null;
            try {
                SecureRandom random = new SecureRandom();
                int num = random.nextInt(0x1000000);
                String formatted = String.format("%06x", num);
                String fileName = formatted + "-" + saveFile.getName();

                fileMediaExport = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES),fileName);


                IOUtils.copyLarge(new FileInputStream(saveFile),new FileOutputStream(fileMediaExport));

            } catch (Exception e) {
                e.printStackTrace();
            }

            if (fileMediaExport != null)
            MediaScannerConnection.scanFile(this, new String[]{fileMediaExport.toString()}, new String[]{fileMediaExport.getName()}, null);

            return fileMediaExport;
        }

        return null;
    }

    Snackbar mSnackbar;

    private void askPostProcessAction() {
        if (saveFile != null && saveFile.exists()) {

            resetMediaPlayer(Uri.fromFile(saveFile));
            start();

        }

    }

    private void showFailure (String message)
    {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.frameRoot), message, Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    private void playVideoExternal(File fileExported) {

        if (saveFile != null && saveFile.exists()) {

            Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(fileExported), MIME_TYPE_VIDEO);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            startActivity(intent);
        }
    }

    private void shareVideo() {


        if (saveFile != null && saveFile.exists()) {

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(MIME_TYPE_VIDEO);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(saveFile));
            startActivityForResult(Intent.createChooser(intent, "Share Video"), 0);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

    }

    @Override
    public void inOutValuesChanged(int thumbInValue, int thumbOutValue) {
		/*
		if (activeRegionTrail != null) {
			
			activeRegionTrail.setStartTime(thumbInValue);
			activeRegionTrail.setEndTime(thumbOutValue);
		}*/
    }


    @Override
    protected void onPause() {

        super.onPause();
        mediaPlayer.reset();

    }

    @Override
    public void onDestroy() {

        super.onDestroy();

    }


    @Override
    protected void onStop() {
        super.onStop();
        this.mAutoDetectEnabled = false;
    }

    private void killVideoProcessor() {

    }

    @Override
    protected void onResume() {
        super.onResume();

        videoView = (VideoView) this.findViewById(R.id.SurfaceView);

        mProgressBar = (ProgressBar) this.findViewById(R.id.progress_spinner);

        surfaceHolder = videoView.getHolder();

        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        currentDisplay = getWindowManager().getDefaultDisplay();

        mVideoSeekbar = (InOutPlayheadSeekBar) this.findViewById(R.id.InOutPlayheadSeekBar);

        mVideoSeekbar.setIndeterminate(false);
        mVideoSeekbar.setSecondaryProgress(0);
        mVideoSeekbar.setProgress(0);
        mVideoSeekbar.setInOutPlayheadSeekBarChangeListener(this);
        mVideoSeekbar.setThumbsInactive();
        mVideoSeekbar.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mediaPlayer.seekTo(mVideoSeekbar.getProgress());

                return false;
            }
        });

        playPauseButton = (ImageButton) this.findViewById(R.id.PlayPauseImageButton);
        playPauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    playPauseButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
                    mAutoDetectEnabled = false;
                } else {
                    if (currentUri != originalVideoUri)
                    {
                        resetMediaPlayer(originalVideoUri);
                        seekTo(0);
                    }
                    start();


                }
            }
        });


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

        /**
        findViewById(R.id.button_auto).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                beginAutoDetect();
            }
        });**/

        ((SeekBar)findViewById(R.id.seekbar_video_obscure)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                //make it even!
                if (i%2!=0) {
                    i+=1;
                }

                mObscureVideoAmount = i;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                processVideo();
            }
        });


        ((SeekBar)findViewById(R.id.seekbar_blur)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mObscureBlurAmount = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                processVideo();
            }
        });

        setPrefs();

        loadMedia(originalVideoUri);


    }

    private void setPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        eyesOnly = prefs.getBoolean("pref_eyes_only", false);

        outFrameRate = Integer.parseInt(prefs.getString("pref_out_fps", DEFAULT_OUT_FPS).trim());
        outBitRate = Integer.parseInt(prefs.getString("pref_out_rate", DEFAULT_OUT_RATE).trim());
        outFormat = DEFAULT_OUT_FORMAT;
        outAcodec = prefs.getString("pref_out_acodec", DEFAULT_OUT_ACODEC).trim();
        outVcodec = prefs.getString("pref_out_vcodec", DEFAULT_OUT_VCODEC).trim();

        outVWidth = Integer.parseInt(prefs.getString("pref_out_vwidth", DEFAULT_OUT_WIDTH).trim());
        outVHeight = Integer.parseInt(prefs.getString("pref_out_vheight", DEFAULT_OUT_HEIGHT).trim());

    }


    public int getAudioSessionId() {
        return 1;
    }


}
