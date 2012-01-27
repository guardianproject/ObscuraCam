package org.witness.sscphase1;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.witness.informa.utils.InformaConstants;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class Wizard extends Activity implements OnClickListener {
	int current;
	
	ScrollView sv;
	LinearLayout progress, holder;
	TextView frameTitle;
	Button wizard_next, wizard_back;
	
	WizardFiles wizardFiles;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wizard);
		
		frameTitle = (TextView) findViewById(R.id.wizard_frame_title);
		
		wizard_next = (Button) findViewById(R.id.wizard_next);
		wizard_next.setOnClickListener(this);
		
		wizard_back = (Button) findViewById(R.id.wizard_back);
		wizard_back.setOnClickListener(this);
		
		sv = (ScrollView) findViewById(R.id.wizard_sv);
		progress = (LinearLayout) findViewById(R.id.wizard_progress);
		holder = (LinearLayout) findViewById(R.id.wizard_holder);
		
		wizardFiles = new WizardFiles(this);
		
		current = getIntent().getIntExtra("current", 0);
		Log.d(InformaConstants.TAG, "CURRENT PAGE: " + current);
	}
	
	public void initFrame() throws JSONException {
		final ProgressCircle[] circles = new ProgressCircle[wizardFiles.frames.length()];
		for(int x = 0; x < wizardFiles.frames.length(); x++) {
			int color = Color.GRAY;
			if(x == current)
				color = Color.CYAN;
			
			circles[x] = new ProgressCircle(color, x * 70);
			
		}
		
		progress.setBackgroundDrawable(new Drawable() {
			private Paint p = new Paint();
			
			@Override
			public void draw(Canvas canvas) {
				for(ProgressCircle pc : circles) {
					p.setColor(pc.color);
					canvas.drawCircle(pc.x, pc.y, pc.r, p);
				}
				
			}

			@Override
			public int getOpacity() {
				return 0;
			}

			@Override
			public void setAlpha(int alpha) {}

			@Override
			public void setColorFilter(ColorFilter cf) {}
			
		});
		
		wizardFiles.setFrame(current);
		frameTitle.setText(wizardFiles.getTitle());
	}
	
	private class WizardFiles extends JSONObject {
		Context _c;
		JSONArray frames;
		JSONObject currentFrame;
		
		public final static String frameTitle = "frameTitle";
		public final static String frameContent = "frameContent";
		public final static String frameOrder = "frameOrder";
		public final static String allFrames = "frames";
		
		public WizardFiles(Context c) {
			_c = c;
			frames = new JSONArray();
			
			// get the list of files within assets/wizard
			try {
				String[] allFiles = _c.getAssets().list("wizard");
				for(String f : allFiles) {
					// get the file
					BufferedReader br = new BufferedReader(new InputStreamReader(_c.getAssets().open("wizard/" + f)));
					String line;
					StringBuilder sb = new StringBuilder();
					while((line = br.readLine()) != null)
						sb.append(line).append('\n');
					
					// if the file is not "order.json"
					if(f.compareTo("order.json") == 0)
						this.put(frameOrder, sb);
					else {
						JSONObject frame = new JSONObject();
						frame.put(frameTitle, parseAsTitle(f));
						frame.put(frameContent, parseAsFrame(sb.toString()));
						frames.put(frame);	
					}
					
					br.close();
				}
				this.put(allFrames, frames);
				Log.d(InformaConstants.TAG, this.toString());
			} catch (IOException e) {
				Log.e(InformaConstants.TAG, e.toString());
			} catch (JSONException e) {
				Log.e(InformaConstants.TAG, e.toString());
			}			
		}
		
		private String parseAsTitle(String rawTitle) {
			String[] words = rawTitle.split("_");
			StringBuffer sb = new StringBuffer();
			for(String word : words) {
				sb.append(word + " ");
			}
			
			return sb.toString().substring(0, sb.length() - 1);
		}
		
		private String parseAsFrame(String rawStream) {
			return rawStream;
		}
		
		public void setFrame(int which) throws JSONException {
			currentFrame = this.frames.getJSONObject(which);
		}
		
		public View getContent() {
			View v = new View(_c);
			return v;
		}
		
		public String getTitle() throws JSONException {
			return currentFrame.getString(frameTitle);
		}
	}
	
	private class ProgressCircle  {
		float x;
		int color;
		float y = 30f;
		float r = 8f;
		
		public ProgressCircle(int color, float x) {
			this.x = x + 20f;
			this.color = color;
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		try {
			initFrame();
		} catch(JSONException e) {
			Log.e(InformaConstants.TAG, e.toString());
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onClick(View v) {
		if(v == wizard_back) {
			if(current > 0) {
				Intent i = new Intent(this,Wizard.class);
				i.putExtra("current", current - 1);
				startActivity(i);
			}
		} else if(v == wizard_next) {
			if(current < wizardFiles.frames.length() - 1) {
				Intent i = new Intent(this,Wizard.class);
				i.putExtra("current", current + 1);
				startActivity(i);
			}
		}
		
	}

}
