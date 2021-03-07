package io.github.waikato_ufdl.ui.settings;

import android.content.Context;

import com.github.waikatoufdl.ufdl4j.auth.Authentication;
import com.github.waikatoufdl.ufdl4j.auth.TokenStorageHandler;
import com.github.waikatoufdl.ufdl4j.auth.Tokens;
import com.github.waikatoufdl.ufdl4j.core.AbstractLoggingObject;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import io.github.waikato_ufdl.SessionManager;

/***
 *  A tokenStorageHandler class to handle the storage and retrieval of the access and refresh tokens which will be used in API calls.
 */

public class tokenStorageHandler extends AbstractLoggingObject implements TokenStorageHandler {
    private final SessionManager sessionManager;
    protected Map<String, Tokens> tokensMap;

    /***
     * Constructor for the token storage handler
     * @param context the context
     */
    public tokenStorageHandler(Context context) {
        sessionManager = new SessionManager(context);
    }

    /***
     * loads tokens from local storage or retrieves tokens
     * @param context the context
     * @return Tokens
     */
    @Override
    public Tokens load(Authentication context) {
        String url;
        url = context.getServer().getURL();

        //use Utility class to load any stored tokens from local storage
        tokensMap = sessionManager.loadTokens();

        //if no tokens have been loaded from local storage then create a new token map
        if (tokensMap == null)
            tokensMap = new HashMap<>();

        //if the token map is empty, retrieve tokens
        if (!tokensMap.containsKey(url)) {
            getLogger().info("Creating empty tokens for: " + url);
            tokensMap.put(url, new Tokens());
        }

        return tokensMap.get(url);
    }

    /***
     * A method to store tokens
     * @param context the context
     * @param tokens the tokens to store
     * @return null if successfully stored, otherwise error message
     */
    @Override
    public String store(Authentication context, Tokens tokens) {
        String url;

        if (tokens.isValid()) {
            url = context.getServer().getURL();
            tokensMap = sessionManager.loadTokens();

            if (tokensMap == null)
                tokensMap = new HashMap<>();

            getLogger().info("Storing tokens for: " + url);
            tokensMap.put(url, tokens);
            sessionManager.storeTokens((HashMap<String, Tokens>) tokensMap);
        }

        return null;
    }

    /***
     * Method to return a short description of the state
     * @return the description of the state
     */
    @NotNull
    @Override
    public String toString() {
        return tokensMap.toString();
    }
}
