package org.witness.informa;

import info.guardianproject.database.sqlcipher.SQLiteDatabase;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.witness.informa.utils.InformaDestKeysList;
import org.witness.informa.utils.InformaDestKeysList.DestKeyManager;
import org.witness.informa.utils.secure.Apg;
import org.witness.securesmartcam.io.ObscuraDatabaseHelper;
import org.witness.securesmartcam.io.ObscuraDatabaseHelper.TABLES;
import org.witness.sscphase1.ObscuraApp;
import org.witness.sscphase1.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class InformaPreferences extends Activity implements OnClickListener {
	SharedPreferences _sp;
	SharedPreferences.Editor _ed;
	
	JSONArray keyObj;
	ArrayList<DestKeyManager> keys;
	ListView keyList;
	
	Button ok, informaPref_destKeys_add;
	EditText pwd;
	boolean canSave;
	int hasEntries;
	AlertDialog ad;
	
	Apg apg;
	ObscuraDatabaseHelper odh;
	SQLiteDatabase db;
	
	public static final String LOG = "[InformaPrefs **********************]";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.informapreferences);
		
		canSave = false;
		hasEntries = 0;
		
		SQLiteDatabase.loadLibs(this);
		
		ok = (Button) findViewById(R.id.informaPref_OK);
		ok.setOnClickListener(this);
		
		informaPref_destKeys_add = (Button) findViewById(R.id.informaPref_destKeys_ADD);
		informaPref_destKeys_add.setOnClickListener(this);
		
		pwd = (EditText) findViewById(R.id.informaPref_dbpw);
		keys = new ArrayList<DestKeyManager>();
		
		_sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		_ed = _sp.edit();
		
		apg = Apg.getInstance();
		if(!apg.isAvailable(this)) {
			// TODO: prompt to install apg from market, etc. and close app.
			Log.d(ObscuraApp.TAG, "we do not have apg.  must install it.");
		} else {
			odh = new ObscuraDatabaseHelper(this);
			
			if(_sp.getString("informaPref_dbpw", "").compareTo("") != 0) {
				canSave = true;
				pwd.setText(_sp.getString("informaPref_dbpw", ""));
				db = odh.getWritableDatabase(_sp.getString("informaPref_dbpw", ""));
			}
			
			if(_sp.getString("informaPref_destKeys", "").compareTo("") == 0) {
				keyObj = new JSONArray();
			} else {
				if(canSave) {
					odh.setTable(db, TABLES.INFORMA_PREFERENCES);
					Cursor c = db.query(odh.getTable(), null, null, null, null, null, null);
					if(c != null && c.getCount() > 0) {
						c.moveToFirst();
						_ed.putInt("informaPref_defaultSecurityLevel", c.getInt(2)).commit();
						try {
							JSONObject jo = new JSONObject(c.getString(1));
							keyObj = jo.getJSONArray("destKeys");
														
							for(int x = 0;x< keyObj.length(); x++) {
								JSONObject k = keyObj.getJSONObject(x);
								keys.add(new DestKeyManager(k.getString("alias"), k.getString("email"), Long.parseLong(k.getString("key"))));
							}
							hasEntries = c.getCount();
						} catch (JSONException e) {}
					}
					c.close();
				} else {
					keyObj = new JSONArray();
				}
			}
			
			keyList = (ListView) findViewById(R.id.informaPref_destKeys);
			keyList.setAdapter(new InformaDestKeysList(this, keys));
		}
	}
	
	public void savePreferencesAndExit() throws JSONException {
		JSONObject destKeys = new JSONObject();
		JSONArray keyArray = new JSONArray();
		
		for(JSONObject key : keys)
			keyArray.put(key);
		
		destKeys.put("destKeys", keyArray);
		
		// LOG TO DATABASE
		if(!canSave) {
			if(pwd.getText().toString().length() < 1) {
				Toast.makeText(
					this,
					getString(R.string.informaPref_dbpw_notfound), 
					Toast.LENGTH_LONG)
					.show();
				return;
			} else {
				canSave = true;
				_ed.putString("informaPref_dbpw", pwd.getText().toString()).commit();
				db = odh.getWritableDatabase(_sp.getString("informaPref_dbpw", ""));
			}
			
		}
		
		odh.setTable(db, TABLES.INFORMA_PREFERENCES);
		
		ContentValues cv = new ContentValues();
		cv.put("destinationKeys", destKeys.toString());
		cv.put("defaultSecurityLevel", _sp.getInt("informaPref_defaultSecurityLevel", ObscuraApp.SECURITY_LEVELS.UnencryptedSharable));
		
		if(hasEntries == 0)
			db.insert(odh.getTable(), null, cv);
		else
			db.update(odh.getTable(), cv, BaseColumns._ID + "=?", new String[] { "1" });
		
		_ed.putString("informaPref_destKeys", "saved " + System.currentTimeMillis()).commit();

		this.finish();
	}
	
	public void chooseNewDestinationKey() {
		LayoutInflater li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View pView = li.inflate(R.layout.informa_keymanager, null);
		
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setView(pView);
		
		final EditText destName = (EditText) pView.findViewById(R.id.informaPrefs_keyalias);
		final EditText destEmail = (EditText) pView.findViewById(R.id.informaPrefs_keyemail);
		
		Button save = (Button) pView.findViewById(R.id.informaPrefs_savekey);
		save.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ad.dismiss();
				ad = null;
				
				long[] foundKeys = apg.getSecretKeyIdsFromEmail(getApplicationContext(), destEmail.getText().toString());
				if(foundKeys.length > 0) {
					StringBuffer keyId = new StringBuffer();
					for(long k : foundKeys)
						keyId.append(k);
					
					try {
						DestKeyManager destKey = new DestKeyManager(
								destName.getText().toString(),
								destEmail.getText().toString(),
								foundKeys[0]
								);
						keys.add(destKey);
						keyList.setAdapter(new InformaDestKeysList(getApplicationContext(), keys));
						
					} catch (JSONException e) {}
					
				} else {
					Toast.makeText(
							getApplicationContext(),
							getApplicationContext().getString(R.string.informaPref_destKeys_notfound), 
							Toast.LENGTH_LONG)
							.show();
				}
			}
		});
		
		ad = b.create();
		ad.show();
	}
	
	
	@Override
	protected void onPause() {		
		db.close();
		odh.close();
		super.onPause();
	}

	@Override
	public void onClick(View v) {
		if(v == ok) {
			try {
				savePreferencesAndExit();
			} catch (JSONException e) {}
		} else if(v == informaPref_destKeys_add) {
			chooseNewDestinationKey();
		}
		
	}
}