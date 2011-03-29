package org.witness.sscphase1;

import java.io.IOException;
import java.util.ArrayList;

import org.witness.sscphase1.secure.Apg;

import android.app.Activity;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

public class EncryptTagger extends Activity {
	Bundle b;
	
	EditText namespace;
	//ImageButton confirmTag;
	Button _btnSelectKey;
	
	String queryBuffer;
	ListView tagSuggestionsHolder;
	ArrayList<String> al;
	
	SSCMetadataHandler mdh;
	boolean shouldLookupKeys;
	

	private Apg _apg;
	
		private static final String SSC = "[Camera Obscura : EncryptTagger] ****************************";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.encrypttagger);
	
		_apg = Apg.createInstance();
		_apg.isAvailable(this);
		
		namespace = (EditText) findViewById(R.id.namespace);
		//confirmTag = (ImageButton) findViewById(R.id.confirmTag);
		_btnSelectKey = (Button) findViewById(R.id.selectKey);
		
		_btnSelectKey.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(v == _btnSelectKey) {
					selectPublicKeys();
				}
			}
		});
		
		b = getIntent().getExtras();
		
		tagSuggestionsHolder = (ListView) findViewById(R.id.tagSuggestionsHolder);
		al = new ArrayList<String>();
		
		shouldLookupKeys = false;
		mdh = new SSCMetadataHandler(this);
		try {
			mdh.createDatabase();
		} catch(IOException e) {}
		try {
			mdh.openDataBase();
		} catch(SQLException e) {}
		
	}
	
	private void selectPublicKeys ()
	{
		_apg.selectEncryptionKeys(this, null);
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		StringBuffer sb = new StringBuffer();
		
		long[] keys = _apg.getEncryptionKeys();
		
		
		if (keys != null)
		{
			for (int i = 0; i < keys.length; i++)
			{
				String userId = _apg.getPublicUserId(this, keys[i]);
				sb.append(userId);
				sb.append(' ');
				
			}
		}
		namespace.setText(sb.toString());
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	
		super.onActivityResult(requestCode, resultCode, data);
		
		_apg.onActivityResult(this, requestCode, resultCode, data);
		
	}
	
	

}
