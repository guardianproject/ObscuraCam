package org.witness.informa.utils;

import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.witness.sscphase1.ObscuraApp;
import org.witness.sscphase1.R;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class KeyChooser extends BaseAdapter {
	JSONArray keys;
	Context c;
	LayoutInflater li;
	ArrayList<Long> selected;
	
	public KeyChooser(Context c, JSONArray keys) {
		this.keys = keys;
		this.c = c;
		this.li = LayoutInflater.from(c);
		this.selected = new ArrayList<Long>();
	}
	
	@Override
	public int getCount() {
		return keys.length();
	}

	@Override
	public Object getItem(int position) {
		try {
			return keys.get(position);
		} catch (JSONException e) {
			return null;
		}
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(final int position, View view, ViewGroup parent) {
		view = li.inflate(R.layout.informa_key, null);
		try {
			final JSONObject key = (JSONObject) keys.get(position);
			
			CheckBox check = (CheckBox) view.findViewById(R.id.keyCheck);
			check.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					try {
						if(isChecked && !selected.contains(key.getLong("key"))) {
							selected.add(key.getLong("key"));
						} else if(!isChecked && selected.contains(key.getLong("key"))) {
							selected.remove(key.getLong("key"));
						}
					} catch (JSONException e) {}
					
				}
				
			});
			
			TextView label = (TextView) view.findViewById(R.id.keyLabel);
			label.setText(key.getString("alias"));
			
			
		} catch (JSONException e) {}
		
		return view;
	}
	
	public long[] getSelectedKeys() {
		long[] r = new long[selected.size()];
		for(int x=0;x<r.length;x++) {
			r[x] = (Long) selected.toArray()[x];
		}
		return r;
	}

}
