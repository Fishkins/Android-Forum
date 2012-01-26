package com.Android_Forum;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.Android_Forum.Utils.Helper;

public class Login extends Activity {
	private EditText userNameEntry;
	private EditText passwordEntry;
	
	/**
	 * Set up modal form to take user login info and verify it against the db
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.login);
		
		Button ok = (Button) findViewById(R.id.ok);
		Button cancel = (Button) findViewById(R.id.cancel);
		
		userNameEntry = (EditText) findViewById(R.id.username);
		passwordEntry = (EditText) findViewById(R.id.password);
		
		ok.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				String userName = userNameEntry.getText().toString();
				String password = passwordEntry.getText().toString();
				
				Helper.db.logUser(userName, password);
	        	
	        	if (Helper.db.getValue("id").length() > 0) {
	        		// Save the UserID in a file for future auto-login
	        		Helper.writeFile("UserID", Helper.db.getValue("id"), getApplicationContext());
	        		
	        		Helper.userID = Integer.parseInt(Helper.db.getValue("id"));
	        		Helper.isUserAdmin = Helper.db.getValue("is_admin").equalsIgnoreCase("1");
	        		
	        		Helper.makeToast(getString(R.string.logged_in) + userName, getApplicationContext());
	        		
					setResult(RESULT_OK);
					finish();
					
	        	} else {
	        		Helper.makeToast(getString(R.string.invalid_login), getApplicationContext());
	        		passwordEntry.setText("");
	        	}
			}
		});
		
		cancel.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
	}
}
