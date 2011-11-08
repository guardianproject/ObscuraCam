package org.witness.informa;

import java.io.File;
import java.io.IOException;

import org.json.JSONObject;
import org.witness.informa.io.ImageReader;
import org.witness.sscphase1.R;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

public class InformaViewer extends Activity implements OnClickListener {
	ImageView imageViewer;
	ScrollView paramViewer;
	TextView params;
	
	ImageButton toggleParamViewer;
	boolean paramsShowing = true;
	
	public final static String IMAGEURI = "passedimage";
	
	Uri imageUri;
	Bitmap imageBitmap;
	
	ImageReader ir;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.informaviewer);
		
		imageViewer = (ImageView) findViewById(R.id.imageViewer);
		paramViewer = (ScrollView) findViewById(R.id.paramViewer);
		toggleParamViewer = (ImageButton) findViewById(R.id.toggleParamViewer);
		
		Bundle passedBundle = getIntent().getExtras();
		if (passedBundle.containsKey(IMAGEURI)) {
			imageUri = Uri.parse(passedBundle.getString(IMAGEURI));
			
			try {
				// Load up the image's dimensions not the image itself
				BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
				
				// Parse the image
				imageBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri), null, bmpFactoryOptions);
		
				imageViewer.setImageBitmap(imageBitmap);
				
				// start an instance of the image reader
				ir = new ImageReader(new File(imageUri.toString()));
				if(ir.isValid()) {
					// inflate param viewer and bitmap with the stuff
					params.setText(ir.getParams());
					
					if(ir.getImageRegions().size() > 0) {
						for(JSONObject imageRegion : ir.getImageRegions())
							addTappableImageRegion(imageRegion);
					}
				} else {
					params.setText("Informa cannot verify this image.  There is no data to show");
				}
				
				toggleParams();
			} catch (IOException e) {}            
        }
        else {
        	// Not passed in, nothing to display
        	finish();
		}
	}
	
	private void addTappableImageRegion(JSONObject imageRegion) {
		// TODO: adds a region to the bitmap, according to its data.
		
		// TODO: onclick brings up region data
	}
	
	private void toggleParams() {
		if(paramsShowing) {
			// close the param viewer
			paramViewer.setVisibility(View.GONE);
			
			// change the toggle button image
			toggleParamViewer.setImageDrawable(getResources().getDrawable(R.drawable.arrow_down));
			
			// reset flag
			paramsShowing = false;
		} else {
			// open the param viewer
			paramViewer.setVisibility(View.VISIBLE);
			
			// change the toggle buttom image
			toggleParamViewer.setImageDrawable(getResources().getDrawable(R.drawable.arrow_up));
			
			// reset flag
			paramsShowing = true;
		}
	}

	@Override
	public void onClick(View v) {
		if(v == toggleParamViewer) {
			toggleParams();
		}
		
	}

}