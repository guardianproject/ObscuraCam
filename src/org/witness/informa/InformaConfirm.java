package org.witness.informa;

import org.witness.sscphase1.R;

import android.app.Activity;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class InformaConfirm extends Activity implements OnClickListener {
	TextView conformationTitle; 
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.informaconfirm);
		
		conformationTitle = (TextView) findViewById(R.id.conformationTitle);
	}
	
	@Override
	public void onClick(View v) {
		
		
	}
}
