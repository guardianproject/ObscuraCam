package org.witness.informa;

import info.guardianproject.database.sqlcipher.SQLiteDatabase;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.witness.informa.utils.InformaConstants;
import org.witness.informa.utils.InformaConstants.Keys.Settings;
import org.witness.informa.utils.InformaConstants.Keys.Tables;
import org.witness.informa.utils.InformaConstants.Keys.TrustedDestinations;
import org.witness.informa.utils.io.DatabaseHelper;
import org.witness.informa.utils.secure.Apg;
import org.witness.securesmartcam.utils.Selections;
import org.witness.securesmartcam.utils.SelectionsAdapter;
import org.witness.sscphase1.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

public class KeyChooser extends Activity implements OnClickListener {
	ListView keyChooser;
	Button keyChooser_ok;
	ArrayList<Selections> keys;
	Handler h;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.informakeychooser);
		
		keyChooser = (ListView) findViewById(R.id.keyChooser);
		keyChooser_ok = (Button) findViewById(R.id.keyChooser_ok);
		keyChooser_ok.setOnClickListener(this);
		keys = new ArrayList<Selections>();
		
		
		h = new Handler();
		
		Runnable r = new Runnable() {
			Apg apg;
			DatabaseHelper dh;
			SQLiteDatabase db;
			SharedPreferences preferences;
			
			@Override
			public void run() {
				preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				
				SQLiteDatabase.loadLibs(getApplicationContext());
				dh = new DatabaseHelper(getApplicationContext());
				db = dh.getReadableDatabase(preferences.getString(Settings.HAS_DB_PASSWORD, ""));
				
				apg = Apg.getInstance();
				dh.setTable(db, Tables.TRUSTED_DESTINATIONS);
				
				try {
					Cursor c = dh.getValue(db, null, null, null);
					c.moveToFirst();
					
					while(!c.isAfterLast()) {
						JSONObject extras = new JSONObject();
						extras.put(TrustedDestinations.EMAIL, c.getString(1));
						extras.put(TrustedDestinations.KEYRING_ID, c.getLong(2));
						
						keys.add(new Selections(c.getString(3), false, extras));
						c.moveToNext();
					}
					
					c.close();
					
				} catch(NullPointerException e) {
					Log.d(InformaConstants.TAG, "cursor was nulllll");
				} catch (JSONException e) {
					Log.d(InformaConstants.TAG, e.toString());
				}
				
				
				h.post(new Runnable() {
					@Override
					public void run() {
						updateList();
					}
				});
			}
		};
		new Thread(r).start();
		
	}
	
	public void updateList() {
		keyChooser.setAdapter(new SelectionsAdapter(this, keys, InformaConstants.Selections.SELECT_MULTI));
	}
	
	@Override
	public void onClick(View v) {
		if(v == keyChooser_ok) {
			ArrayList<Long> selected = new ArrayList<Long>();
			for(Selections s : keys) {
				if(s.getSelected())
					try {
						selected.add(s.getExtras().getLong(TrustedDestinations.KEYRING_ID));
					} catch (JSONException e) {
						Log.d(InformaConstants.TAG, e.toString());
					}
			}
			if(selected.size() > 0) {
				getIntent().putExtra(InformaConstants.Keys.Intent.ENCRYPT_LIST, ((Long[]) selected.toArray()));
				setResult(Activity.RESULT_OK, getIntent());
				finish();
			}
		}
	}
}
