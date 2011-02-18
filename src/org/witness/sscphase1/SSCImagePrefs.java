package org.witness.sscphase1;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

public class SSCImagePrefs extends Activity {
	private static final String SSC = "[SSCCameraObscura : Image Prefs] ****************************";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN );
        setContentView(R.layout.imageprefs);
    }
}
