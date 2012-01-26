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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.Android_Forum.Utils.Helper;

public class View_Thread extends Activity {
	private String currentThreadID, currentThreadName;
	private int currentThreadUserID;
	private GestureLibrary gLib;
	private int userID = 0;
	private HashMap<Integer, Boolean> permissions;
	
    /**
     * Read data from calling activity and populate the thread's layout
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        userID = Helper.userID;
        
        Bundle threadData = getIntent().getExtras();
        currentThreadID = threadData.getString("ID");
        currentThreadName = threadData.getString("Name");
        currentThreadUserID = threadData.getInt("UserID");
        
        this.setTitle(currentThreadName);
        
        viewThread();
    }
    
    /**
     * Refreshes view if login information changed
     */
    @Override
    public void onResume() {
    	super.onResume();
    	if (userID != Helper.userID) {
        	viewThread();
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
     * Populate view with all the posts in this thread
     */
    private void viewThread() {
    	userID = Helper.userID;
    	
        // Load list of posts from the server
    	Helper.db.loadThread(currentThreadID);
        
    	// Set up layout
    	setContentView(R.layout.thread);
    	LinearLayout threadLayout = (LinearLayout) findViewById(R.id.linear);

    	RelativeLayout currentPost;
    	String postID;
    	TextView postTitle, postDate, postText;
    	permissions = new HashMap<Integer, Boolean>();
    	Boolean canEdit;
    	LayoutInflater vi = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
    	
    	if (Helper.db.numRows() == 0) {
    		String noPostsText = getResources().getString(R.string.no_posts);
    		
    		if (userID == 0)
    			noPostsText += " " + getResources().getString(R.string.not_logged_in);
    		
    		((TextView) findViewById(R.id.header)).setText(noPostsText);
    	}
    	
    	// Add a display of each post in this thread
    	for (int i=0; i < Helper.db.numRows(); i++){
    		// Load post layout and find its components
    		vi.inflate(R.layout.postcontent, threadLayout);
    		currentPost = (RelativeLayout) findViewById(R.id.layout);
    		
    		postID = Helper.db.getID(i);
    		
    		currentPost.setId(Integer.parseInt(postID.trim()));
    		postTitle = (TextView) currentPost.findViewById(R.id.title);
    		postDate = (TextView) currentPost.findViewById(R.id.date);
    		postText = (TextView) currentPost.findViewById(R.id.text);
    		
    		// Populate with the post's data
    		postTitle.setText(Helper.db.getValue(i, "name"));
    		postDate.setText("Posted by " + Helper.db.getValue(i, "username") + " on " + Helper.db.getValue(i, "created_on"));
    		postText.setText(Helper.db.getValue(i, "message"));
    		
       		currentPost.setOnClickListener(new OnClickListener() {
	            @Override
	            public void onClick(View v) { 
	            	viewPost(v);
	            }
	        });    		
    		
        	// Record whether user can edit this post
    		canEdit = Helper.isUserAdmin || (Helper.userID > 0 && 
    					Helper.userID == Integer.parseInt(Helper.db.getValue(i, "posted_by")));
        	permissions.put(new Integer(postID), canEdit);
    		
    		registerForContextMenu(currentPost);
		}
    	
    	Button addButton = (Button) findViewById(R.id.add);
    	
    	if (Helper.userID == 0) {
    		// Only allow users to add new posts if they're logged in
    		addButton.setVisibility(View.GONE);
    	} else {
    		addButton.setId(0);
	        addButton.setOnClickListener(new OnClickListener() {
	            @Override
	            public void onClick(View v) {   	
	            	editPost(v);
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
    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {  
    super.onCreateContextMenu(menu, v, menuInfo);  
    	menu.setHeaderTitle(((TextView) v.findViewById(R.id.title)).getText()); 
        menu.add(0, v.getId(), 0, R.string.view);
        menu.add(0,v.getId(), 0, R.string.share);
        
        if (permissions.get(v.getId())) {
	        menu.add(0, v.getId(), 0, R.string.edit);
	        menu.add(0, v.getId(), 0, R.string.delete);
        }
    }  
    
    /**
     * Create a context menu with options appropriate the user's permissions
     */
    @Override  
    public boolean onContextItemSelected(MenuItem item) {  
        if (item.getTitle()==getString(R.string.view)) {viewPost(findViewById(item.getItemId()));}  
        else if (item.getTitle()==getString(R.string.edit)) {editPost(findViewById(item.getItemId()));}
        else if (item.getTitle()==getString(R.string.delete)) {deletePost(item.getItemId());}  
        else if (item.getTitle()==getString(R.string.share)) {sharePost(item.getItemId());}  
        else {return false;}  
        return true;  
    }  
    
    /**
     * Open a post in the post activity
     * @param v View with a post's id
     */
    private void viewPost(View v) {  
    	Intent viewIntent = new Intent(View_Thread.this, View_Post.class);
    	Bundle postData = new Bundle();
    	postData.putString("ID", Integer.toString(v.getId()));
    	postData.putString("Thread_ID", currentThreadID);
    	viewIntent.putExtras(postData);
    	View_Thread.this.startActivityForResult(viewIntent,Helper.EDIT_REQUEST_CODE);
    }
    
    /**
     * Open a post for editing
     * @param v View with a post's id
     */
    private void editPost(View v) {
    	Intent viewIntent = new Intent(View_Thread.this, Edit_Post.class);
    	Bundle threadData = new Bundle();
    	threadData.putString("ID", Integer.toString(v.getId()));
    	threadData.putString("Thread_ID", currentThreadID);
    	viewIntent.putExtras(threadData);
    	View_Thread.this.startActivityForResult(viewIntent,Helper.EDIT_REQUEST_CODE);
    }
    
    /**
     * Share the title text to the post with any app that handles ACTION_SEND (e.g. twitter, email, IM)
     * @param id ID of the post to take the text from
     */
    private void sharePost(int id) {
    	Intent i = new Intent(android.content.Intent.ACTION_SEND);
    	
    	i.setType("text/plain");
    	i.putExtra(Intent.EXTRA_SUBJECT, R.string.forum_post);
    	i.putExtra(Intent.EXTRA_TEXT, ((TextView) findViewById(id).findViewById(R.id.title)).getText().toString());
    	
    	startActivity(Intent.createChooser(i, getString(R.string.share)));
    }
    
    /**
     * If a post has been edited, the editing activity will return a result code
     * of RESULT_OK so we refresh the screen.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	// Refresh if the name of a post in this thread could have changed
    	if (resultCode == RESULT_OK) {
    		viewThread();
    	}
    }
  
    /**
     * Delete a post from the database
     * @param id ID of the post to delete
     */
    private void deletePost(int id) {  
		final String clickedButton = Integer.toString(id);
		final String clickedPostText = ((TextView) findViewById(id).findViewById(R.id.title)).getText().toString();
		
		new AlertDialog.Builder(View_Thread.this)
			.setTitle(R.string.del_post)
			.setMessage(R.string.confirm_del_post)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					Helper.makeToast(clickedPostText + getString(R.string.deleted), getApplicationContext());
					
	            	Helper.db.deletePost(clickedButton);
					viewThread();
				}
			})
			.setNegativeButton(R.string.no, null)
			.show();
	}
    
    /**
     * Delete the thread currently being viewed and close the activity,
     * taking us back to the main list of threads
     */
    private void deleteThread() {  
		new AlertDialog.Builder(View_Thread.this)
			.setTitle(R.string.del_thread)
			.setMessage(R.string.confirm_del_thread)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Helper.makeToast(currentThreadName + getString(R.string.deleted), getApplicationContext());
					
	            	Helper.db.deleteThread(currentThreadID);
					viewThread();
					
					setResult(RESULT_OK);
					finish();
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
					viewThread();
					Helper.makeToast(getString(R.string.screen_refreshed), getApplicationContext());
					
				} else if ("login".equals(action)) {
					// Only login if user is logged out
					if (Helper.userID == 0) 
						startActivityForResult(new Intent(View_Thread.this, Login.class), Helper.LOGIN_REQUEST_CODE);
					
				} else if ("logout".equals(action)) {
					// Only logout if they're logged in
					if (Helper.userID > 0) {
						Helper.logout(getApplicationContext());
			        	viewThread();
					}
					
				} else if ("delete".equals(action)) {
					// Only allow thread deletion if this is their thread or they're an admin
					if (Helper.isUserAdmin || currentThreadUserID == Helper.userID) deleteThread();
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
    		
    		// Enable delete button if user is an admin or created this thread
    		if (Helper.isUserAdmin || currentThreadUserID == Helper.userID)
    			menu.findItem(R.id.delete).setVisible(true);
    		
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
        	viewThread();
            return true;
            
        case R.id.login:
        	startActivityForResult(new Intent(this, Login.class), Helper.LOGIN_REQUEST_CODE);
        	return true;
        	
        case R.id.logout:
        	Helper.logout(getApplicationContext());
        	viewThread();
        	return true;
        	
        case R.id.delete:
        	deleteThread();
        	return true;
        	
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}