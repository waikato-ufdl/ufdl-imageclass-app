package com.example.myapplication.ui.settings;
import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;

import com.example.myapplication.R;
import com.example.myapplication.ui.images.ClassifiedImage;

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

    public static ArrayList<ClassifiedImage> getImageList(Integer key)
    {
        if(imagesCollection.containsKey(key)) {
            return imagesCollection.get(key);
        }

        return null;
    }
}
