package com.Android_Forum.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import android.util.Log;

import com.Android_Forum.Interfaces.DatabaseHelper;

public class RemoteDatabaseHelper implements DatabaseHelper {
	private static final String requestHeader = Helper.md5("Fishkins' Forum");
	private static final String webDomain = "";	// Needs to be set depending on the server running
	private static RemoteDatabaseHelper mInstance;
	private String mGetURL;
	private String mSetURL;
	private JSONObject JsonObj;
	private ArrayList<String> KeyArr;
	
	/**
	 * Construct a database helper.
	 */
	private RemoteDatabaseHelper(){
		mGetURL = "http://" + webDomain + "/forums/get/"; 
		mSetURL = "http://" + webDomain + "/forums/post/";
	}
	
	/**
	 * Get an instance of this class.
	 * @return {@link RemoteDatabaseHelper} instance
	 */
	public static RemoteDatabaseHelper getInstance(){
		if (mInstance == null)
			mInstance = new RemoteDatabaseHelper();
		
		return mInstance;
	}
	
	/**
	 * Load all threads into the app.
	 */
	@Override
	public void loadThreads() {
		getURLData("?loadType=Threads");
	}
	
	/**
	 * Load the requested thread.
	 * @param threadID The thread ID.
	 */
	@Override
	public void loadThread(String threadID) {
		getURLData("?loadType=Thread&ID=" + threadID);
	}
	
	/**
	 * Load the requested post.
	 * @param postID The post ID. 
	 */
	@Override
	public void loadPost(String postID) {
		getURLData("?loadType=Post&ID=" + postID);
	}
	
	/**
	 * Check user credentials against the DB.
	 * @param userName UserName
	 * @param password password
	 */
	@Override
	public void logUser(String userName, String password) {
		getURLData("?loadType=User&UserName=" + userName + "&Password=" + password);
	}
	
	/**
	 * Confirms that userID is valid and gets username and admin status.
	 * @param userID User's ID.
	 */
	@Override
	public void confirmUser(String userID) {
		getURLData("?loadType=ConfirmUser&UserID=" + userID);
	}
	
