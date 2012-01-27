package org.witness.sscphase1;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class Wizard extends Activity implements OnClickListener {
	int wn, next, previous = 0;
	
	ScrollView sv;
	LinearLayout progress;
	TextView title;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wizard);
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onClick(View v) {
		
		
	}

}
