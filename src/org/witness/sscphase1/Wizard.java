package org.witness.sscphase1;

import info.guardianproject.database.sqlcipher.SQLiteDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.witness.informa.utils.InformaConstants;
import org.witness.informa.utils.io.DatabaseHelper;
import org.witness.informa.utils.secure.Apg;
import org.witness.securesmartcam.utils.Selections;
import org.witness.securesmartcam.utils.SelectionsAdapter;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Wizard extends Activity implements OnClickListener {
	int current;
		
	LinearLayout progress, holder, navigation_holder;
	TextView frameTitle;
	Button wizard_next, wizard_back, wizard_done;
	
	private SharedPreferences preferences;
	private SharedPreferences.Editor _ed;
	
	WizardForm wizardForm;
	
	Apg apg;
	DatabaseHelper dh;
	SQLiteDatabase db;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wizard);
		
		preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		_ed = preferences.edit();
		
		current = getIntent().getIntExtra("current", 0);
		wizardForm = new WizardForm(this);
		
		frameTitle = (TextView) findViewById(R.id.wizard_frame_title);
		progress = (LinearLayout) findViewById(R.id.wizard_progress);
		holder = (LinearLayout) findViewById(R.id.wizard_holder);
		navigation_holder = (LinearLayout) findViewById(R.id.wizard_navigation_holder);
		
		wizard_done = (Button) findViewById(R.id.wizard_done);
		wizard_back = (Button) findViewById(R.id.wizard_back);
		wizard_next = (Button) findViewById(R.id.wizard_next);
		
		if(current < wizardForm.frames.length() - 1)
			wizard_next.setOnClickListener(this);
		else {
			wizard_next.setVisibility(View.GONE);
			wizard_back.setVisibility(View.GONE);
			
			wizard_done.setVisibility(View.VISIBLE);
			wizard_done.setOnClickListener(this);
		}
		
		if(current > 0)
			wizard_back.setOnClickListener(this);
		else {
			setMandatory(wizard_back);
		}
		
		try {
			initFrame();
		} catch(JSONException e) {
			Log.e(InformaConstants.TAG, e.toString());
		}
	}
	
	public void setMandatory(View v) {
		((Button) v).setAlpha(0.3f);
		((Button) v).setClickable(false);
	}
	
	public void enableAction(View v) {
		((Button) v).setAlpha(1.0f);
		((Button) v).setClickable(true);
	}
	
	public void makeToast(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}
	
	public void initFrame() throws JSONException {
		wizardForm.setFrame(current);
		frameTitle.setText(wizardForm.getTitle());

		ArrayList<View> views = wizardForm.getContent();
		for(View v : views)
			holder.addView(v);
	}
	
	@SuppressWarnings("unused")
	private void getUserPGP() {
		apg = Apg.getInstance();
		if(!apg.isAvailable(getApplicationContext()))
			makeToast(getResources().getString(R.string.wizard_error_no_apg));
		else {
			apg.selectSecretKey(this);
		}
	}
	
	private void setUserPGP() {
		Log.d(InformaConstants.TAG, "opening up database...");
		
		SQLiteDatabase.loadLibs(this);
		
		dh = new DatabaseHelper(this);
		db = dh.getWritableDatabase(preferences.getString(InformaConstants.Settings.HAS_DB_PASSWORD, ""));
		
		dh.setTable(db, InformaConstants.Tables.INFORMA_SETUP);
		
		long localTimestamp = System.currentTimeMillis();
		
		ContentValues cv = new ContentValues();
		cv.put(InformaConstants.Tables.Setup.SIG_KEY_ID, apg.getSignatureKeyId());
		cv.put(InformaConstants.Tables.Setup.DEFAULT_SECURITY_LEVEL, InformaConstants.Keys.SecurityLevels.UNENCRYPTED_NOT_SHARABLE);
		cv.put(InformaConstants.Tables.Setup.LOCAL_TIMESTAMP, localTimestamp);
		cv.put(InformaConstants.Tables.Setup.PUBLIC_TIMESTAMP, getPublicTimestamp(localTimestamp));
		
		long insert = db.insert(dh.getTable(), null, cv);
		if(insert != 0)
			enableAction(wizard_next);
		
		db.close();
	}
	
	private long getPublicTimestamp(long ts) {
		//TODO public timestamp?
		return ts;
	}
	
	@SuppressWarnings("unused")
	private void saveDBPW(String pw) {
		_ed.putString(InformaConstants.Settings.HAS_DB_PASSWORD, pw).commit();
	}
	
	@SuppressWarnings("unused")
	private void setDBPWCache(ArrayList<Selections> cacheSelection) {
		for(Selections s : cacheSelection) {
			if(s.getSelected())
				_ed.putString(InformaConstants.Settings.DB_PASSWORD_CACHE_TIMEOUT, String.valueOf(cacheSelection.indexOf(s) + 200)).commit();
		}
	}
	
	@SuppressWarnings("unused")
	private void setDefaultImageHandling(ArrayList<Selections> imageHandlingSelection) {
		for(Selections s : imageHandlingSelection) {
			if(s.getSelected())
				_ed.putString(InformaConstants.Settings.DEFAULT_IMAGE_HANDLING, String.valueOf(imageHandlingSelection.indexOf(s) + 300)).commit();
		}
	}
	
	@SuppressWarnings("unused")
	private String[] getDefaultImageHandlingOptions() {
		return getResources().getStringArray(R.array.default_image_handling);
	}
	
	@SuppressWarnings("unused")
	private String[] getDBPWCacheValues() {
		return getResources().getStringArray(R.array.password_cache);
	}
	
	private class WizardForm extends JSONObject {
		Context _c;
		JSONArray frames, order;
		JSONObject currentFrame;
		ArrayList<Callback> callbacks;
		
		public final static String frameKey = "frameKey";
		public final static String frameTitle = "frameTitle";
		public final static String frameContent = "frameContent";
		public final static String frameOrder = "frameOrder";
		public final static String allFrames = "frames";
		
		public WizardForm(Context c) {
			_c = c;
			frames = new JSONArray();
			order = new JSONArray();
			callbacks = new ArrayList<Callback>();
			
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
			for(int f=0; f<frames.length(); f++) {
				JSONObject frame = frames.getJSONObject(f);
				if(frame.getString(frameKey).compareTo(order.getString(which)) == 0)
					currentFrame = frame;
			}
		}
		
		public ArrayList<Callback> getCallbacks() {
			return callbacks;
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
		
		private String[] parseArguments(String args) {
			String[] a = null;
			if(args != null) {
				a = args.split(",");
			}
			return a;
		}
		
		public ArrayList<View> getContent() throws JSONException {
			ArrayList<View> views = new ArrayList<View>();
			String content = currentFrame.getString(frameContent);
			
			for(final String s : content.split("\n")) {
				if(s.contains("{$")) {
					final String type = findKey(s, "type");
					final String callback = findKey(s, "callback");
					final boolean isMandatory = Boolean.parseBoolean(findKey(s, "mandatory"));
					final String attachTo = findKey(s, "attachTo");
					
					if(isMandatory)
						Wizard.this.setMandatory(wizard_next);
					
					if(type.compareTo("button") == 0) {
						Button button = new Button(_c);
						button.setText(findKey(s, "text"));
						
						String[] args = parseArguments(findKey(s, "args"));
						final Callback buttonCall = new Callback(callback, args); 
						
						button.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								try {
									buttonCall.doCallback();
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
						
						edittext.addOnLayoutChangeListener(new TextView.OnLayoutChangeListener() {

							@Override
							public void onLayoutChange(View v, int left,
									int top, int right, int bottom,
									int oldLeft, int oldTop, int oldRight,
									int oldBottom) {
								// TODO Auto-generated method stub
								
							}
							
						});
						views.add(edittext);
					} else if(type.compareTo("password") == 0) {
						EditText edittext = new EditText(_c);
						edittext.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
						edittext.setTransformationMethod(new PasswordTransformationMethod());
						
						edittext.addOnLayoutChangeListener(new TextView.OnLayoutChangeListener() {

							@Override
							public void onLayoutChange(View v, int left,
									int top, int right, int bottom,
									int oldLeft, int oldTop, int oldRight,
									int oldBottom) {
								
								String pw = ((EditText) v).getText().toString();
								if(pw.length() == 0) {}
								else if(pw.length() < 6 && pw.length() > 0)
									Wizard.this.makeToast(_c.getResources().getString(R.string.wizard_error_password_too_short));
								else {
									enableAction(wizard_next);
									if(callback != null) {
										if(attachTo == null)
											callbacks.add(new Callback(callback, new String[] {pw}));
										
									}
								}
								
							}
							
						});
						
						views.add(edittext);
					} else if(type.compareTo("select_one") == 0 || type.compareTo("select_multi") == 0) {
						
						ArrayList<Selections> selections = new ArrayList<Selections>();
						ListView lv = new ListView(_c);
						
						for(String option : findKey(s, "values").split(",")) {
							if(Character.toString(option.charAt(0)).compareTo("#") == 0) {
								// populate from callback
								Callback populate = new Callback(option.substring(1), null);
								
								try {
									for(String res : (String[]) populate.doCallback())
										selections.add(new Selections(res, false));
									
								} catch (IllegalAccessException e) {
									Log.d(InformaConstants.TAG, e.toString());
								} catch (NoSuchMethodException e) {
									Log.d(InformaConstants.TAG, e.toString());
								} catch (InvocationTargetException e) {
									Log.d(InformaConstants.TAG, e.toString());
								}
							} else 
								selections.add(new Selections(option, false));
						}
						
						callbacks.add(new Callback(callback, new Object[] {selections}));
						
						lv.setAdapter(new SelectionsAdapter(_c, selections, type));
						views.add(lv);
					}
				} else {
					TextView tv = new TextView(_c);
					tv.setText(s);
					views.add(tv);
				}
			}
			
			return views;
		}
		
		public String getTitle() throws JSONException {
			return currentFrame.getString(frameTitle);
		}
	}
	
	public class Callback {
		String _func;
		Object[] _args;
		
		public Callback(String func, Object[] args) {
			_func = func;
			_args = args;
		}
		
		public Object doCallback() throws  IllegalAccessException, NoSuchMethodException, InvocationTargetException {
			Method method;
			if(_args != null) {
				Class<?>[] paramTypes = new Class[_args.length];
				
				for(int p=0; p<paramTypes.length; p++)
					paramTypes[p] = _args[p].getClass();
				
				method = Wizard.this.getClass().getDeclaredMethod(_func, paramTypes);
			} else
				method = Wizard.this.getClass().getDeclaredMethod(_func, null);
			
			method.setAccessible(true);
			return method.invoke(Wizard.this, _args);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
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
				// do the callbacks...
				for(Callback c: wizardForm.getCallbacks()) {
					try {
						c.doCallback();
					} catch (IllegalAccessException e) {
						Log.d(InformaConstants.TAG, e.toString());
					} catch (NoSuchMethodException e) {
						Log.d(InformaConstants.TAG, e.toString());
					} catch (InvocationTargetException e) {
						Log.d(InformaConstants.TAG, e.toString());
					}
				}
				
				Intent i = new Intent(this,Wizard.class);
				i.putExtra("current", current + 1);
				startActivity(i);
				finish();
			}
		} else if(v == wizard_done) {
			_ed.putBoolean(InformaConstants.Settings.SETTINGS_VIEWED, true).commit();
			Intent i = new Intent(this, ObscuraApp.class);
			startActivity(i);
			finish();
		}
		
	}
	
	@Override
	protected void onActivityResult(int request, int result, Intent data) {
		super.onActivityResult(request, result, data);
		
		if(result == Activity.RESULT_OK) {
			apg.onActivityResult(this, request, result, data);
			
			switch(request) {
			case Apg.SELECT_SECRET_KEY:
				setUserPGP();
				break;
			}
		}
			
	}
	
	@Override
	public void onBackPressed() {}

}
