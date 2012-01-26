package com.Android_Forum;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class TextEntry extends Activity {
	private int itemID;
	private String itemName;
	private EditText text;
	
	/**
	 * Set up modal form to take text input from the user
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.textentry);
		
		Button ok = (Button) findViewById(R.id.ok);
		Button cancel = (Button) findViewById(R.id.cancel);
		
		Bundle goodies = getIntent().getExtras();
		itemID = goodies.getInt("ID");
		itemName = goodies.getString("OldName");
		
		text = ((EditText) findViewById(R.id.text));
		
		text.setText(itemName);
		
		ok.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				String newName = text.getText().toString();
				Intent data = new Intent();
				data.putExtra("Name", newName);
				data.putExtra("ID", itemID);
				setResult(RESULT_OK, data);
				finish();
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
