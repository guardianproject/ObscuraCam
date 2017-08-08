package org.witness.ssc.video;

import android.content.Context;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class FFMPEGWrapper {

	Context context;

	public FFMPEGWrapper(Context _context) throws FileNotFoundException, IOException {
		context = _context;

		FFmpeg ffmpeg = FFmpeg.getInstance(context);
		try {
			ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

				@Override
				public void onStart() {}

				@Override
				public void onFailure() {
                    Log.e(getClass().getName(),"Failed to install binary");
                }

				@Override
				public void onSuccess() {
                    Log.i(getClass().getName(),"SUCCESS: installed binary");
                }

				@Override
				public void onFinish() {}
			});
		} catch (FFmpegNotSupportedException e) {
			// Handle if FFmpeg is not supported by device

            Log.e(getClass().getName(),"Failed to install binary",e);
		}
	}
	


	public void processVideo(File redactSettingsFile, 
			ArrayList<RegionTrail> regionTrails, File inputFile, File outputFile, String format, int mDuration,
			int iWidth, int iHeight, int oWidth, int oHeight, int frameRate, int kbitRate, String vcodec, String acodec, ExecuteBinaryResponseHandler listener) throws Exception {

        DecimalFormat df = new DecimalFormat("####0.00");

        ArrayList<String> alCmds = new ArrayList<>();

        alCmds.add("-y");
        alCmds.add("-i");
        alCmds.add(inputFile.getCanonicalPath());


        float widthMod = ((float)oWidth)/((float)iWidth);
		float heightMod = ((float)oHeight)/((float)iHeight);
		
	//	writeRedactData(redactSettingsFile, obscureRegionTrails, widthMod, heightMod, mDuration);

        alCmds.add("-b:v");
        alCmds.add("1M");

        alCmds.add("-b:a");
        alCmds.add("64k");

        alCmds.add("-vf");

        StringBuffer filters = new StringBuffer();


            for (RegionTrail trail : regionTrails)
            {

                if (trail.isDoTweening())
                {
                    int timeInc = 100;

                    for (int i = 0; i < mDuration; i = i+timeInc)
                    {
                        ObscureRegion or = trail.getCurrentRegion(i, trail.isDoTweening());
                        if (or != null)
                        {

                            int x = (int)or.getBounds().left;
                            int y = (int)or.getBounds().top;
                            int height = (int)or.getBounds().height();
                            int width = (int)or.getBounds().width();
                            String color = "black";
                            float timeStart = ((float)or.timeStamp)/1000f;
                            float timeStop = (((float)or.timeStamp)+100)/1000f;

                            float timeEnd = ((float)mDuration)/1000f;
                            timeStop = Math.max(timeStop,timeEnd);

                            filters.append("drawbox=x=" + x + ":y=" + y
                                    + ":w=" + width + ":h=" + height
                                    + ":color=" + color
                                    + ":t=max"
                                    + ":enable='between(t,"
                                    + df.format(timeStart) + "," + df.format(timeStop) + ")',");
                        }
                    }

                }
                else
                {

                    for (Integer orKey : trail.getRegionKeys())
                    {
                        ObscureRegion or = trail.getRegion(orKey);

                        int x = (int)or.getBounds().left;
                        int y = (int)or.getBounds().top;
                        int height = (int)or.getBounds().height();
                        int width = (int)or.getBounds().width();
                        String color = "black";

                        filters.append("drawbox=x=" + x + ":y=" + y
                                + ":w=" + width + ":h=" + height
                                + ":color=" + color
                                + ":t=max");

                      }


                }
            }


            Log.d(getClass().getName(),"filters: " + filters.toString());

            String filterCmd = filters.toString();

            alCmds.add(filterCmd.substring(0,filterCmd.length()-1));

            alCmds.add(outputFile.getCanonicalPath());

            String[] cmd = alCmds.toArray(new String[alCmds.size()]);

            try {


            FFmpeg ffmpeg = FFmpeg.getInstance(context);
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(cmd,listener);
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
            Log.e(getClass().getName(),"already running");
        }
	}
	
	private void writeRedactData(File redactSettingsFile, ArrayList<RegionTrail> regionTrails, float widthMod, float heightMod, int mDuration) throws IOException {
		// Write out the finger data
					
		FileWriter redactSettingsFileWriter = new FileWriter(redactSettingsFile);
		PrintWriter redactSettingsPrintWriter = new PrintWriter(redactSettingsFileWriter);
		ObscureRegion or = null, lastOr = null;
		String orData = "";
		
		for (RegionTrail trail : regionTrails)
		{
			
			if (trail.isDoTweening())
			{
				int timeInc = 100;
				
				for (int i = 0; i < mDuration; i = i+timeInc)
				{
					or = trail.getCurrentRegion(i, trail.isDoTweening());
					if (or != null)
					{
						orData = or.getStringData(widthMod, heightMod,i,timeInc, trail.getObscureMode());
						redactSettingsPrintWriter.println(orData);
					}
				}
				
			}
			else
			{
				
				for (Integer orKey : trail.getRegionKeys())
				{
					or = trail.getRegion(orKey);
					
					if (lastOr != null)
					{
						
						orData = lastOr.getStringData(widthMod, heightMod,or.timeStamp,or.timeStamp-lastOr.timeStamp, trail.getObscureMode());
					}
					
					redactSettingsPrintWriter.println(orData);
					
					lastOr = or;
				}
				
				if (or != null)
				{
					orData = lastOr.getStringData(widthMod, heightMod,or.timeStamp,or.timeStamp-lastOr.timeStamp, trail.getObscureMode());
					redactSettingsPrintWriter.println(orData);
				}
			}
		}
		
		redactSettingsPrintWriter.flush();
		
		redactSettingsPrintWriter.close();

				
	}
	

}


