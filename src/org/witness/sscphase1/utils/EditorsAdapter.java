package org.witness.sscphase1.utils;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.witness.informa.utils.InformaConstants;
import org.witness.informa.utils.InformaConstants.Keys;
import org.witness.securesmartcam.utils.Selections;
import org.witness.sscphase1.Preferences;
import org.witness.sscphase1.R;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

public class EditorsAdapter extends BaseAdapter {
	ArrayList<Selections> selections;
	LayoutInflater li;
	Preferences p;
	
	public EditorsAdapter(Preferences p, ArrayList<Selections> selections) {
		this.selections = selections;
		this.p = p;
		li = LayoutInflater.from(p.getApplicationContext());
	}
	
	@Override
	public int getCount() {
		return selections.size();
	}

	@Override
	public Object getItem(int position) {
		return selections.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		convertView = li.inflate(R.layout.editor_listview, null);
		JSONObject selectionExtras = ((Selections) getItem(position)).getExtras();
		TextView editorText = (TextView) convertView.findViewById(R.id.editorText);
		
		try {
			editorText.setText(
					selectionExtras.getString(Keys.TrustedDestinations.DISPLAY_NAME) + "\n" + 
					selectionExtras.getString(Keys.TrustedDestinations.EMAIL));
		} catch (JSONException e) {}
		
		ImageButton editorDeleteButton = (ImageButton) convertView.findViewById(R.id.editorDeleteButton);
		editorDeleteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				p.removeContact(-1, selections.get(position));
			}
		});
		
		
		return convertView;
	}

}
