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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

public class IdTagger extends Activity {
	Bundle b;
	int tagIndex;
	SSCCalculator calc;
	
	EditText namespace;
	CheckBox consentCheckbox;
	ImageButton confirmTag;
	String queryBuffer;
	ListView tagSuggestionsHolder;
	ArrayList<String> al;
	
	SSCMetadataHandler mdh;
	boolean shouldLookupNames, isNewSubject;
	
	private static final String SSC = "[Camera Obscura : IdTagger] ****************************";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.idtagger);
		
		namespace = (EditText) findViewById(R.id.namespace);
		consentCheckbox = (CheckBox) findViewById(R.id.consentCheckbox);
		confirmTag = (ImageButton) findViewById(R.id.confirmTag);
		confirmTag.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(v == confirmTag) {
					saveSubject(namespace.getText().toString());
				}
			}
		});
		
		calc = new SSCCalculator();
		b = getIntent().getExtras();
		try {
			tagIndex = calc.jsonGetTagId(b.getString("tagIndex"));
		} catch (Exception e) {}
		
		tagSuggestionsHolder = (ListView) findViewById(R.id.tagSuggestionsHolder);
		al = new ArrayList<String>();
		
		shouldLookupNames = false;
		isNewSubject = true;
		
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
		if(subjectName.compareTo("") != 0) {
			// add subject into table of known subjects if this is a new subject.
			if(isNewSubject) {
				int subjectIndex = mdh.insertIntoDatabase("ssc_subjects", "(s_entityName,associatedMedia)", "\"" + subjectName + "\"," + b.getInt("imageResourceCursor"));
			}
			
			int finalConsent = 0;
			if(consentCheckbox.isChecked()) {
				finalConsent = 1;
			}
			
			// add subject to tag's table ("associatedSubjects_[imageId]_[tagId]")
			mdh.insertIntoDatabase(
					"associatedSubjects_" + b.getInt("imageResourceCursor") + "_" + tagIndex,
					"(d_associatedTag,s_entityName,s_informedConsentGiven)",
					tagIndex + ",\"" + subjectName + "\"," + finalConsent
					);
			
			// and return to previous activity
		} else {
			// toast to the user that they didn't input anything
		}
		
	}
}