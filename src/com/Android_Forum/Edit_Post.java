
package com.Android_Forum;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.Android_Forum.Utils.Helper;

public class Edit_Post extends Activity {
	private EditText title;
	private EditText text;
	private String currentThreadID;
	private String currentPostID;
	private GestureLibrary gLib;
	
    /**
     * Read data from calling activity and populate the thread's layout
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Only let them edit the post if they're logged in
        if (Helper.userID == 0) {
        	finish();
        }
        
        Bundle threadData = getIntent().getExtras();
        currentThreadID = threadData.getString("Thread_ID");
        currentPostID = threadData.getString("ID");
        viewPost(currentPostID);
    }
    
    /**
     * Populate view with the post's information
     * @param postID ID of the post to display.
     */
    private void viewPost(String postID) {
    	setContentView(R.layout.post);
    	
        title = (EditText)findViewById(R.id.title);
        text = (EditText)findViewById(R.id.text);
        
        if (!postID.equals("0")) {
	        // Get a basic JSON object
        	Helper.db.loadPost(currentPostID);
        	
        	// Check that user has permission to edit this post
        	if (!Helper.isUserAdmin && Helper.userID != Integer.valueOf(Helper.db.getValue("posted_by")))
        		finish();
        	
	        title.setText(Helper.db.getValue("name"));
	        text.setText(Helper.db.getValue("message"));
	        ((TextView)findViewById(R.id.date)).setText(
	        		"Posted by " + Helper.db.getValue("username") + " on " + Helper.db.getValue("created_on"));
	        this.setTitle(Helper.db.getValue("name"));
        }
        
        // Set up button click listeners
        // OKAY button: When a user clicks Okay, it send data to the server
        Button button = (Button)findViewById(R.id.ok);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	HashMap<String, String> data = new HashMap<String, String>();
            	data.put("name", title.getText().toString());
            	data.put("message", text.getText().toString());
            	data.put("thread_id", currentThreadID);
            	data.put("posted_by", String.valueOf(Helper.userID));
            	
            	if (currentPostID.equals("0")) {
            		Helper.db.addPost(data);
            	} else {
            		Helper.db.editPost(currentPostID, data);
            	}
            	
				setResult(RESULT_OK);
				finish();
            }
          });
        
        button = (Button)findViewById(R.id.cancel);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	setResult(RESULT_CANCELED);
            	finish();
            }
          });
        
		gLib = GestureLibraries.fromRawResource(this, R.raw.gestures);
		if (!gLib.load()) {
			Helper.makeToast(getString(R.string.gesture_load_failed), getApplicationContext());
		}
		
		GestureOverlayView gestures = (GestureOverlayView) findViewById(R.id.gestures);
		gestures.setGestureVisible(false);
		gestures.addOnGesturePerformedListener(handleGestureListener);        
    }
    
    /**
     * Monitor user gestures and perform gestures accordingly (all actions that can be triggered by
     * gestures can also be accessed through the menu or a button)
     */
	private OnGesturePerformedListener handleGestureListener = new OnGesturePerformedListener() {
		@Override
		public void onGesturePerformed(GestureOverlayView gestureView,
				Gesture gesture) {
			ArrayList<Prediction> predictions = gLib.recognize(gesture);
 
			if (predictions.size() > 0 && predictions.get(0).score > 2.0) {
				String action = predictions.get(0).name;
				if ("back".equals(action)) {
					finish();
				} else if ("refresh".equals(action)) {
					viewPost(currentPostID);
					Helper.makeToast(Edit_Post.this.getString(R.string.screen_refreshed), getApplicationContext());
				} else if ("logout".equals(action)) {
					logoutPrompt();
				}
			}
		}
	};
   
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.forumoptions, menu);
        menu.findItem(R.id.login).setVisible(false);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.refresh:
        	viewPost(currentPostID);
            return true;
            
        case R.id.logout:
        	logoutPrompt();
        	return true;
        	
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    /**
     * Prompt the user if they really want to log out. If they say yes,
     * log them out and close the current activity (because it's only for editing)
     */
    private void logoutPrompt() {
		new AlertDialog.Builder(Edit_Post.this)
		.setTitle(R.string.logout)
		.setMessage(R.string.confirm_logout)
		.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Helper.logout(getApplicationContext());
				setResult(RESULT_CANCELED);
				Edit_Post.this.finish();
			}
		})
		.setNegativeButton(R.string.no, null)
		.show();
    }
}