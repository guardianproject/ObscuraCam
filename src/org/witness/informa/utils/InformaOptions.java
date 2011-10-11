package org.witness.informa.utils;

import org.witness.sscphase1.R;

import android.content.Context;
import android.graphics.drawable.Drawable;

public class InformaOptions {
	String _optionText;
	Drawable _displayIcon, unselectedIcon, selectedIcon;
	boolean _isSelected;
	
	public InformaOptions(Context c, String optionText, boolean isSelected) {
		_optionText = optionText;
		_isSelected = isSelected;
		
		unselectedIcon = c.getResources().getDrawable(R.drawable.ic_context_fill);
		selectedIcon = c.getResources().getDrawable(R.drawable.action_item_selected);
		
		if(_isSelected)
			_displayIcon = selectedIcon;
		else
			_displayIcon = unselectedIcon;
	}
}
