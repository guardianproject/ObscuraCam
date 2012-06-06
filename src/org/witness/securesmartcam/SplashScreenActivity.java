/**
 * This Activity will be the initial Activity for the application.
 * It will load the main Activity (CameraObscuraMainMenu) on click
 * 
 * NOT BEING USED
 */
package org.witness.securesmartcam;

import org.witness.sscphase1.ObscuraApp;
import org.witness.sscphase1.R;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class SplashScreenActivity extends Activity implements OnClickListener {
	
	View splashScreenView;
	
	TextView splashScreenTextView;
	ImageView splashScreenImageView;
	
	View creditsTextView;
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Use LayoutInflator for Views
        LayoutInflater inflater = LayoutInflater.from(this);        
        splashScreenView = inflater.inflate(R.layout.splashscreen, null);
        setContentView(splashScreenView);
             
        splashScreenTextView = (TextView) findViewById(R.id.SplashTextView);
        splashScreenTextView.setOnClickListener(this);
        
        splashScreenImageView = (ImageView) findViewById(R.id.SplashScreenImageView);
        splashScreenImageView.setOnClickListener(this);
       
        creditsTextView = findViewById(R.id.CreditsTextView);
        creditsTextView.setOnClickListener(this);
    }
    
	public void onClick(View view) {
		if (view == splashScreenTextView || view == splashScreenImageView) {
			Intent intent = new Intent(this, ObscuraApp.class);
			startActivity(intent);				
		} else if (view == creditsTextView) {
			String url = "https://guardianproject.info/apps/securecam/";
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(url));
			startActivity(i);			
		}
	}


    /*
     * Handling screen configuration changes ourselves, we don't want the activity to restart on rotation
     */
    @Override
    public void onConfigurationChanged(Configuration conf) 
    {
        super.onConfigurationChanged(conf);
    }
}
