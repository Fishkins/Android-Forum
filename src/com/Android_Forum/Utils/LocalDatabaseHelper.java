package com.Android_Forum.Utils;

import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.Android_Forum.Interfaces.DatabaseHelper;

public class LocalDatabaseHelper implements DatabaseHelper {
	private static final int VERSION = 1;
	private static final String FORUMS_DATABASE_NAME = "Forums";
	private static final String USERS_TABLE_NAME = "Users";
	private static final String THREADS_TABLE_NAME = "Threads";
	private static final String POSTS_TABLE_NAME = "Posts";
	
	private static final String SQL_CREATE_USERS = 
		"CREATE TABLE " + USERS_TABLE_NAME +
		" (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
		" username TEXT NOT NULL," +
		" password TEXT NOT NULL," +
		" is_admin INTEGER NOT NULL);";
	
	private static final String SQL_CREATE_THREADS = 
		"CREATE TABLE " + THREADS_TABLE_NAME +
		" (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
		" name TEXT NOT NULL," +
		" created_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
		" posted_by INTEGER NOT NULL," +
		" FOREIGN KEY(posted_by) REFERENCES " + USERS_TABLE_NAME + "(id));";
	
	private static final String SQL_CREATE_POSTS = 
		"CREATE TABLE " + POSTS_TABLE_NAME +
		" (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
		" name TEXT NOT NULL," +
		" message TEXT NOT NULL," +
		" created_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
		" posted_by INTEGER NOT NULL," +
		" thread_id INTEGER NOT NULL," +
		" FOREIGN KEY(posted_by) REFERENCES " + USERS_TABLE_NAME + "(id)," + 
		" FOREIGN KEY(thread_id) REFERENCES " + THREADS_TABLE_NAME + "(id));";
	
	private static LocalDatabaseHelper mInstance;
	private SQLiteDatabase db; 
	private Cursor mData;
	private boolean hasData = false;
		
	/**
	 * Construct a database helper and create/retrieve the database
	 */
	private LocalDatabaseHelper(Context context){
		db = new OpenHelper(context).getWritableDatabase();
	}
	
	/**
	 * Get an instance of this class.
	 * @return {@link LocalDatabaseHelper} instance
	 */
	public static LocalDatabaseHelper getInstance(Context context){
		if (mInstance == null)
			mInstance = new LocalDatabaseHelper(context);
		
		return mInstance;
	}

	/**
	 * Load the data of all threads
	 */
	@Override
	public void loadThreads() {
		String[] cols = {"id", "name", "posted_by"};
		mData = db.query(THREADS_TABLE_NAME, cols, null, null, null, null, "created_on DESC");
		hasData = mData.getCount() > 0;
	}

	/**
	 * Load posts for a specific thread
	 */
	@Override
	public void loadThread(String threadID) {
		String sql = "SELECT P.*, U.username FROM " + POSTS_TABLE_NAME + " P INNER JOIN " + 
				USERS_TABLE_NAME + " U ON P.posted_by = U.id WHERE P.thread_id = ? " +
				"ORDER BY created_on DESC"; 
		String[] args = {threadID};
		mData = db.rawQuery(sql, args);
		hasData = mData.getCount() > 0;
	}

	
	/**
	 * Load the requested post.
	 * @param postID The post ID. 
	 */
	@Override
	public void loadPost(String postID) {
		String sql = "SELECT P.*, U.username FROM " + POSTS_TABLE_NAME + " P INNER JOIN " + 
				USERS_TABLE_NAME + " U ON P.posted_by = U.id WHERE P.id = ?"; 
		String[] args = {String.valueOf(postID)};
		mData = db.rawQuery(sql, args);
		
		if (mData.getCount() > 0) {
			hasData = true;
			mData.moveToFirst();
		} else {
			hasData = false;
		}
	}

	/**
	 * Check user credentials against the DB.
	 * @param userName UserName
	 * @param password password
	 */
	@Override
	public void logUser(String userName, String password) {
		String[] cols = {"id", "is_admin"};
		String[] args = {userName.toLowerCase(), Helper.md5(password)};
		mData = db.query(USERS_TABLE_NAME, cols, "username = ? AND password = ?", args, null, null, null);
		
		if (mData.getCount() > 0) {
			mData.moveToFirst();
			hasData = true;
		} else {
			hasData = false;
		}
	}

	/**
	 * Confirms that userID is valid and gets username and admin status.
	 * @param userID User's ID.
	 */
	@Override
	public void confirmUser(String userID) {
		String[] cols = {"username", "is_admin"};
		String[] args = {userID};
		mData = db.query(USERS_TABLE_NAME, cols, "id = ?", args, null, null, null);		
		
		if (mData.getCount() > 0) {
			mData.moveToFirst();
			hasData = true;
		} else {
			hasData = false;
		}
	}

