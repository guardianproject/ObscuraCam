package org.witness.sscphase1;

import org.witness.informa.utils.InformaConstants;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

class InformaSettings {

	
	static interface OnSettingsSeen {
		void onSettingsSeen();
	}
	
	static boolean show(final Activity activity) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		
		if(!preferences.getBoolean(InformaConstants.Keys.Settings.SETTINGS_VIEWED, false)) {
			Log.d(InformaConstants.TAG, "virgin user, EULA accepted. launching wizard");
			if(activity instanceof OnSettingsSeen) {
				Intent intent = new Intent(activity, Wizard.class);
				activity.startActivity(intent);
			}
			
			return false;
		} else if(preferences.getString(InformaConstants.Keys.Settings.HAS_DB_PASSWORD, "").compareTo(InformaConstants.PW_EXPIRY) == 0) {
			Log.d(InformaConstants.TAG, "user\'s password expired.  must log in again.");
			Intent intent = new Intent(activity, Login.class);
			activity.startActivity(intent);
			return false;
		}
		
		return true;
	}
	
	

}
