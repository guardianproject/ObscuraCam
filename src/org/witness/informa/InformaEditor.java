package org.witness.informa;

import java.util.ArrayList;

import org.witness.informa.utils.InformaOptions;
import org.witness.informa.utils.InformaOptionsAdapter;
import org.witness.sscphase1.R;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.ListView;
import android.widget.Toast;

public class InformaEditor extends Activity implements OnClickListener {
	SharedPreferences _sp;
	SharedPreferences.Editor _ed;
	boolean isVirginInformaUser;
	
	ImageView imageRegionThumb;
	EditText subjectNameHolder;
	Button informaSubmit;
	ListView otherInformaOptionsHolder;
	
	Bundle regionRect,regionInfo;
	int regionId, obscureType;
	
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
		informaSubmit.setOnClickListener(this);
		
		otherInformaOptionsHolder = (ListView) findViewById(R.id.otherInformaOptionsHolder);
		
		// unpack the options user can perform on the image region
		informaOptions = new ArrayList<InformaOptions>();
		informaOptions.add(new InformaOptions(getResources().getString(R.string.informaOpt_consent),false));
		informaOptions.add(new InformaOptions(getResources().getString(R.string.informaOpt_autoFilter),false));
		
		_sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		_ed = _sp.edit();
		
		alignPreferences();
		
		if(getIntent().hasExtra("regionInfo")) {
			// and it should have it!
			regionInfo = getIntent().getBundleExtra("regionInfo");
						
			regionId = regionInfo.getInt("regionId");
			obscureType = regionInfo.getInt("obscureType");
			regionRect = regionInfo.getBundle("regionRect");
			
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
			// pack up a bundle to be saved with the image
			if(subjectNameHolder.getText().toString().compareTo("") != 0) {
				Bundle informaReturn = new Bundle();
				
				informaReturn.putString("regionSubject", subjectNameHolder.getText().toString());
				informaReturn.putBoolean("informedConsent", informaOptions.get(0).getSelected());
				informaReturn.putBoolean("persistObscureType", informaOptions.get(1).getSelected());
				
				// this should be handled better...
				informaReturn.putInt("regionId", regionId);
				
				// TODO: if(getIntent() == OBSCURA_CAM)
				// must prevent intent hijacking!
				
				getIntent().putExtra("informaReturn", informaReturn);
				setResult(Activity.RESULT_OK,getIntent());
				finish();
			} else {
				Toast.makeText(this, "You haven't identified anyone", Toast.LENGTH_LONG).show();
			}
			
			
		}
	}

}
