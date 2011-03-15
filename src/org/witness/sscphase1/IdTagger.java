package org.witness.sscphase1;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ListView;

public class IdTagger extends Activity {
	EditText namespace;
	ListView tagSuggestionsHolder;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.idtagger);
		
		namespace = (EditText) findViewById(R.id.namespace);
		tagSuggestionsHolder = (ListView) findViewById(R.id.tagSuggestionsHolder);
		
		
	}

}