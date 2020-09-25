package com.example.myapplication.ui.settings;
import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.myapplication.R;
import com.example.myapplication.ui.images.ClassifiedImage;
import com.github.waikatoufdl.ufdl4j.Client;
import com.github.waikatoufdl.ufdl4j.auth.MemoryOnlyStorage;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is a class that will be used to store and retrieve user settings from shared storage
 * - currently stores & retrieves the state of the theme selected by the user
 */
public class Utility {
    public static Context context = null;
    public static final String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                                Manifest.permission.CAMERA};

    private static HashMap<Integer, ArrayList<ClassifiedImage>> imagesCollection = new HashMap<>();
    private static Client client;


    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
    }

    /**
     * A method that will set the current Context
     * @param con: context of the activity
     */
    public static void setContext (Context con)
    {
        context = con;
    }

    /**
     * This method will save the dark mode state
     * @param state : True (Dark Theme) or False (Light Theme)
     */
    public static void saveDarkModeState(Boolean state)
    {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putBoolean("DarkMode", state);
        editor.commit();
    }

    /**
     * A method to load/retrieve the saved dark mode state
     * @return : a boolean value indicating whether dark mode is set to true or false
     */
    public static Boolean loadDarkModeState()
    {
        Boolean state = getPrefs(context).getBoolean("DarkMode", false);
        return state;
    }

    /**
     * A method to return the theme to set
     * @return int: a value which will indicate the theme to set
     */
    public static int getTheme()
    {
        //check to see if dark mode theme is set to true, if it is return dark theme
        if (loadDarkModeState()) {
            return R.style.DarkTheme;
        }

        //else, return light theme
        return R.style.AppTheme;
    }

    /**
     * Store an image list into the hashmap (imagesCollection)
     * @param key   : The primary key (int) of the dataset for which the image list belongs
     * @param value : An arraylist of ClassifiedImages to store
     */
    public static void saveImageList(Integer key, ArrayList<ClassifiedImage> value)
    {
        imagesCollection.put(key, value);
    }

    /**
     * Retrieve an image list from the hashmap (imagesCollection)
     * @param key : the primary key of the dataset for which the image list belongs
     * @return
     */
    public static ArrayList<ClassifiedImage> getImageList(Integer key)
    {
        //check if an image list has been stored for the particular dataset and if so, return it
        if(imagesCollection.containsKey(key)) {
            return imagesCollection.get(key);
        }

        return null;
    }

    /**
     * A method to save the user's username into shared preference
     * @param username The username to store
     */
    public static void saveUsername(String username)
    {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString("Username", username);
        editor.commit();
    }

    /**
     * A method to retrieve the user's username
     * @return
     */
    public static String loadUsername()
    {
        return getPrefs(context).getString("Username", null);
    }


    /**
     * A method to save the user's password to sharedPreferences
     * @param password the password to save
     */
    public static void savePassword(String password)
    {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString("Password", password);
        editor.commit();
    }


    /**
     * A method to retrieve a stored password from sharedPreferences
     * @return
     */
    public static String loadPassword()
    {
        return getPrefs(context).getString("Password", null);
    }


    /**
     * A method to save the server's URL to sharedPreference
     * @param URL the server URL
     */
    public static void saveServerURL(String URL)
    {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString("URL", URL);
        editor.commit();
    }


    /**
     * A method to retrieve the server's URL from sharedPreference
     * @return
     */
    public static String loadServerURL()
    {
        return getPrefs(context).getString("URL", null);
    }


    /**
     * Method to connect to the UFDL backend using the user's details which are stored in settings.  Need to also provide a tokenStorageHandler to
     * handle the storage and retrieval of the access and refresh tokens which will be used in API calls.
     */
    public static void connectToServer()
    {
        if(client == null) {
            client = new Client(loadServerURL(), loadUsername(), loadPassword(), new MemoryOnlyStorage());
        }
    }

    public static Client getClient()
    {
        return client;
    }


}
