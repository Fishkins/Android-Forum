package com.Android_Forum;

import java.util.ArrayList;
import java.util.HashMap;

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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.Android_Forum.Utils.Helper;

public class View_Main extends Activity {
    /** Called when the activity is first created. */
	private EditText title;
	private HashMap<Integer, Integer> permissions;
	private int userID = 0;
	private GestureLibrary gLib;

	/**
	 * If the user has stored login information, log them in.
	 * Load a list of all the threads in the forum.
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Helper.initDBHelper(getApplicationContext());
        
        if (!Helper.isOnline(getApplicationContext())) {
        	// We need an Internet connection for this app
        	Helper.makeToast("Fishkins' Forum requires an internet connecton. Exiting.", getApplicationContext());
        	finish();
        } else {
	        // Check for stored login information
	        userID = Helper.autoLogin(getApplicationContext());
	        
	        viewMain();
        }
    }
    
    /**
     * Refreshes view if login information changed
     */
    @Override
    public void onResume() {
    	super.onResume();
    	if (userID != Helper.userID) {
        	viewMain();
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
     * Populates screen with display of all threads
     */
    private void viewMain() {	
    	userID = Helper.userID;
    	
        // Load list of threads from the server
        Helper.db.loadThreads();
        
        setContentView(R.layout.main);
        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.layout);

        RelativeLayout currentThread;
        TextView threadTitle;
    	String id;
    	permissions = new HashMap<Integer, Integer>();
    	
    	LayoutInflater vi = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
    	
    	if (Helper.db.numRows() == 0) {
    		String noThreadsText = getResources().getString(R.string.no_threads);
    		
    		if (userID == 0)
    			noThreadsText += " " + getResources().getString(R.string.not_logged_in);
    		
    		((TextView) findViewById(R.id.threads_header)).setText(noThreadsText);
    	}
    	
    	// Add each thread to the display
    	for (int i=0; i < Helper.db.numRows(); i++){
    		vi.inflate(R.layout.threadcontent, mainLayout);
    		
    		currentThread = (RelativeLayout) findViewById(R.id.content_layout);
    		threadTitle = (TextView) currentThread.findViewById(R.id.title);
    		
    		id = Helper.db.getID(i);
    		
    		currentThread.setId(Integer.parseInt(id.trim()));
    		threadTitle.setText(Helper.db.getValue(i, "name"));
    		
    		currentThread.setOnClickListener(new OnClickListener() {
	            @Override
	            public void onClick(View v) {  
	            	viewThread(v);
	            }
	        });

        	// Record whether user can edit this thread
        	permissions.put(new Integer(id), Integer.parseInt(Helper.db.getValue(i, "posted_by")));
    		
    		registerForContextMenu(currentThread);
    	}
        
		title = (EditText) findViewById(R.id.entry);
    	
    	Button addButton = (Button) findViewById(R.id.add);
        
        if (Helper.userID == 0) {
        	// Only allow users to add new threads if they're logged in
        	addButton.setVisibility(View.GONE);
        	title.setVisibility(View.GONE);
        	findViewById(R.id.header).setVisibility(View.GONE);
        } else {
	        // Set up button click listeners
	        // Add button: When a user clicks Add, it will send the info to the server
	        //             to create a new thread.
	        addButton.setOnClickListener(new OnClickListener() {
	            @Override
	            public void onClick(View v) { 
	            	// Collect data from layout
	            	HashMap<String, String> data = new HashMap<String, String>();
	            	data.put("name", title.getText().toString());
	            	data.put("posted_by", String.valueOf(Helper.userID));
	            	
	            	Helper.db.addThread(data);
					Helper.makeToast(title.getText().toString() + getString(R.string.added), getApplicationContext());
					viewMain();
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
     * Create a context menu with options appropriate the user's permissions
     */
    @Override  
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {  
    super.onCreateContextMenu(menu, v, menuInfo);  
        menu.setHeaderTitle(((TextView) v.findViewById(R.id.title)).getText());  
        menu.add(0, v.getId(), 0, R.string.view);
        menu.add(0,v.getId(), 0, R.string.share);
        
        // Let the user Rename/Delete this thread if they created it or are an admin
        if (Helper.isUserAdmin || permissions.get(v.getId()) == Helper.userID) {
	        menu.add(0, v.getId(), 0, R.string.rename);
	        menu.add(0, v.getId(), 0, R.string.delete);
        }
    }  
    
    /**
     * Call the appropriate function when a context menu item is selected
     */
    @Override  
    public boolean onContextItemSelected(MenuItem item) {  
        if (item.getTitle()==getString(R.string.view)) {viewThread(findViewById(item.getItemId()));}  
        else if (item.getTitle()==getString(R.string.rename)) {renameThread(item.getItemId());}
        else if (item.getTitle()==getString(R.string.delete)) {deleteThread(item.getItemId());}  
        else if (item.getTitle()==getString(R.string.share)) {shareThread(item.getItemId());}  
        else {return false;}  
        return true;  
    }  
    
    /**
     * Open a thread in the thread view activity.
     * @param v View containing the thread ID and Name.
     */
    private void viewThread(View v) {  
    	TextView clickedThread = (TextView) v.findViewById(R.id.title);
    	Intent viewIntent = new Intent(View_Main.this, View_Thread.class);
    	
    	Bundle threadData = new Bundle();
    	threadData.putString("ID", Integer.toString(v.getId()));
    	threadData.putString("Name", clickedThread.getText().toString());
    	threadData.putInt("UserID", permissions.get(v.getId()));
    	
    	viewIntent.putExtras(threadData);
    	View_Main.this.startActivityForResult(viewIntent, Helper.EDIT_REQUEST_CODE);
    }  
    
    /**
     * Pop up a text entry layout to get the thread's new name
     * @param id ID of the thread to be modified
     */
    private void renameThread(int id) {
    	Intent i = new Intent(this,TextEntry.class);
    	i.putExtra("ID", id);
    	i.putExtra("OldName", ((TextView) findViewById(id).findViewById(R.id.title)).getText().toString());
    	startActivityForResult(i, Helper.RENAME_REQUEST_CODE);
    }
    
    /**
     * Share the title text to the thread with any app that handles ACTION_SEND (e.g. twitter, email, IM)
     * @param id ID of the thread to take the text from
     */
    private void shareThread(int id) {
    	Intent i = new Intent(android.content.Intent.ACTION_SEND);
    	
    	i.setType("text/plain");
    	i.putExtra(Intent.EXTRA_SUBJECT, R.string.forum_post);
    	i.putExtra(Intent.EXTRA_TEXT, ((TextView) findViewById(id).findViewById(R.id.title)).getText().toString());
    	
    	startActivity(Intent.createChooser(i, getString(R.string.share)));
    }
    
    /**
     * Process response from other activities we opened. If it was the rename activity, send the new name
     * to the server. If it was a login or edit activity, refresh the screen to show any new data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (resultCode == RESULT_OK) {
    		switch (requestCode) {
    		case Helper.RENAME_REQUEST_CODE:
				// Collect data from layout
				HashMap<String, String> hashData = new HashMap<String, String>();
				Bundle extras = data.getExtras();
				hashData.put("name", extras.getString("Name"));
				
	        	Helper.db.renameThread(String.valueOf(extras.getInt("ID")), hashData);
				viewMain();
				break;
				
    		case Helper.LOGIN_REQUEST_CODE:
    			viewMain();
    			break;
    		
    		case Helper.EDIT_REQUEST_CODE:
    			viewMain();
    			break;
    		}
    	}
    }
  
    /**
     * Delete a thread and its posts from the database
     * @param id ID of the thread to delete
     */
    private void deleteThread(int id) {  
		final String clickedButton = Integer.toString(id);
		final String clickedThreadText = ((TextView) findViewById(id).findViewById(R.id.title)).getText().toString();
		
		new AlertDialog.Builder(View_Main.this)
			.setTitle(R.string.del_thread)
			.setMessage(R.string.confirm_del_thread)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Helper.makeToast(clickedThreadText + getString(R.string.deleted), getApplicationContext());
	            	Helper.db.deleteThread(clickedButton);
					viewMain();
				}
			})
			.setNegativeButton(R.string.no, null)
			.show();
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
					viewMain();
					Helper.makeToast(getString(R.string.screen_refreshed), getApplicationContext());
				} else if ("login".equals(action)) {
					if (Helper.userID == 0)
						startActivityForResult(new Intent(View_Main.this, Login.class), Helper.LOGIN_REQUEST_CODE);
				} else if ("logout".equals(action)) {
					if (Helper.userID > 0) {
						Helper.logout(getApplicationContext());
						viewMain();
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
        	viewMain();
            return true;
            
        case R.id.login:
        	startActivityForResult(new Intent(this, Login.class), Helper.LOGIN_REQUEST_CODE);
        	return true;
        	
        case R.id.logout:
        	Helper.logout(getApplicationContext());
        	viewMain();
        	return true;
        	
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}