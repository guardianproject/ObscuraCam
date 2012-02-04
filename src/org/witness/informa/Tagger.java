package org.witness.informa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.witness.informa.utils.InformaConstants;
import org.witness.informa.utils.InformaConstants.Keys.ImageRegion;
import org.witness.informa.utils.Options;
import org.witness.informa.utils.OptionsAdapter;
import org.witness.securesmartcam.utils.ObscuraConstants;
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

public class Tagger extends Activity implements OnClickListener {
	EditText subjectNameHolder;
	ImageView imageRegionThumb;
	Button informaSubmit;
	
	HashMap<String, String> mProps;
	ArrayList<Options> informaOptions;
	ListView otherInformaOptionsHolder;
	
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.informaeditor);
		
		mProps = (HashMap<String, String>) getIntent().getSerializableExtra(ObscuraConstants.ImageRegion.PROPERTIES);
		Log.d(InformaConstants.TAG, mProps.toString());
		
		try {
			informaOptions = parseChecklist();
		} catch (IOException e) {
			Log.e(InformaConstants.TAG, e.toString());
		}
		

		subjectNameHolder = (EditText) findViewById(R.id.subjectNameHolder);
		if(mProps.get(ImageRegion.Subject.PSEUDONYM).compareTo("") != 0)
			subjectNameHolder.setText(mProps.get(ImageRegion.Subject.PSEUDONYM));
		
		if(getIntent().hasExtra(ImageRegion.THUMBNAIL)) {
			byte[] ba = getIntent().getByteArrayExtra(ImageRegion.THUMBNAIL);
			Bitmap b = BitmapFactory.decodeByteArray(ba,0,ba.length);
			Matrix m = new Matrix();
			m.postScale(80f/b.getWidth(), 80f/b.getHeight());
			Bitmap preview = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
			
			imageRegionThumb = (ImageView) findViewById(R.id.imageRegionThumb);
			imageRegionThumb.setImageBitmap(preview);
		}
		
		informaSubmit = (Button) findViewById(R.id.informaSubmit);
		informaSubmit.setOnClickListener(this);
		
		otherInformaOptionsHolder = (ListView) findViewById(R.id.otherInformaOptionsHolder);
		otherInformaOptionsHolder.setAdapter(new OptionsAdapter(informaOptions, this));
		
	}
	
	private String getKey(String s, String key) {
		if(s.indexOf(key) != -1) {
			String keyTail = s.substring(s.indexOf(key + "="));
			String[] pair = keyTail.substring(0, keyTail.indexOf(";")).split("=");
			return pair[1];
		} else {
			return null;
		}
	}
	
	private ArrayList<Options> parseChecklist() throws IOException {
		ArrayList<Options> io = new ArrayList<Options>();
		BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("informa.checklist")));
		String line;
		
		while((line = br.readLine()) != null) {
			String id = getKey(line, "id");
			String value;
			
			// cycle through mProps to find matching id.  if it's set, this is the value
			if(mProps.containsKey(id))
				value = mProps.get(id);
			else
				value = getKey(line, "defaultValue");
			
			io.add(new Options(this, getKey(line, "type"), value, getKey(line, "text"), id));
		}
		
		br.close();
		return io;
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		if(v == informaSubmit) {
			if(subjectNameHolder.getText().toString().compareTo("") != 0) {
				mProps.put(ImageRegion.Subject.PSEUDONYM, subjectNameHolder.getText().toString());
				
				for(Options opt : informaOptions)
					mProps.put(opt.getId(), opt.getValueAsString());
				
				getIntent().putExtra(ImageRegion.TAGGER_RETURN, mProps);
				getIntent().putExtra(ImageRegion.INDEX, getIntent().getIntExtra(ImageRegion.INDEX, 0));
				setResult(Activity.RESULT_OK,getIntent());
				finish();
				
			} else {
				ObscuraConstants.makeToast(getApplicationContext(), getResources().getString(R.string.tagger_no_name_error));
			}
		}
		
	}
}
