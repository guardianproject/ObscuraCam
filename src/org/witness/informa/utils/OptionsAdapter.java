package org.witness.informa.utils;

import java.util.ArrayList;

import org.witness.sscphase1.R;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

public class OptionsAdapter extends BaseAdapter {
	ArrayList<Options> _options;
	LayoutInflater li;
	
	public OptionsAdapter(ArrayList<Options> options, Context c) {
		_options = options;
		li = LayoutInflater.from(c);
	}
	@Override
	public int getCount() {
		return _options.size();
	}

	@Override
	public Object getItem(int position) {
		return _options.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		Options opt = _options.get(position);
		if(opt.getValue().getClass().equals(Boolean.class)) {
			convertView = li.inflate(R.layout.informa_option_checkbox, null);
			TextView optionText = (TextView) convertView.findViewById(R.id.optionText);
			CheckBox optionSelection = (CheckBox) convertView.findViewById(R.id.optionSelection);
			
			optionText.setText(opt._text);
			optionSelection.setChecked((Boolean) _options.get(position).getValue());
			optionSelection.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton button, boolean value) {
					_options.get(position).setValue(value);
				}
				
			});
		}
		return convertView;
	}

}
