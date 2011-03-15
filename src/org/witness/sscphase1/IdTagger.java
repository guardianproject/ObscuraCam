package org.witness.sscphase1;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.database.SQLException;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

public class IdTagger extends Activity {
	Bundle b;
	
	EditText namespace;
	ImageButton confirmTag;
	String queryBuffer;
	ListView tagSuggestionsHolder;
	ArrayList<String> al;
	
	SSCMetadataHandler mdh;
	boolean shouldLookupNames;
	
	private static final String SSC = "[Camera Obscura : IdTagger] ****************************";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.idtagger);
		
		namespace = (EditText) findViewById(R.id.namespace);
		confirmTag = (ImageButton) findViewById(R.id.confirmTag);
		confirmTag.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(v == confirmTag) {
					saveSubject(namespace.getText().toString());
				}
			}
		});
		
		b = getIntent().getExtras();
		
		tagSuggestionsHolder = (ListView) findViewById(R.id.tagSuggestionsHolder);
		al = new ArrayList<String>();
		
		shouldLookupNames = false;
		mdh = new SSCMetadataHandler(this);
		try {
			mdh.createDatabase();
		} catch(IOException e) {}
		try {
			mdh.openDataBase();
		} catch(SQLException e) {}
		try {
			al = mdh.readBatchFromDatabase("ssc_subjects", "s_entityName", "ASC");
			Log.v(SSC,"found " + al.size() + " tags in db");
			if(al.size() > 0) {
				shouldLookupNames = true;
			}
		} catch(SQLException e) {}
		
		if(shouldLookupNames) {
			namespace.addTextChangedListener(new TextWatcher() {
				public void afterTextChanged(Editable s) {
					Log.v(SSC,"after text changed: " + s.toString());
				
				}

				public void beforeTextChanged(CharSequence s, int start, int count,
						int after) {
					Log.v(SSC,"before text changed: " + s.toString());
				
				}

				public void onTextChanged(CharSequence s, int start, int before,
						int count) {
					Log.v(SSC,"ON text changed: " + s.toString());
				
				}
			});
		}
	}
	
	public void saveSubject(String subjectName) {
		int subjectIndex = mdh.insertIntoDatabase("ssc_subjects", "(s_entityName,associatedMedia)", "\"" + subjectName + "\"," + b.getInt("imageResourceCursor"));
	}
}