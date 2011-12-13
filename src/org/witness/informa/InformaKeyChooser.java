package org.witness.informa;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.witness.informa.utils.KeyChooser;
import org.witness.sscphase1.ObscuraApp;
import org.witness.sscphase1.R;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ListView;

public class InformaKeyChooser extends Activity implements OnClickListener {
	Button keyChooser_ok;
	ListView keyChooser;
	JSONArray keys;
	KeyChooser keyChooserAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.informa_choosekeys);
		
		if(!getIntent().hasExtra("destKeys")) {
			setResult(Activity.RESULT_CANCELED);
			finish();
		}
		
		try {
			JSONObject j = new JSONObject(getIntent().getStringExtra("destKeys"));
			keys = j.getJSONArray("destKeys");
		} catch (JSONException e) {}
		
		keyChooser_ok = (Button) findViewById(R.id.keyChooser_ok);
		keyChooser_ok.setOnClickListener(this);
		
		keyChooser = (ListView) findViewById(R.id.keyChooser);
		keyChooserAdapter = new KeyChooser(this, keys);
		keyChooser.setAdapter(keyChooserAdapter);		
	}

	@Override
	public void onClick(View v) {
		if(v == keyChooser_ok) {
			getIntent().putExtra("selectedKeys", keyChooserAdapter.getSelectedKeys());
			setResult(Activity.RESULT_OK, getIntent());
			finish();
		}
		
	}

}
