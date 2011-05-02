package org.witness.securesmartcam.jpegredaction;

import android.app.Activity;
import android.os.Bundle;

public class JpegRedaction extends Activity {
    private native void redactit();

    static {
        System.loadLibrary("JpegRedaction");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);

        redactit();
    }
}