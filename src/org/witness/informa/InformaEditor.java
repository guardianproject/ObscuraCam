package org.witness.informa;

import java.util.ArrayList;

import org.witness.informa.utils.InformaOptions;
import org.witness.informa.utils.InformaOptionsAdapter;
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
import android.widget.ListView;

public class InformaEditor extends Activity implements OnClickListener {
	SharedPreferences _sp;
	SharedPreferences.Editor _ed;
	boolean isVirginInformaUser;
	
	ImageView imageRegionThumb;
	EditText subjectNameHolder;
	Button informaSubmit;
	ListView otherInformaOptionsHolder;
	
	Bundle regionInfo;
	ArrayList<InformaOptions> informaOptions;
	
	public static final String LOG = "[Informa **********************]";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.informaeditor);
		
		
		imageRegionThumb = (ImageView) findViewById(R.id.imageRegionThumb);
		subjectNameHolder = (EditText) findViewById(R.id.subjectNameHolder);
		informaSubmit = (Button) findViewById(R.id.informaSubmit);
		
		otherInformaOptionsHolder = (ListView) findViewById(R.id.otherInformaOptionsHolder);
		
		// unpack the options user can perform on the image region
		informaOptions = new ArrayList<InformaOptions>();
		informaOptions.add(new InformaOptions(this,getResources().getString(R.string.informaOpt_consent),false));
		informaOptions.add(new InformaOptions(this,getResources().getString(R.string.informaOpt_autoFilter),false));
		
		_sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		_ed = _sp.edit();
		
		alignPreferences();
		
		if(getIntent().hasExtra("regionInfo")) {
			regionInfo = getIntent().getBundleExtra("regionInfo");
			
			int[] dims = regionInfo.getIntArray("regionDimensions");
			int ot = regionInfo.getInt("obscureType");
			
			Log.d(LOG, "region extras:\nwidth: " + dims[0] + " height: " + dims[1] + " obscureType: " + ot);
		}
		
		otherInformaOptionsHolder.setAdapter(new InformaOptionsAdapter(this,informaOptions));
		
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
