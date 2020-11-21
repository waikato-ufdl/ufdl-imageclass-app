package io.github.waikato_ufdl.ui.settings;
import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Patterns;

import io.github.waikato_ufdl.DBManager;

import io.github.waikato_ufdl.R;

import io.github.waikato_ufdl.R;
import io.github.waikato_ufdl.ui.images.ClassifiedImage;
import com.github.waikatoufdl.ufdl4j.Client;
import com.github.waikatoufdl.ufdl4j.action.Licenses;
import com.github.waikatoufdl.ufdl4j.action.Projects;
import com.github.waikatoufdl.ufdl4j.auth.Authentication;
import com.github.waikatoufdl.ufdl4j.auth.MemoryOnlyStorage;
import com.github.waikatoufdl.ufdl4j.auth.TokenStorageHandler;
import com.github.waikatoufdl.ufdl4j.auth.Tokens;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

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

        return new ArrayList<>();
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

    public static void storeTokens(HashMap tokens)
    {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        Gson gson = new Gson();
        String json = gson.toJson(tokens);
        String accountDetails = loadServerURL()+"*"+loadUsername()+"*"+loadPassword();
        editor.putString(accountDetails,json);
        editor.apply();
    }

    public static HashMap<String,Tokens>  loadTokens()
    {
        SharedPreferences prefs = getPrefs(context);
        Gson gson = new Gson();
        //get from shared prefs in gson and convert to maps again
        String storedHashMapString = prefs.getString("tokens",null);
        String accountDetails = loadServerURL()+"*"+loadUsername()+"*"+loadPassword();
        if(storedHashMapString!=null) {
            java.lang.reflect.Type type = new TypeToken<HashMap<String, Tokens>>() {
            }.getType();

            //Get the hashMap
            HashMap<String, Tokens> map = gson.fromJson(storedHashMapString, type);
            return map;
        }

        return null;
    }

    /**
     * Method to connect to the UFDL backend using the user's details which are stored in settings.  Need to also provide a tokenStorageHandler to
     * handle the storage and retrieval of the access and refresh tokens which will be used in API calls.
     */
    public static void connectToServer() {
        System.out.println((loadServerURL() + " " + loadPassword() + " " + loadPassword()));
        client = new Client(loadServerURL(), loadUsername(), loadPassword(), new tokenStorageHandler());


        ExecutorService clientConnection = Executors.newSingleThreadExecutor();

        clientConnection.execute(() -> {
            try {
                DBManager dbManager = new DBManager(context);
                System.out.println("\nLicenses:");
                for (Licenses.License license : client.licenses().list()) {
                    System.out.println(license);
                    Log.d("connectToServer: ", "INSERTING LICENSE INTO SQLITE");
                    dbManager.insertLicenses(license.getPK(), license.getName());
                }

                System.out.println("\nProjects:");
                for (Projects.Project project : client.projects().list()) {
                    System.out.println(project);
                    Log.d("connectToServer: ", "INSERTING PROJECT INTO SQLITE");
                    dbManager.insertProjects(project.getPK(), project.getName());
                }

            } catch (IllegalStateException e) {

                Log.d("connectToServer: ", "Please check your username, password and server URL details in settings");
            } catch (Exception e) {

            }
        });

        clientConnection.shutdown();
    }

    /**
     * Getter method to retrieve client
     * @return
     */
    public static Client getClient()
    {
        return client;
    }

    /**
     * Method to check whether a URL is valid
     * @param URL
     * @return
     */
    public static boolean isValidURL(String URL)
    {
        return Patterns.WEB_URL.matcher(URL).matches();
    }

    /**
     * Method to check there is a connection established between the device and the server by trying to retrieve data via an API request
     * @return
     */
    public static boolean isConnected(){
        // create the shared boolean variable
        final AtomicBoolean b = new AtomicBoolean();

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    boolean val = (client.licenses().list().size() > 0) ? true : false;
                    b.set(val);
                } catch (Exception ex) {
                    b.set(false);
                }
            }
        });
        t.start();

        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //return the boolean value
        return b.get();
    }
}
