package org.witness.informa;

import java.util.ArrayList;

import org.witness.sscphase1.ObscuraApp;
import org.witness.sscphase1.R;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class InformaOptions extends BaseAdapter {
	private ArrayList<InformaOption> _informaOptions;
	LayoutInflater li;
	
	public InformaOptions(Context c, ArrayList<InformaOption> informaOptions) {
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
		optionSelection.setChecked(_informaOptions.get(position).getSelected());
		
		optionSelection.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton cb, boolean isChecked) {
				_informaOptions.get(position).setSelected(isChecked);
			}
			
		});
		
		
		return view;
	}
	
	public static class InformaOption {
		String _optionText;
		boolean _isSelected;
		
		public InformaOption(String optionText, boolean isSelected) {
			_optionText = optionText;
			_isSelected = isSelected;
		}
		
		public void setSelected(boolean selection) {
			_isSelected = selection;
		}
		
		public boolean getSelected() {
			return _isSelected;
		}
	}

}