 	/**
	 * Access post/thread data.
	 * @param key The name of some data in the post/thread.
	 * @return Corresponding value in the post/thread.
	 */
	@Override
	public String getValue(String key) {
		if (!hasData) return "";
		
		int col = mData.getColumnIndex(key);
		
		if (col < 0) return "";
		
		return mData.getString(col);
	}

	/**
	 * Access post/thread data.
	 * @param index Index of the post/thread in the data
	 * @param key The name of some data in the post/thread.
	 * @return Value for the given post/thread
	 */ 
	@Override
	public String getValue(int index, String key) {
		if (!hasData || !mData.moveToPosition(index)) 
			return "";
		
		return getValue(key);
	}

	/**
	 * Get number of posts/threads currently loaded.
	 * @return The number of posts/threads.
	 */
	@Override
	public int numRows() {
		return mData.getCount();
	}

	/**
	 * Access post/thread ID by index.
	 * @param index Numerical index of a post/thread.
	 * @return The ID.
	 */
	@Override
	public String getID(int index) {
		return getValue(index, "id");
	}

	/**
	 * Delete a thread.
	 * @param threadID ID of the thread to be deleted.
	 */
	@Override
	public void deleteThread(String threadID) {
		db.delete(THREADS_TABLE_NAME, "id = ?", new String[] {threadID});
	}

	/**
	 * Create a new thread.
	 * @param data Data to populate the thread. 
	 */
	@Override
	public void addThread(HashMap<String, String> data) {
		db.insert(THREADS_TABLE_NAME, null, hashToContentValues(data));
	}

	/**
	 * Change name of a thread.
	 * @param threadID ID of thread to rename.
	 * @param data Map of name attribute to the new name.
	 */
	@Override
	public void renameThread(String threadID, HashMap<String, String> data) {
		db.update(THREADS_TABLE_NAME, hashToContentValues(data), "id = ?", new String[] {threadID});
	}
	
	/**
	 * Delete a post.
	 * @param postID ID of post to delete.
	 */
	@Override
	public void deletePost(String postID) {
		db.delete(POSTS_TABLE_NAME, "id = ?", new String[] {postID});
	}
	
	/**
	 * Create a post.
	 * @param data Data to populate post.
	 */
	@Override
	public void addPost(HashMap<String, String> data) {
		db.insert(POSTS_TABLE_NAME, null, hashToContentValues(data));
	}

	/**
	 * Modify post data.
	 * @param postID ID of post to modify.
	 * @param data New post information.
	 */
	@Override
	public void editPost(String postID, HashMap<String, String> data) {
		db.update(POSTS_TABLE_NAME, hashToContentValues(data), "id = ?", new String[] {postID});
	}

	/**
	 * Transfer key/value pairs from a {@link HashMap} to {@link ContentValues}
	 * @param data {@link HashMap}containing the data.
	 * @return {@link ContentValues} containing the data.
	 */
	private ContentValues hashToContentValues(HashMap<String, String> data) {
		ContentValues values = new ContentValues();
		
		for (Map.Entry<String, String> entry : data.entrySet()) {
			values.put(entry.getKey(), entry.getValue());
		}
		
		return values;
	}
	
	/**
	 * Implementation of {@link SQLiteOpenHelper} to create 
	 * tables for our forum.
	 * @author cjudkins
	 */
	private static class OpenHelper extends SQLiteOpenHelper {
		OpenHelper(Context context) {
			super(context, FORUMS_DATABASE_NAME, null, VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE_USERS);
			db.execSQL(SQL_CREATE_THREADS);
			db.execSQL(SQL_CREATE_POSTS);
			
			// Create default admin
			ContentValues values = new ContentValues();
			values.put("is_admin", 1);
			values.put("username", "admin");
			values.put("password", Helper.md5("admin"));
			db.insert(USERS_TABLE_NAME, null, values);
			
			// Create default user
			values = new ContentValues();
			values.put("is_admin", 0);
			values.put("username", "user");
			values.put("password", Helper.md5("user"));
			db.insert(USERS_TABLE_NAME, null, values);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Drop and recreate the db to upgrade
			db.execSQL("DROP TABLE IF EXISTS " + POSTS_TABLE_NAME + ";");
			db.execSQL("DROP TABLE IF EXISTS " + THREADS_TABLE_NAME + ";");
			db.execSQL("DROP TABLE IF EXISTS " + USERS_TABLE_NAME + ";");
					
			onCreate(db);			
		}
	}
}