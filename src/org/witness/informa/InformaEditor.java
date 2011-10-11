package org.witness.informa;

import org.witness.sscphase1.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class InformaEditor extends Activity implements OnClickListener {
	SharedPreferences _sp;
	SharedPreferences.Editor _ed;
	boolean isVirginInformaUser;
	
	ImageView imageRegionThumb;
	EditText subjectNameHolder;
	Button informaSubmit;
	LinearLayout otherInformaOptionsHolder;
	
	Bundle regionInfo;
	
	public static final String LOG = "[Informa **********************]";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.informaeditor);
		
		imageRegionThumb = (ImageView) findViewById(R.id.imageRegionThumb);
		subjectNameHolder = (EditText) findViewById(R.id.subjectNameHolder);
		informaSubmit = (Button) findViewById(R.id.informaSubmit);
		otherInformaOptionsHolder = (LinearLayout) findViewById(R.id.otherInformaOptionsHolder);
		
		_sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		_ed = _sp.edit();
		
		alignPreferences();
		
		if(getIntent().hasExtra("regionInfo")) {
			regionInfo = getIntent().getBundleExtra("regionInfo");
			
			int[] dims = regionInfo.getIntArray("regionDimensions");
			int ot = regionInfo.getInt("obscureType");
			
			Log.d(LOG, "region extras:\nwidth: " + dims[0] + " height: " + dims[1] + " obscureType: " + ot);
		}
		
	}
	
	private void alignPreferences() {
		isVirginInformaUser = _sp.getBoolean("VirginInformaUser", true);
		
		// TODO: if(isVirginInformaUser) launchWizard();
	}
	
	@Override
	public void onClick(View v) {
		if(v == informaSubmit) {
			// TODO: whatever we do here to store/persist
		}
	}

}
