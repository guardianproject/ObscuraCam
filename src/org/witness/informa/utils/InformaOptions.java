package org.witness.informa.utils;

public class InformaOptions {
	String _optionText;
	boolean _isSelected;
	
	public InformaOptions(String optionText, boolean isSelected) {
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
