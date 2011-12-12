package org.witness.informa.utils;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.witness.sscphase1.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class InformaDestKeysList extends BaseAdapter {
	ArrayList<DestKeyManager> keys;
	LayoutInflater li;
	Context c;
	
	public InformaDestKeysList(Context c, ArrayList<DestKeyManager> keys) {
		this.c = c;
		this.keys = keys;
		this.li = LayoutInflater.from(this.c);
		
	}

	@Override
	public int getCount() {
		return keys.size();
	}

	@Override
	public Object getItem(int index) {
		return keys.get(index);
	}

	@Override
	public long getItemId(int item) {
		return keys.indexOf(item);
	}

	@Override
	public View getView(final int item, View view, ViewGroup viewGroup) {
		view = li.inflate(R.layout.informa_destkeys, null);
		
		TextView label = (TextView) view.findViewById(R.id.destKey_label);
		ImageButton edit = (ImageButton) view.findViewById(R.id.destKey_edit);
		edit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Change name
				
			}
			
		});
		
		ImageButton delete = (ImageButton) view.findViewById(R.id.destKey_delete);
		delete.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				
			}
			
		});
		
		label.setText(keys.get(item).alias);
		
		return view;
	}
	
	public static class DestKeyManager extends JSONObject {
		String alias;
		String email;
		long key;
		
		public DestKeyManager(String alias, String email, long key) throws JSONException {
			this.alias = alias;
			this.email = email;
			this.key = key;
			
			this.put("alias", alias);
			this.put("email", email);
			this.put("key", key);
		}
	}
}
