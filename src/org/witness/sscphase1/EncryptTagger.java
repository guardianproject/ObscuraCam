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
import android.widget.Toast;

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
	private long[] _selectedPublicKeys;
	private long _selectedPrivateKey;
	
	private static final String SSC = "[Camera Obscura : EncryptTagger] ****************************";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.encrypttagger);
	
		_apg = Apg.createInstance();
		
		
		if (!_apg.isAvailable(this))
		{
			//should prompt to install, open the market etc
		}
		
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
	
	private void selectPrivateKey ()
	{
		_apg.selectSecretKey(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if (_apg.isAvailable(this))
		{
			
			
			
			//first check for the current set of select keys
			long[] _selectedPublicKeys = _apg.getEncryptionKeys();
			
			if (_selectedPublicKeys != null)
			{
				StringBuffer sb = new StringBuffer();
				
				for (long key : _selectedPublicKeys)
				{
					String userId = _apg.getPublicUserId(this, key);
					sb.append(userId);
					sb.append(',');
					
				}
			
				String keySet = sb.toString();
				namespace.setText(keySet.substring(0,keySet.length()-1));
			}
			else
			{
				//load persisted key ids from database?
			}
			
			_selectedPrivateKey = _apg.getSignatureKeyId();
			
			if (_selectedPrivateKey == 0)
			{
				Toast.makeText(this, "Please choose your private key", Toast.LENGTH_SHORT).show();
				selectPrivateKey ();
			}
			
			//then check if there is any encrypted data
			String lastEncryptedData = _apg.getEncryptedData();
			if (lastEncryptedData != null)
			{
				//this is base64 encode binary data
				
				Toast.makeText(this, "Secured! -> " + lastEncryptedData, Toast.LENGTH_LONG).show();
				
			}
		}
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	
		super.onActivityResult(requestCode, resultCode, data);
		
		_apg.onActivityResult(this, requestCode, resultCode, data);
		
	}
	
	private void doEncryptionTest (String asecretmessage)
	{
		
		//set the keys either just selected or loaded from database
		_apg.setEncryptionKeys(_selectedPublicKeys);

		//will encrypted using selected keys		
		_apg.encrypt(this, asecretmessage);
			
	}
	

}
