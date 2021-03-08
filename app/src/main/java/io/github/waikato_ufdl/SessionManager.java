package io.github.waikato_ufdl;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Patterns;

import com.github.waikatoufdl.ufdl4j.Client;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;
import com.github.waikatoufdl.ufdl4j.auth.Tokens;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;

import io.github.waikato_ufdl.ui.settings.tokenStorageHandler;

/***
 * a Session class that will be used to store and retrieve the current user's settings.
 */

public class SessionManager {
    public static final String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA};

    private static Client client;

    /**
     * boolean value which is used to distinguish whether the app is currently in online mode or offline mode (true : online mode)
     */
    public static boolean isOnlineMode = false;

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
    }

    private final DBManager dbManager;
    private final Context context;

    /***
     * Constructor for the session manager
     * @param context the context
     */
    public SessionManager(Context context) {
        this.context = context;
        this.dbManager = new DBManager(context);
    }

    /***
     * Gets the Database Manager
     * @return the database manager
     */
    public DBManager getDbManager() {
        return dbManager;
    }

    /***
     * This method will save the dark mode state
     * @param state : True (Dark Theme) or False (Light Theme)
     */
    public void saveDarkModeState(Boolean state) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putBoolean("DarkMode", state);
        editor.apply();
    }

    /***
     * A method to load/retrieve the saved dark mode state
     * @return : true if dark mode.
     */
    public Boolean loadDarkModeState() {
        return getPrefs(context).getBoolean("DarkMode", false);
    }

    /***
     * A method to return the theme to set
     * @return an integer representing the dark mode or light mode theme
     */
    public int getTheme() {
        if (loadDarkModeState()) {
            return R.style.DarkTheme;
        }

        //else, return light theme
        return R.style.AppTheme;
    }

    /***
     * Method to load the primary key of the user that is currently logged in.
     * @return primary key of user. Else, -1 if no user primary key has been stored.
     */
    public int getUserPK() {
        return getPrefs(context).getInt("pk", -1);
    }

    /***
     * Method to remove any active user session.
     */
    public void removeSession() {
        createSession(-1);
    }

    /***
     * Stores the user's primary key to shared preferences to act as the session ID.
     * @param pk the user's primary key
     */
    public void createSession(int pk) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putInt("pk", pk);
        editor.apply();
    }

    /***
     * Method to store the tokens to shared preferences.
     * @param tokens the tokens to store
     */
    public void storeTokens(HashMap<String, Tokens> tokens) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        Gson gson = new Gson();
        String json = gson.toJson(tokens);
        String accountDetails = loadServerURL() + loadUsername() + loadPassword();
        editor.putString(accountDetails, json);
        editor.apply();
    }

    /***
     * Method to load the tokens from shared preferences
     * @return stored tokens. Returns null if no tokens have been stored to shared preferences.
     */
    public HashMap<String, Tokens> loadTokens() {
        SharedPreferences prefs = getPrefs(context);
        Gson gson = new Gson();
        String accountDetails = loadServerURL() + loadUsername() + loadPassword();
        String storedHashMapString = prefs.getString(accountDetails, null);
        if (storedHashMapString != null) {
            java.lang.reflect.Type type = new TypeToken<HashMap<String, Tokens>>() {
            }.getType();

            return gson.fromJson(storedHashMapString, type);
        }

        return null;
    }

    /***
     * Method to check whether there is a user currently logged in
     * @return true if user is logged in
     */
    public boolean isLoggedIn() {
        return getUserPK() != -1;
    }

    /***
     * Method to connect to the UFDL backend using the user's details which are stored in the local database
     */
    public void connectToServer() {
        client = new Client(loadServerURL(), loadUsername(), loadPassword(), new tokenStorageHandler(context));
    }

    /***
     * Getter method to retrieve client
     * @return The client for communicating with the UFDL backend
     */
    public static Client getClient() {
        return client;
    }


    /***
     * Method to check whether a URL is valid
     * @param URL the URL
     * @return true if the URL is valid
     */
    public static boolean isValidURL(String URL) {
        return Patterns.WEB_URL.matcher(URL).matches();
    }

    /***
     * A method to save the user's username into shared preference
     * @param username The username to store
     */
    public void saveUsername(String username) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString("Username", username);
        editor.apply();
    }

    /***
     * A method to retrieve the user's username from local storage
     * @return stored username
     */
    public String loadUsername() {
        return getPrefs(context).getString("Username", null);
    }

    /***
     * A method to save the user's password to shared preferences
     * @param password the password to store
     */
    public void savePassword(String password) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString("Password", password);
        editor.apply();
    }

    /***
     * A method to retrieve a stored password from shared preferences
     * @return the user's stored password
     */
    public String loadPassword() {
        return getPrefs(context).getString("Password", null);
    }


    /***
     * A method to save the server's URL to shared preferences
     * @param URL the server URL
     */
    public void saveServerURL(String URL) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString("URL", URL);
        editor.apply();
    }

    /***
     * A method to retrieve the server's URL from sharedPreference
     * @return the server URL stored in local storage
     */
    public String loadServerURL() {
        return getPrefs(context).getString("URL", null);
    }

    public ImageClassificationDatasets getDatasetAction()
    {
        try {
            return SessionManager.getClient().action(ImageClassificationDatasets.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
