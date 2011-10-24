package org.witness.informa;

import java.util.ArrayList;
import java.util.HashMap;

import org.witness.informa.InformaOptions.InformaOption;
import org.witness.securesmartcam.ImageEditor;
import org.witness.sscphase1.ObscuraApp;
import org.witness.sscphase1.R;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

public class InformaEditor extends Activity implements OnClickListener {
	//ImageView imageRegionThumb;
	EditText subjectNameHolder;
	Button informaSubmit;
	ListView otherInformaOptionsHolder;
	ImageView imageRegionThumb;
	
	HashMap<String, String> _mProps;
	ArrayList<InformaOption> informaOption;
	
	public static final String LOG = "[Informa **********************]";
	
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.informaeditor);
		
		_mProps = (HashMap<String, String>) getIntent().getSerializableExtra("mProps");
		
		subjectNameHolder = (EditText) findViewById(R.id.subjectNameHolder);

		if(_mProps.get("regionSubject").compareTo("") != 0)
			subjectNameHolder.setText(_mProps.get("regionSubject"));
				
		if(getIntent().hasExtra("byteArray")) {
			
			imageRegionThumb = (ImageView) findViewById(R.id.imageRegionThumb);
			Bitmap b = BitmapFactory.decodeByteArray(
					getIntent().getByteArrayExtra("byteArray"),
					0,
					getIntent().getByteArrayExtra("byteArray").length
				);
			
			Matrix matrix = new Matrix();
			matrix.postScale(80f/b.getWidth(), 80f/b.getHeight());			
			Bitmap preview = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
			
			imageRegionThumb.setImageBitmap(preview);
		}
		
		informaSubmit = (Button) findViewById(R.id.informaSubmit);
		informaSubmit.setOnClickListener(this);
		
		otherInformaOptionsHolder = (ListView) findViewById(R.id.otherInformaOptionsHolder);
		
		// unpack the options user can perform on the image region
		informaOption = new ArrayList<InformaOption>();
		
		informaOption.add(new InformaOption(
				getResources().getString(R.string.informaOpt_consent),
				Boolean.parseBoolean(_mProps.get("informedConsent"))
				)
		);
		/*
		informaOption.add(new InformaOption(
				getResources().getString(R.string.informaOpt_autoFilter),
				Boolean.parseBoolean(_mProps.get("persistObscureType"))
				)
		);*/
		
		otherInformaOptionsHolder.setAdapter(new InformaOptions(this,informaOption));
		
		
	}
	
	@Override
	public void onClick(View v) {
		if(v == informaSubmit) {
			// pack up a bundle to be saved with the image
			if(subjectNameHolder.getText().toString().compareTo("") != 0) {
				_mProps.put("regionSubject", subjectNameHolder.getText().toString());
				_mProps.put("informedConsent", Boolean.toString(informaOption.get(0).getSelected()));
				//_mProps.put("persistObscureType", Boolean.toString(informaOption.get(1).getSelected()));
				
				// TODO: if(getIntent() == OBSCURA_CAM)
				// must prevent intent hijacking!
				
				getIntent().putExtra("informaReturn", _mProps);
				getIntent().putExtra("irIndex", getIntent().getIntExtra("irIndex", 0));
				setResult(Activity.RESULT_OK,getIntent());
				finish();
			} else {
				Toast.makeText(this, "You haven't identified anyone", Toast.LENGTH_LONG).show();
			}
			
			
		}
	}

}
