package org.witness.informa.utils;

import java.util.ArrayList;

import org.witness.sscphase1.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class InformaOptionsAdapter extends BaseAdapter {
	private ArrayList<InformaOptions> _informaOptions;
	LayoutInflater li;
	
	public InformaOptionsAdapter(Context c, ArrayList<InformaOptions> informaOptions) {
		_informaOptions = informaOptions;
		li = LayoutInflater.from(c);
	}
	
	@Override
	public int getCount() {
		return _informaOptions.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(final int position, View view, ViewGroup viewGroup) {
		view = li.inflate(R.layout.informa_options, null);
		CheckBox optionSelection = (CheckBox) view.findViewById(R.id.optionSelection);
		TextView optionText = (TextView) view.findViewById(R.id.optionText);
		
		optionText.setText(_informaOptions.get(position)._optionText);
		optionSelection.setSelected(_informaOptions.get(position)._isSelected);
		
		optionSelection.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				_informaOptions.get(position).setSelected(((CheckBox) v).isSelected());
			}
			
		});
		
		return view;
	}

}
