package org.witness.sscphase1;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

public class EncryptTagger extends Activity {
	Bundle b;
	
	EditText namespace;
	ImageButton confirmTag;
	String queryBuffer;
	ListView tagSuggestionsHolder;
	ArrayList<String> al;
	
	SSCMetadataHandler mdh;
	boolean shouldLookupKeys;
	
	private static final String SSC = "[Camera Obscura : EncryptTagger] ****************************";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.encrypttagger);
		
		namespace = (EditText) findViewById(R.id.namespace);
		confirmTag = (ImageButton) findViewById(R.id.confirmTag);
		
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

}
