package org.witness.sscphase1;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;

public class PreferencesActivity extends PreferenceActivity implements OnPreferenceClickListener {
	
	Preference doneButtonPref;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            
            /*
            // For the Done Button
            doneButtonPref = (Preference) findPreference("DoneButtonPref");
            doneButtonPref.setOnPreferenceClickListener(this);
           	*/
    }

	public boolean onPreferenceClick(Preference pref) {
		/*
		if (pref == doneButtonPref) {
			finish();
			return true;			
		}
		*/

		return false;
	}
}
