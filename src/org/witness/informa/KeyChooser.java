package org.witness.informa;

import info.guardianproject.database.sqlcipher.SQLiteDatabase;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.witness.informa.utils.InformaConstants;
import org.witness.informa.utils.InformaConstants.Keys;
import org.witness.informa.utils.InformaConstants.Keys.Settings;
import org.witness.informa.utils.InformaConstants.Keys.Tables;
import org.witness.informa.utils.InformaConstants.Keys.TrustedDestinations;
import org.witness.informa.utils.io.DatabaseHelper;
import org.witness.informa.utils.secure.Apg;
import org.witness.securesmartcam.utils.ObscuraConstants;
import org.witness.securesmartcam.utils.Selections;
import org.witness.securesmartcam.utils.SelectionsAdapter;
import org.witness.sscphase1.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
					Cursor c = dh.getValue(db, new String[] {
							Keys.TrustedDestinations.EMAIL,
							Keys.TrustedDestinations.DISPLAY_NAME,
							Keys.TrustedDestinations.KEYRING_ID
					}, null, null);
					c.moveToFirst();
					
					while(!c.isAfterLast()) {
						JSONObject extras = new JSONObject();
						extras.put(TrustedDestinations.EMAIL, c.getString(c.getColumnIndex(Keys.TrustedDestinations.EMAIL)));
						extras.put(TrustedDestinations.KEYRING_ID, c.getString(c.getColumnIndex(Keys.TrustedDestinations.KEYRING_ID)));
						extras.put(TrustedDestinations.DISPLAY_NAME, c.getString(c.getColumnIndex(TrustedDestinations.DISPLAY_NAME)));
						
						// TODO: rehandle missing keys
						keys.add(new Selections(extras.getString(TrustedDestinations.DISPLAY_NAME), false, extras));
							
						c.moveToNext();
					}
					
					c.close();
					db.close();
					dh.close();
					
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
	
	private void checkForEmptyEncryptList() {
		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setIcon(android.R.drawable.ic_dialog_alert);
		b.setTitle(R.string.chooser_no_choice_warning_title);
		b.setMessage(R.string.chooser_no_choice_warning_text);
		b.setPositiveButton(R.string._continue, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				getIntent().putExtra(Keys.USER_CANCELED_EVENT, Keys.USER_CANCELED_EVENT);
				setResult(Activity.RESULT_CANCELED, getIntent());
				finish();
				
			}
		});
		b.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
		b.show();
	}
	
	@Override
	public void onClick(View v) {
		if(v == keyChooser_ok) {
			ArrayList<Long> sel = new ArrayList<Long>();
			for(Selections s : keys) {
				if(s.getSelected())
					try {
						sel.add(Long.parseLong(s.getExtras().getString(TrustedDestinations.KEYRING_ID)));
					} catch (JSONException e) {
						Log.d(InformaConstants.TAG, e.toString());
					}
			}
			if(sel.size() > 0) {
				long[] selected = new long[sel.size()];
				for(int l=0; l<sel.size(); l++)
					selected[l] = sel.get(l);
					
				getIntent().putExtra(InformaConstants.Keys.Intent.ENCRYPT_LIST, selected);
				setResult(Activity.RESULT_OK, getIntent());
				finish();
			} else
				checkForEmptyEncryptList();
		}
	}
}
