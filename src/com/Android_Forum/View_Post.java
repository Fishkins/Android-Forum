package com.Android_Forum;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.TextView;

import com.Android_Forum.Utils.Helper;

public class View_Post extends Activity {
	private TextView title;
	private TextView text;
	private String currentThreadID;
	private String currentPostID;
	private GestureLibrary gLib;
	private int userID = 0;
	private boolean canEdit;
	
    /**
     * Read data from calling activity and populate the thread's layout
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Helper.initDBHelper(getApplicationContext());
        
        userID = Helper.userID;
        
        Bundle threadData = getIntent().getExtras();
        currentThreadID = threadData.getString("Thread_ID");
        currentPostID = threadData.getString("ID");
        
        viewPost(currentPostID);
    }
    
    /**
     * Refreshes view if login information changed
     */
    @Override
    public void onResume() {
    	super.onResume();
    	if (userID != Helper.userID) {
    		viewPost(currentPostID);
    	}
    }
    
    /**
     * Stores user ID this view was loaded with so we know if it changes
     */
    @Override
    public void onPause() {
    	super.onPause();
    	userID = Helper.userID;
    }
    
    /**
     * Populate view with the post's information
     * @param postID Post ID.
     */
    private void viewPost(String postID) {
    	setContentView(R.layout.postview);
    	
        title = (TextView)findViewById(R.id.title);
        text = (TextView)findViewById(R.id.text);
        
        // Get a basic JSON object and print to screen
    	Helper.db.loadPost(postID);
        title.setText(Helper.db.getValue("name"));
        text.setText(Helper.db.getValue("message"));
        ((TextView)findViewById(R.id.date)).setText(
    			"Posted by " + Helper.db.getValue("username") + " on " + Helper.db.getValue("created_on"));
        
        this.setTitle(Helper.db.getValue("name"));
        
        canEdit = Helper.isUserAdmin || Helper.userID == Integer.parseInt(Helper.db.getValue("posted_by"));
        
        // Only allow users to edit this post if they're an admin or it's their post
        if (canEdit) {
        	Button editButton = (Button) findViewById(R.id.edit);
        	Button deleteButton = (Button) findViewById(R.id.delete);
        	
        	editButton.setVisibility(View.VISIBLE);
        	deleteButton.setVisibility(View.VISIBLE);
        	
        	editButton.setOnClickListener(new OnClickListener() {
	            @Override
	            public void onClick(View v) {
	            	Intent viewIntent = new Intent(View_Post.this, Edit_Post.class);
	            	Bundle postData = new Bundle();
	            	postData.putString("ID", currentPostID);
	            	postData.putString("Thread_ID", currentThreadID);
	            	viewIntent.putExtras(postData);
	            	View_Post.this.startActivityForResult(viewIntent,Helper.EDIT_REQUEST_CODE);
	            }
	        });
        	deleteButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					deletePost();
				}
			});
        }
        
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
					Helper.makeToast(View_Post.this.getString(R.string.screen_refreshed), getApplicationContext());
					
				} else if ("login".equals(action)) {
					if (Helper.userID == 0)
						startActivityForResult(new Intent(View_Post.this, Login.class), Helper.LOGIN_REQUEST_CODE);
					
				} else if ("logout".equals(action)) {
					if (Helper.userID > 0) {
						Helper.logout(getApplicationContext());
						viewPost(currentPostID);
					}
					
				} else if ("delete".equals(action)) {
					if (canEdit) {
						deletePost();
					}
				}
			}
		}
	};
   
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.forumoptions, menu);
        return true;
    }
    
    /**
     * Filter menu options so the user can only log in or out if they're
     * already logged out or in, respectively.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	if (Helper.userID > 0) {
    		menu.findItem(R.id.login).setVisible(false);
    		menu.findItem(R.id.logout).setVisible(true);
    	} else {
    		menu.findItem(R.id.login).setVisible(true);
    		menu.findItem(R.id.logout).setVisible(false);
    	}
    	
        return true;
    }
    
    /**
     * Perform the action the user selected in the options menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.refresh:
        	viewPost(currentPostID);
            return true;
            
        case R.id.login:
        	startActivityForResult(new Intent(this, Login.class), Helper.LOGIN_REQUEST_CODE);
        	Helper.makeToast(String.valueOf(Helper.userID), getApplicationContext());
        	return true;
        	
        case R.id.logout:
        	Helper.logout(getApplicationContext());
        	viewPost(currentPostID);
        	return true;
        	
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    /**
     * Monitor resultcodes of activities we call so we can refresh when there's new information
     * to display
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	// Refresh view if the post was edited or we logged in
    	if (resultCode == RESULT_OK) {
    		switch (requestCode) {
    		case Helper.LOGIN_REQUEST_CODE:
    			viewPost(currentPostID);
    			break;
    			
    		case Helper.EDIT_REQUEST_CODE:
    			// Refresh the view and indicate that thread view also needs to refresh
    			viewPost(currentPostID);
    			setResult(RESULT_OK);
    			break;
    		}
    	}
    }
    
    /**
     * Delete the post currently being viewed and close the activity,
     * taking us back to the thread
     */
    private void deletePost() {  
		final String postText = ((TextView) findViewById(R.id.title)).getText().toString();
		
		new AlertDialog.Builder(View_Post.this)
			.setTitle(R.string.del_post)
			.setMessage(R.string.confirm_del_post)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					Helper.makeToast(postText + getString(R.string.deleted), getApplicationContext());
	            	Helper.db.deletePost(currentPostID);
					setResult(RESULT_OK);
					finish();
				}
			})
			.setNegativeButton(R.string.no, null)
			.show();
	}
}
