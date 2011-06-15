package org.witness.securesmartcam.jpegredaction;

import android.app.Activity;
import android.os.Bundle;

public class JpegRedaction extends Activity {
    public native void redactit(String src_path, String dest_path, String regions);

    static {
        System.loadLibrary("JpegRedaction");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);

 //       redactit(path);
    }
}