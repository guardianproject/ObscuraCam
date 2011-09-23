package org.witness.securesmartcam;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class CameraObscuraPreferences {
	
	SharedPreferences sharedPreferences;
	SharedPreferences.Editor editor;

	// These need to match the values in arrays.xml
	public static final int ORIGINALIMAGE_PREF_DELETE = 0;
	public static final int ORIGINALIMAGE_PREF_LOCK = 1;
	public static final int ORIGINALIMAGE_PREF_LEAVE = 2;
	public static final int ORIGINALIMAGE_PREF_DEFAULT = ORIGINALIMAGE_PREF_LOCK;

	// These need to match the values in arrays.xml
	public static final int PANIC_BUTTON_PREF_WIPE = 0;
	public static final int PANIC_BUTTON_PREF_LOCK = 1;
	public static final int PANIC_BUTTON_PREF_DEFAULT = PANIC_BUTTON_PREF_LOCK;
	
	public static final boolean SPLASH_SCREEN_PREF_DEFAULT = true;
	public static final boolean AUTO_SIGN_PREF_DEFAULT = true;
	public static final boolean AUTO_SUBMIT_PREF_DEFAULT = false;
	
	public static final int RISK_0 = 0; // No Risk
	public static final int RISK_1 = 1; // Medium Level Risk
	public static final int RISK_2 = 2; // Extreme Risk
	
	public static final int DEFAULT_RISK_LEVEL = RISK_1;
	
	public CameraObscuraPreferences(Context context) {
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		editor = sharedPreferences.edit();
		
		//setDefaults();
		
		// Set Defaults if not set
		// This is a bit hacky
		/*
		setSplashScreenPref(getSplashScreenPref());
		setAutoSignPref(getAutoSignPref());
		setAutoSubmitPref(getAutoSignPref());
		setOriginalImagePref(getOriginalImagePref());
		setPanicButtonPref(getPanicButtonPref());
		*/
	}
	
	public String getRiskLevelLabel(int riskLevel) {
		switch (riskLevel) {
			case CameraObscuraPreferences.RISK_0:
				// No Risk
				return "No Risk";
			case CameraObscuraPreferences.RISK_1:
				// Medium Level Risk
				return "Medium Risk";
			case CameraObscuraPreferences.RISK_2:
				// Extreme Risk
				return "High Risk";
			default:
				return "No Risk";
		}				
	}
	
	public void setDefaults() {
		editor.clear();
		setRiskLevel(RISK_1);
	}

	public int getRiskLevel() {
		return sharedPreferences.getInt("RiskLevel", DEFAULT_RISK_LEVEL);		
	}
	
	public void setRiskLevel(int riskLevel) {
		
		editor.putInt("RiskLevel", riskLevel);
		editor.commit();
		
		switch (riskLevel) {
			case CameraObscuraPreferences.RISK_0:
				// No Risk
				setAutoSignPref(false);
				setAutoSubmitPref(false);
				setOriginalImagePref(ORIGINALIMAGE_PREF_LEAVE);
				setPanicButtonPref(PANIC_BUTTON_PREF_LOCK);
				break;
			case CameraObscuraPreferences.RISK_1:
				// Medium Level Risk
				setAutoSignPref(true);
				setAutoSubmitPref(false);
				setOriginalImagePref(ORIGINALIMAGE_PREF_LOCK);
				setPanicButtonPref(PANIC_BUTTON_PREF_LOCK);
				break;
			case CameraObscuraPreferences.RISK_2:
				// Extreme Risk
				setAutoSignPref(false);
				setAutoSubmitPref(true);
				setOriginalImagePref(ORIGINALIMAGE_PREF_DELETE);
				setPanicButtonPref(PANIC_BUTTON_PREF_WIPE);			
				break;
		}		
	}
	
	public boolean getSplashScreenPref() {
		return sharedPreferences.getBoolean("SplashScreenPref", SPLASH_SCREEN_PREF_DEFAULT);
	}

	public void setSplashScreenPref(boolean splashScreenPref) {
		editor.putBoolean("SplashScreenPref", splashScreenPref);
		editor.commit();
	}
	
	public boolean getWalkThroughPref() {
		return sharedPreferences.getBoolean("WalkThroughPref", true);
	}
	
	public void setWalkThroughPref(boolean walkThroughPref) {
		editor.putBoolean("WalkThroughPref", walkThroughPref);
		editor.commit();
	}

	public boolean getAutoSignPref() {
		return sharedPreferences.getBoolean("AutoSignPref", AUTO_SIGN_PREF_DEFAULT);
	}

	public void setAutoSignPref(boolean autoSignPref) {
		editor.putBoolean("AutoSignPref", autoSignPref);
		editor.commit();
	}

	public boolean getAutoSubmitPref() {
		return sharedPreferences.getBoolean("AutoSubmitPref", AUTO_SUBMIT_PREF_DEFAULT);
	}

	public void setAutoSubmitPref(boolean autoSubmitPref) {
		editor.putBoolean("AutoSubmitPref", autoSubmitPref);
		editor.commit();
	}

	public int getOriginalImagePref() {
		return Integer.valueOf(sharedPreferences.getString("OriginalImagePref", "" + ORIGINALIMAGE_PREF_DEFAULT));
	}

	public void setOriginalImagePref(int originalImagePref) {
		editor.putString("OriginalImagePref", ""+originalImagePref);
		editor.commit();
	}

	public int getPanicButtonPref() {
		return Integer.valueOf(sharedPreferences.getString("PanicButtonPref","" + PANIC_BUTTON_PREF_DEFAULT));
	}

	public void setPanicButtonPref(int panicButtonPref) {
		editor.putString("PanicButtonPref", ""+panicButtonPref);
		editor.commit();
	}	
}
