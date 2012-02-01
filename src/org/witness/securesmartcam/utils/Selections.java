package org.witness.securesmartcam.utils;

public class Selections {
	String _optionValue;
	boolean _selected;
	
	public Selections(String optionValue, boolean selectDefault) {
		_optionValue = optionValue;
		_selected = selectDefault;
	}
	
	public boolean getSelected() {
		return _selected;
	}
	
	public void setSelected(boolean selected) {
		_selected = selected;
	}
}
