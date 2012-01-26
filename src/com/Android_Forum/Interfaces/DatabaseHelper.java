package com.Android_Forum.Interfaces;

import java.util.HashMap;

public interface DatabaseHelper {
	/**
	 * Load all threads into the app.
	 */
	public void loadThreads();
	
	/**
	 * Load the requested thread.
	 * @param threadID The thread ID.
	 */
	public void loadThread(String threadID); 
		
	/**
	 * Load the requested post.
	 * @param postID The post ID. 
	 */
	public void loadPost(String postID); 
		
	/**
	 * Check user credentials against the DB.
	 * @param userName UserName
	 * @param password password
	 */
	public void logUser(String userName, String password); 
		
	/**
	 * Confirms that userID is valid and gets username and admin status.
	 * @param userID User's ID.
	 */
	public void confirmUser(String userID); 
		
 	/**
	 * Access post/thread data.
	 * @param key The name of some data in the post/thread.
	 * @return Corresponding value in the post/thread.
	 */
	public String getValue(String key); 
	
	/**
	 * Access post/thread data.
	 * @param index Index of the post/thread in the data
	 * @param key The name of some data in the post/thread.
	 * @return Value for the given post/thread
	 */ 
	public String getValue(int index, String key); 
		
	/**
	 * Get number of posts/threads currently loaded.
	 * @return The number of posts/threads.
	 */
	public int numRows(); 
	
	/**
	 * Access post/thread ID by index.
	 * @param index Numerical index of a post/thread.
	 * @return The ID.
	 */
	public String getID(int index); 

	/**
	 * Delete a thread.
	 * @param threadID ID of the thread to be deleted.
	 */
	public void deleteThread(String threadID);
	
	/**
	 * Create a new thread.
	 * @param data Data to populate the thread. 
	 */
	public void addThread(HashMap<String, String> data);
	
	/**
	 * Change name of a thread.
	 * @param threadID ID of thread to rename.
	 * @param data Map of name attribute to the new name.
	 */
	public void renameThread(String threadID, HashMap<String, String> data);
	
	/**
	 * Delete a post.
	 * @param postID ID of post to delete.
	 */
	public void deletePost(String postID);
	
	/**
	 * Create a post.
	 * @param data Data to populate post.
	 */
	public void addPost(HashMap<String, String> data);
	
	/**
	 * Modify post data.
	 * @param postID ID of post to modify.
	 * @param data New post information.
	 */
	public void editPost(String postID, HashMap<String, String> data);
}