 	/**
	 * Access post/thread data.
	 * @param key The name of some data in the post/thread.
	 * @return Corresponding value in the post/thread.
	 */
	@Override
	public String getValue(String key) {
		try {
			return (String) JsonObj.get(key);
		} catch (JSONException e) {
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * Access post/thread data.
	 * @param index Index of the post/thread in the data
	 * @param key The name of some data in the post/thread.
	 * @return Value for the given post/thread
	 */ 
	@Override
	public String getValue(int index, String key) {
		try {
			return (String) ((JSONObject) JsonObj.get(getID(index))).get(key);
		} catch (JSONException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	/**
	 * Get number of posts/threads currently loaded.
	 * @return The number of posts/threads.
	 */
	@Override
	public int numRows() {
		if (KeyArr != null) {
			return KeyArr.size();
		} else {
			return -1;
		}
	}
	
	/**
	 * Access post/thread ID by index.
	 * @param index Numerical index of a post/thread.
	 * @return The ID.
	 */
	@Override
	public String getID(int index) {
		return KeyArr.get(index);
	}
	
	/**
	 * @return string of DatabaseHelper's data
	 */
	@Override
	public String toString(){
		String obj = "GetURL: " + mGetURL + "\n";
			obj += "SetURL: " + mSetURL + "\n\n";
			obj += "Data:\n" + JsonObj.toString() + "\n";
		
		return obj;
	}
	
	/**************************************** Web Server Commands ****************************************/
	
	/**
	 * Convert is to a string and return the string.
	 * @param is InputStream to be convert to a String.
	 * @return is as a String.
	 */
    private static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
 
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }	
	
	/**
	 * Retrieve JSON data from mGetURL
	 * @param params The GET parameters to pass to the webpage
	 */
	private void getURLData(String params){
		HttpClient httpclient = new DefaultHttpClient();
		
        // Prepare a request object
        HttpGet httpget = new HttpGet(mGetURL + params); 
        httpget.setHeader("User-Agent",requestHeader);
 
        // Execute the request
        HttpResponse response;
        try {
            response = httpclient.execute(httpget);
        	// Examine the response status
            Log.i("DatabaseHelper",response.getStatusLine().toString());
 
            // Get hold of the response entity
            HttpEntity entity = response.getEntity();
            
            // If the response does not enclose an entity, there is no need
            // to worry about connection release
            if (entity != null) {
            	 
                // A Simple JSON Response Read
                InputStream instream = entity.getContent();
                String result= convertStreamToString(instream);
                
                // Try to parse as key/value pairs.
                try {
                	JsonObj = new JSONObject(result);
                	
                	JSONArray jsonKeys = JsonObj.names();
                	KeyArr = new ArrayList<String>();
                	
                	if (jsonKeys != null) {
	                	for (int i=0; i<jsonKeys.length(); i++) {
	                		KeyArr.add(jsonKeys.getString(i));
	                	}
	                	
	                	try {
	                		// Test if strings can be parsed as ints
	                		Collections.sort(KeyArr, new StringAsIntComparator());
	                	} catch (NumberFormatException nfe) {
	                		// No need to sort
	                	}
                	}
                	
                } catch (JSONException e) {
                }
                
                // Closing the input stream will trigger connection release
                instream.close();
            }
        }catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	/**
	 * Comparator to sort list of strings based on descending integer value.
	 */
	private class StringAsIntComparator implements Comparator<String> {
		/*
		 * Convert the given strings to integers and compare them,
		 * returning 1/0/-1 if object1 is less/equal/great than object2
		 * This will sort the strings in descending integer order
		 */
		@Override
		public int compare(String object1, String object2) {
			int int1 = Integer.parseInt(object1);
			int int2 = Integer.parseInt(object2);
			
			return (int1 == int2 ? 0 : (int1 < int2 ? 1 : -1));
		}
	}

	/**
	 * Delete a thread.
	 * @param threadID ID of the thread to be deleted.
	 */
	@Override
	public void deleteThread(String threadID) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("Type", "ThreadDel");
		data.put("id", threadID);
		sendData(data);
	}

	/**
	 * Create a new thread.
	 * @param data Data to populate the thread. 
	 */
	@Override
	public void addThread(HashMap<String, String> data) {
		data.put("Type", "Thread");
		sendData(data);
	}

	/**
	 * Change name of a thread.
	 * @param threadID ID of thread to rename.
	 * @param data Map of name attribute to the new name.
	 */
	@Override
	public void renameThread(String threadID, HashMap<String, String> data) {
		data.put("Type", "ThreadRename");
		data.put("id", threadID);
		sendData(data);
	}
	
	/**
	 * Delete a post.
	 * @param postID ID of post to delete.
	 */
	@Override
	public void deletePost(String postID) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("Type", "PostDel");
		data.put("id", postID);
		sendData(data);
	}

	/**
	 * Create a post.
	 * @param data Data to populate post.
	 */
	@Override
	public void addPost(HashMap<String, String> data) {
		data.put("Type", "Post");
		sendData(data);
	}

	/**
	 * Modify post data.
	 * @param postID ID of post to modify.
	 * @param data New post information.
	 */
	@Override
	public void editPost(String postID, HashMap<String, String> data) {
		data.put("Type", "PostEdit");
		data.put("id", postID);		
		sendData(data);
	}
	
	/**
	 * Send data to SetURL. 
	 * @param data key/value pairs of data to be sent to the server as json
	 */
	private void sendData(HashMap<String, String> data) {
		HttpPost request = new HttpPost(mSetURL);
		String key;
		JSONStringer json = new JSONStringer();

		try {
			json.object();
			Iterator<String> e = data.keySet().iterator();

			while (e.hasNext()) {
				key = (String) e.next();
				json.key(key.replace("_", "")).value(data.get(key));
			}
			json.endObject();
		} catch (JSONException e1) {
			e1.printStackTrace();
		}

		StringEntity entity = null;
		try {
			entity = new StringEntity(json.toString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		entity.setContentType("application/json; charset=UTF-8"); //text/plain;charset=UTF-8
		entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,"application/json; charset=UTF-8"));
		request.setEntity(entity);
		request.setHeader("User-Agent",requestHeader);
		
		DefaultHttpClient httpClient = new DefaultHttpClient();

		try {
			HttpResponse temp = httpClient.execute(request);
			byte[] what = new byte[3000];
			temp.getEntity().getContent().read(what);
			String thing = new String(what);
			thing += "";
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
