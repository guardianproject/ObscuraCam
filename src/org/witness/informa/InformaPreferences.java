package org.witness.informa;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.witness.informa.utils.InformaDestKeysList;
import org.witness.informa.utils.InformaDestKeysList.DestKeyManager;
import org.witness.informa.utils.secure.Apg;
import org.witness.securesmartcam.io.ObscuraDatabaseHelper;
import org.witness.sscphase1.ObscuraApp;
import org.witness.sscphase1.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
	boolean canSave = false;
	AlertDialog ad;
	
	Apg apg;
	ObscuraDatabaseHelper odh;
	
	public static final String LOG = "[InformaPrefs **********************]";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.informapreferences);
		
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
			
			if(_sp.getString("informaPref_dbpw", "").compareTo("") == 0) {
				// we do not have a password.  we cannot do a thing yet.
				
			} else {
				canSave = true;
				odh.getWritableDatabase(_sp.getString("informaPref_dbpw", ""));
			}
			
			if(_sp.getString("informaPref_destKeys", "").compareTo("") == 0) {
				keyObj = new JSONArray();
				// user has no destination keys... launch apg to select some
			} else {
				if(canSave) {
				// load keys from database
				} else {
					// we have never seen the database before
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
		// TODO: LOG TO DATABASE
		Log.d(ObscuraApp.TAG, destKeys.toString());
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(
			requestCode == Apg.SELECT_PUBLIC_KEYS
		) {
			Log.d(ObscuraApp.TAG, "got public key!\n" + data.toString());
		}
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
