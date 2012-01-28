package org.witness.sscphase1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.witness.informa.utils.InformaConstants;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class Wizard extends Activity implements OnClickListener {
	int current;
	
	BroadcastReceiver br;
	
	LinearLayout progress, holder;
	TextView frameTitle;
	Button wizard_next, wizard_back;
	
	WizardForm wizardForm;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wizard);
		
		current = getIntent().getIntExtra("current", 0);
		wizardForm = new WizardForm(this);
		
		frameTitle = (TextView) findViewById(R.id.wizard_frame_title);
		
		wizard_next = (Button) findViewById(R.id.wizard_next);
		if(current < wizardForm.frames.length() - 1)
			wizard_next.setOnClickListener(this);
		else {
			//TODO: destroy back/next, replace with done.
			wizard_next.setAlpha(0.3f);
			wizard_next.setClickable(false);
		}
		
		wizard_back = (Button) findViewById(R.id.wizard_back);
		if(current > 0)
			wizard_back.setOnClickListener(this);
		else {
			wizard_back.setAlpha(0.3f);
			wizard_back.setClickable(false);
		}
		
		
		progress = (LinearLayout) findViewById(R.id.wizard_progress);
		holder = (LinearLayout) findViewById(R.id.wizard_holder);
	}
	
	public void initFrame() throws JSONException {
		final ProgressCircle[] circles = new ProgressCircle[wizardForm.frames.length()];
		for(int x = 0; x < wizardForm.frames.length(); x++) {
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
		
		wizardForm.setFrame(current);
		frameTitle.setText(wizardForm.getTitle());
		
		ArrayList<View> views = wizardForm.getContent();
		for(View v : views)
			holder.addView(v);
	}
	
	private void setUserPGP() {
		Log.d(InformaConstants.TAG, "getting the user PGP key...");
	}
	
	private class WizardForm extends JSONObject {
		Context _c;
		JSONArray frames, order;
		JSONObject currentFrame;
		
		public final static String frameKey = "frameKey";
		public final static String frameTitle = "frameTitle";
		public final static String frameContent = "frameContent";
		public final static String frameOrder = "frameOrder";
		public final static String allFrames = "frames";
		
		public WizardForm(Context c) {
			_c = c;
			frames = new JSONArray();
			order = new JSONArray();
			
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
					if(f.compareTo("order.wizard") == 0) {
						for(String s : sb.toString().split(",")) {
							order.put(s);
						}
					} else {
						JSONObject frame = new JSONObject();
						frame.put(frameKey, f);
						frame.put(frameTitle, parseAsTitle(f));
						frame.put(frameContent, sb.toString());
						frames.put(frame);	
					}
					
					br.close();
				}
				this.put(frameOrder, order);
				this.put(allFrames, frames);
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
		
		public void setFrame(int which) throws JSONException {
			Log.d(InformaConstants.TAG, order.toString());
			for(int f=0; f<frames.length(); f++) {
				JSONObject frame = frames.getJSONObject(f);
				if(frame.getString(frameKey).compareTo(order.getString(which)) == 0)
					currentFrame = frame;
			}
		}
		
		private String findKey(String content, String key) {
			if(content.indexOf(key) != -1) {
				String keyTail = content.substring(content.indexOf(key + "="));
				String[] pair = keyTail.substring(0, keyTail.indexOf(";")).split("=");
				return pair[1];
			} else {
				return null;
			}
		}
		
		public ArrayList<View> getContent() throws JSONException {
			ArrayList<View> views = new ArrayList<View>();
			String content = currentFrame.getString(frameContent);
			
			for(final String s : content.split("\n")) {
				if(s.contains("{$")) {
					String type = findKey(s, "type");
					final String callback = findKey(s, "callback");
					
					if(type.compareTo("button") == 0) {
						Button button = new Button(_c);
						button.setText(findKey(s, "text"));
						button.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								if(callback != null)
									try {
										doCallback(callback, null);
									} catch (IllegalAccessException e) {
										Log.d(InformaConstants.TAG, e.toString());
									} catch (NoSuchMethodException e) {
										Log.d(InformaConstants.TAG, e.toString());
									} catch (InvocationTargetException e) {
										Log.d(InformaConstants.TAG, e.toString());
									}
							}
							
						});
						views.add(button);
						
					} else if(type.compareTo("input") == 0) {
						EditText edittext = new EditText(_c);
						views.add(edittext);
					} else if(type.compareTo("password") == 0) {
						EditText edittext = new EditText(_c);
						edittext.setInputType(InputType.TYPE_NUMBER_VARIATION_PASSWORD);
						edittext.setOnFocusChangeListener(new TextView.OnFocusChangeListener() {

							@Override
							public void onFocusChange(View v, boolean hasFocus) {
								Log.d(InformaConstants.TAG, "FOCUS CHANGED!");
								
							}
							
						});
						views.add(edittext);
					} else if(type.compareTo("select_one") == 0) {
						
					} else if(type.compareTo("select_multi") == 0) {
						
					}
				} else {
					TextView tv = new TextView(_c);
					tv.setText(s);
					views.add(tv);
				}
			}
			
			return views;
		}
		
		private void doCallback(String callback, String[] args) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
			Method method = Wizard.this.getClass().getDeclaredMethod(callback, null);
			method.setAccessible(true);
			method.invoke(Wizard.this, args);
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
				finish();
			}
		} else if(v == wizard_next) {
			if(current < wizardForm.frames.length() - 1) {
				Intent i = new Intent(this,Wizard.class);
				i.putExtra("current", current + 1);
				startActivity(i);
				finish();
			}
		}
		
	}

}
