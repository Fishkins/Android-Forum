package com.Android_Forum.Utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.Android_Forum.R;
import com.Android_Forum.Interfaces.DatabaseHelper;
			
public class Helper {
	// Whether to use local DB or web server
	private static final boolean USE_REMOTE_DB = false;
	
	// Request codes to be used in StartActivityForResult
	public static final int LOGIN_REQUEST_CODE = 100;
	public static final int EDIT_REQUEST_CODE = 101;
	public static final int RENAME_REQUEST_CODE = 102;
	
	public static int userID = 0;
	public static boolean isUserAdmin = false;
	
	// Set up database helper with URLs to send JSON to
	public static DatabaseHelper db;
			
	/**
	 * Initialize the database helper
	 * @param context Application context
	 */
	public static void initDBHelper(Context context) {
		if (db == null)
			db = (USE_REMOTE_DB ? RemoteDatabaseHelper.getInstance() : LocalDatabaseHelper.getInstance(context));
	}
	
	/**
	 * Display text as a toast
	 * @param text Test to display
	 * @param context Application context
	 */
    public static void makeToast(CharSequence text, Context context) {
    	makeToast(text, context, Toast.LENGTH_SHORT);
    }
    
	/**
	 * Display text as a toast
	 * @param text Test to display
	 * @param context Application context
	 * @param duration Toast duration
	 */
    public static void makeToast(CharSequence text, Context context, int duration) {
    	Toast toast = Toast.makeText(context, text, duration);
		toast.show();
    }
	
    /**
     * md5 encrypt a string
     * @param s String to encrypt
     * @return Encrypted string
     */
    public static String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i < messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
    
	/**
	 * Create a file with the given text. Will overwrite contents of file if it already exists.
	 * @param fileName File name.
	 * @param fileText Text to put in file.
	 * @param ctx Application context.
	 */
    public static void writeFile(String fileName, String fileText, Context ctx) {
    	try {
			FileOutputStream fos = ctx.openFileOutput(fileName, Context.MODE_PRIVATE);
			fos.write(fileText.getBytes());
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
	/**
	 * Open a file with name fileName and return the contents
	 * @param fileName File name
	 * @param ctx Application context
	 * @return File contents
	 */
	public static String readFile(String fileName, Context ctx) {
    	byte[] fileText = new byte[10];
    	int bytesRead = 0;
    	String fileOutput = "";
    	
    	try {
			FileInputStream fos = ctx.openFileInput(fileName);
			bytesRead = fos.read(fileText);
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for (int i=0; i < bytesRead; i++) {
			fileOutput += (char) fileText[i];
		}
		
		return fileOutput;
    }
    
    /**
     * Check if user has persistent login info.
     * If they do, consult with the database to make sure it's still valid.
     * @param ctx Application context
     * @return userID if the login is valid, else return 0
     */
    public static int autoLogin(Context ctx) {
    	if (userID > 0)
    		return userID;
    	
        String loginID = Helper.readFile("UserID", ctx);
        
        if (loginID.length() > 0) {
        	Helper.db.confirmUser(loginID);
        	
        	if (Helper.db.getValue("username").length() > 0) {
        		// Validation successful. Store the userID and whether they're an admin locally
        		isUserAdmin = Helper.db.getValue("is_admin").equalsIgnoreCase("1");
	        	userID = Integer.parseInt(loginID);
	        	makeToast(ctx.getString(R.string.logged_in) + Helper.db.getValue("username"), ctx);
	        	
        	} else {
        		// Invalid userID. Clear it from the file and notify user.
        		logout(ctx);
        		makeToast(ctx.getString(R.string.invalid_stored_login), ctx);
        	}
        }
        return userID;
    }
	
    public static void logout(Context ctx) {
    	logout(ctx, true);
    }
    
    /**
     * Logs the user out by erasing their ID from the variable and the file
     * @param Application context
     */
    public static void logout(Context ctx, boolean showToast) {
    	userID = 0;
    	isUserAdmin = false;
    	writeFile("UserID", "", ctx);
    	
    	if (showToast)
    		makeToast(ctx.getString(R.string.logged_out), ctx);
    }
    
    /**
     * Checks whether the phone has some type of internet connection established and returns a boolean
     * @param Application context
     */
	public static boolean isOnline(Context ctx) {
	    ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    return (netInfo != null && netInfo.isConnected());
	}
}