package org.witness.securesmartcam.utils;

import java.util.ArrayList;

import org.witness.informa.utils.InformaConstants;
import org.witness.sscphase1.R;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SelectionsAdapter extends BaseAdapter {
	ArrayList<Selections> _selections;
	boolean _isMulti;
	LayoutInflater li;
	
	public SelectionsAdapter(Context c, ArrayList<Selections> selections, String multiType) {
		_selections = selections;
		
		if(multiType.compareTo("select_one") == 0)
			_isMulti = false;
		else
			_isMulti = true;
		
		li = LayoutInflater.from(c);
	}

	@Override
	public int getCount() {
		return _selections.size();
	}

	@Override
	public Object getItem(int position) {
		return _selections.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, final ViewGroup parent) {
		convertView = li.inflate(R.layout.select_listview, null);
		TextView selectText = (TextView) convertView.findViewById(R.id.selectText);
		selectText.setText(_selections.get(position)._optionValue);
		
		CheckBox selectBox = (CheckBox) convertView.findViewById(R.id.selectBox);
		selectBox.setSelected(_selections.get(position).getSelected());
				
		if(!_isMulti) {
			
			selectBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					
					if(isChecked) {
						for(Selections s: _selections) {
							if(position != _selections.indexOf(s)) {
								LinearLayout ll = (LinearLayout) parent.getChildAt(_selections.indexOf(s));
								CheckBox cb = (CheckBox) ll.getChildAt(0);
								
								s.setSelected(false);
								cb.setChecked(false);
							} else
								s.setSelected(true);
						}
						
						
					}
				
				}
			
			});
		}
		return convertView;
	}
}
