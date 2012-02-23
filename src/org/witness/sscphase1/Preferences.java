package org.witness.sscphase1;

import java.util.ArrayList;

import info.guardianproject.database.sqlcipher.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;
import org.witness.informa.utils.InformaConstants;
import org.witness.informa.utils.InformaConstants.Keys;
import org.witness.informa.utils.InformaConstants.Keys.Tables;
import org.witness.informa.utils.io.DatabaseHelper;
import org.witness.informa.utils.secure.Apg;
import org.witness.securesmartcam.utils.Selections;
import org.witness.sscphase1.utils.EditorsAdapter;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

public class Preferences extends PreferenceActivity implements OnClickListener {
	PreferenceCategory pc;
	SharedPreferences _sp;
	DatabaseHelper dh;
	SQLiteDatabase db;
	Apg apg;
	ArrayList<Selections> trustedDestinations;
	ArrayList<Long> cleanUpDeadContacts;
	
	Button addNew;
	ListView tdmanager_holder;
	EditorsAdapter editorsAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		setContentView(R.layout.preferences);
		
		SQLiteDatabase.loadLibs(this);
		
		_sp = PreferenceManager.getDefaultSharedPreferences(this);
		dh = new DatabaseHelper(this);
		db = dh.getWritableDatabase(_sp.getString(Keys.Settings.HAS_DB_PASSWORD, ""));
		apg = Apg.getInstance();
		
		trustedDestinations = new ArrayList<Selections>();
		editorsAdapter = new EditorsAdapter(this, trustedDestinations);
		
		cleanUpDeadContacts = new ArrayList<Long>();
		
		addNew = (Button) findViewById(R.id.tdmanager_addNew);
		addNew.setOnClickListener(this);
		
		tdmanager_holder = (ListView) findViewById(R.id.tdmanager_holder);
		tdmanager_holder.setAdapter(editorsAdapter);
		
		dh.setTable(db, Tables.TRUSTED_DESTINATIONS);
		try {
			Cursor c = dh.getValue(db, new String[] {
					BaseColumns._ID,
					Keys.TrustedDestinations.DISPLAY_NAME, 
					Keys.TrustedDestinations.EMAIL,
					Keys.TrustedDestinations.KEYRING_ID}, null, null);
			c.moveToFirst();
			while(!c.isAfterLast()) {
				try {
					String keyId = c.getString(c.getColumnIndex(Keys.TrustedDestinations.KEYRING_ID));
					
					if(apg.getPublicUserId(getApplicationContext(), Long.parseLong(keyId)).compareTo("<unknown>") != 0) {
						JSONObject tdExtras = new JSONObject();
						tdExtras.put(Keys.TrustedDestinations.DISPLAY_NAME, c.getString(c.getColumnIndex(Keys.TrustedDestinations.DISPLAY_NAME)));
						tdExtras.put(Keys.TrustedDestinations.EMAIL, c.getString(c.getColumnIndex(Keys.TrustedDestinations.EMAIL)));
						tdExtras.put(Keys.TrustedDestinations.KEYRING_ID, c.getString(c.getColumnIndex(Keys.TrustedDestinations.KEYRING_ID)));
						trustedDestinations.add(new Selections(null, false, tdExtras));
					} else {
						cleanUpDeadContacts.add(c.getLong(c.getColumnIndex(BaseColumns._ID)));
					}
				} catch(JSONException e) {}
				c.moveToNext();
			}
			c.close();
			
			for(long l : cleanUpDeadContacts)
				removeContact(l, null);
		
		} catch(NullPointerException e) {}
	}
	
	private void refreshList() {
		tdmanager_holder.destroyDrawingCache();
		tdmanager_holder.setVisibility(ListView.INVISIBLE);
		tdmanager_holder.setAdapter(editorsAdapter);
		tdmanager_holder.setVisibility(ListView.VISIBLE);
	}
	
	public void removeContact(long dbId, Selections removeFromViewableList) {			
		if(dbId == -1) {
			try {
				Cursor c = dh.getValue(db, 
						new String[] {BaseColumns._ID}, 
						Keys.TrustedDestinations.KEYRING_ID, 
						removeFromViewableList.getExtras().getString(Keys.TrustedDestinations.KEYRING_ID));
				c.moveToFirst();
				dbId = c.getLong(c.getColumnIndex(BaseColumns._ID));
				c.close();
			} catch(NullPointerException e) {}
			catch (JSONException e) {}
		}
		
		dh.removeValue(db, new String[] {BaseColumns._ID}, new Object[] {dbId});
		
		if(removeFromViewableList != null) {
			trustedDestinations.remove(trustedDestinations.indexOf(removeFromViewableList));
			refreshList();
		}
			
	}
	
	public void setTrustedDestinations() {
		for(long keyId : apg.getEncryptionKeys()) {
			String userId = apg.getPublicUserId(this, keyId);
			String email_ = userId.substring(userId.indexOf("<") + 1);
			String email = email_.substring(0, email_.indexOf(">"));
			String displayName = userId.substring(0, userId.indexOf("<"));
			
			if(userId.indexOf("(") != -1)
				displayName = userId.substring(0, userId.indexOf("("));
			
			ContentValues cv = new ContentValues();
			cv.put(Keys.TrustedDestinations.KEYRING_ID, keyId);
			cv.put(Keys.TrustedDestinations.EMAIL, email);
			cv.put(Keys.TrustedDestinations.DISPLAY_NAME, displayName);
			
			db.insert(dh.getTable(), null, cv);
			
			
			try {
				JSONObject tdExtras = new JSONObject();
				tdExtras.put(Keys.TrustedDestinations.KEYRING_ID, Long.toString(keyId));
				tdExtras.put(Keys.TrustedDestinations.EMAIL, email);
				tdExtras.put(Keys.TrustedDestinations.DISPLAY_NAME, displayName);
				trustedDestinations.add(new Selections(null, false, tdExtras));
				
			} catch(JSONException e) {}
		}
		refreshList();
		apg.setEncryptionKeys(new long[] {0L});
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		dh.close();
		if(db != null)
			db.close();
	}
	
	@Override
	public void onClick(View v) {
		if(v == addNew)
			apg.selectEncryptionKeys(this, null);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(resultCode == Activity.RESULT_OK) {
			if(requestCode == Apg.SELECT_PUBLIC_KEYS) {
				apg.onActivityResult(this, requestCode, resultCode, data);
				setTrustedDestinations();
				
				
			}
		}
	}

}
