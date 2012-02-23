package org.witness.sscphase1;

import info.guardianproject.database.sqlcipher.SQLiteDatabase;
import info.guardianproject.database.sqlcipher.SQLiteException;

import org.witness.informa.utils.InformaConstants.Keys;
import org.witness.informa.utils.io.DatabaseHelper;
import org.witness.securesmartcam.utils.ObscuraConstants;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class Login extends Activity implements OnClickListener {
	private Button loginButton;
	private EditText loginPasswordHolder;
	private DatabaseHelper dh;
	private SQLiteDatabase db;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		
		SQLiteDatabase.loadLibs(this);
		dh = new DatabaseHelper(this);
		db = null;
		
		loginButton = (Button) findViewById(R.id.loginButton);
		loginButton.setOnClickListener(this);
		
		loginPasswordHolder = (EditText) findViewById(R.id.loginPasswordHolder);
		loginPasswordHolder.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
		loginPasswordHolder.setTransformationMethod(new PasswordTransformationMethod());
	}
	
	public boolean tryLogin() {
		boolean isLoggedIn = false;
		String pwd = loginPasswordHolder.getText().toString();
		
		try {
			db = dh.getReadableDatabase(pwd);
			if(db.isOpen()) {
				SharedPreferences _sp = PreferenceManager.getDefaultSharedPreferences(this);
				SharedPreferences.Editor _ed = _sp.edit();
				_ed.putString(Keys.Settings.HAS_DB_PASSWORD, pwd).commit();
				isLoggedIn = true;
			}
		} catch(SQLiteException e) {
			loginPasswordHolder.setText("");
		}
		
		return isLoggedIn;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if(db != null)
			db.close();
		dh.close();
	}
	
	@Override
	public void onClick(View v) {
		if(v == loginButton && tryLogin())
			finish();
		else {
			ObscuraConstants.makeToast(getApplicationContext(), getResources().getString(R.string.login_fail));
		}
	}
}
