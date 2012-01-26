package org.witness.sscphase1;

import org.witness.informa.utils.InformaConstants;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

class InformaSettings {
	public static final String INFORMA = "informa";
	public static final String SETTINGS_VIEWED = "informa.SettingsViewed";
	public static final String HAS_DB_PASSWORD = "informa.PasswordSet";
	public static final String DB_PASSWORD_CACHE_TIMEOUT = "informa.PasswordCacheTimeout";
	public static final String HAS_TRUSTED_ENDPOINTS = "informa.HasTrustedEndpoints";
	
	
	private static final String TAG = "*************** INFORMA PREFERENCES ***********\n";
	
	static interface OnSettingsSeen {
		void onSettingsSeen();
	}
	
	static boolean show(final Activity activity) {
		SharedPreferences preferences = activity.getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor _ed = preferences.edit();
		
		if(!preferences.getBoolean(SETTINGS_VIEWED, false)) {
			Log.d(TAG, "virgin user, EULA accepted. launching wizard");
			if(activity instanceof OnSettingsSeen) {
				Intent intent = new Intent(activity, Wizard.class);
				activity.startActivityForResult(intent, InformaConstants.FROM_INFORMA_WIZARD);
			}
			
			return false;
		} else if(!preferences.getBoolean(HAS_DB_PASSWORD, false)) {
			Log.d(TAG, "user\'s password expired.  must log in again.");
			final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setCancelable(false);
			builder.setTitle(R.string.ip_login_title);
			
			
			builder.create().show();
			return false;
		}
		
		return true;
	}
	
	

}
