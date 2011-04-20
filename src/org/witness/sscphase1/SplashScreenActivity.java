/**
 * This Activity will be the initial Activity for the application.
 * It (will) loads the preferences to see if it should be displayed.
 * If not, it will load the main Activity (CameraObscura).
 * 
 * This Activity (will) contain a Splash screen then an About screen
 * and finally the preferences screen
 */
package org.witness.sscphase1;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SplashScreenActivity extends Activity implements OnClickListener, OnSeekBarChangeListener {

	// See wireframes:  Logo in middle and link to Guardian page
	
	CameraObscuraPreferences prefs;
	
	View splashScreenView;
	View walkThroughView;
	
	View splashScreenTextView;
	//ImageView splashScreenImageView;
	View creditsTextView;
	
	Button walkThroughSkipButton;
	Button walkThroughContinueButton;
	SeekBar walkThroughPrefSlider;
	TextView walkThroughPrefSliderOutput;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.splashscreen);
        // Use LayoutInflator for Views
        LayoutInflater inflater = LayoutInflater.from(this);        
        splashScreenView = inflater.inflate(R.layout.splashscreen, null);
        setContentView(splashScreenView);
             
        splashScreenTextView = findViewById(R.id.SplashTextView);
        splashScreenTextView.setOnClickListener(this);
       
        
        creditsTextView = findViewById(R.id.CreditsTextView);
        creditsTextView.setOnClickListener(this);
        
    	walkThroughView = inflater.inflate(R.layout.walkthrough, null);
    	//walkThroughSkipButton = (Button) walkThroughView.findViewById(R.id.WalkThroughSkipButton);
    	//walkThroughSkipButton.setOnClickListener(this);
    	
    	walkThroughContinueButton = (Button) walkThroughView.findViewById(R.id.WalkThroughContinueButton);
    	walkThroughContinueButton.setOnClickListener(this);
    	
    	// Slider for Risk Level/Default Preferences
		walkThroughPrefSlider = (SeekBar) walkThroughView.findViewById(R.id.WalkThroughPrefSlider);
		walkThroughPrefSlider.setMax(CameraObscuraPreferences.RISK_2);
		walkThroughPrefSlider.setProgress(CameraObscuraPreferences.RISK_1);
		walkThroughPrefSlider.setOnSeekBarChangeListener(this);
		
		walkThroughPrefSliderOutput = (TextView) walkThroughView.findViewById(R.id.WalkThroughPrefSliderOutput);
		
    	
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        prefs = new CameraObscuraPreferences(this);
        //prefs.setDefaults();
		walkThroughPrefSliderOutput.setText(prefs.getRiskLevelLabel(CameraObscuraPreferences.RISK_1));
    }    

	public void onClick(View view) {
		if (view == splashScreenTextView) {
			// Display Walk Through
			if (prefs.getWalkThroughPref()) {
				setContentView(walkThroughView);
			} else {
				Intent intent = new Intent(this, CameraObscuraMainMenu.class);
				startActivity(intent);				
			}
		} /* else if (view == walkThroughSkipButton) {
			// Load Main View Through an Intent
			Intent intent = new Intent(this, CameraObscura.class);
    		startActivity(intent);
    		finish();
		} */ else if (view == walkThroughContinueButton) {
			Intent intent = new Intent(this, CameraObscuraMainMenu.class);
			startActivity(intent);
		} else if (view == creditsTextView) {
			String url = "https://guardianproject.info/apps/securecam/";
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(url));
			startActivity(i);			
		}
	}

	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		walkThroughPrefSliderOutput.setText(prefs.getRiskLevelLabel(progress));
		prefs.setRiskLevel(progress);
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
		
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		
	}	
}
