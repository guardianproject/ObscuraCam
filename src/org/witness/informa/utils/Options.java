package org.witness.informa.utils;

import android.content.Context;

public class Options {
	String _text; 
	private String _id;
	Object _value;
	
	public Options(Context c, String type, Object defaultValue, String text, String id) {
		_text = text;
		_id = id;
		
		if(type.compareTo("boolean") == 0)
			_value = new Boolean((String) defaultValue);
	}
	
	public Object getValue() {
		return _value;
	}
	
	public String getValueAsString() {
		return _value.toString();
	}
	
	public String getId() {
		return _id;
	}
	
	public void setValue(Object value) {
		if(value.getClass().equals(_value.getClass()))
			_value = value;
	}
}
