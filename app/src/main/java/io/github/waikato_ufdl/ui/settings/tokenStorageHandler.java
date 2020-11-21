package io.github.waikato_ufdl.ui.settings;

import com.github.waikatoufdl.ufdl4j.auth.Authentication;
import com.github.waikatoufdl.ufdl4j.auth.TokenStorageHandler;
import com.github.waikatoufdl.ufdl4j.auth.Tokens;
import com.github.waikatoufdl.ufdl4j.core.AbstractLoggingObject;

import java.util.HashMap;
import java.util.Map;


public class tokenStorageHandler extends AbstractLoggingObject implements TokenStorageHandler {
    protected Map<String, Tokens> tokensMap;

    /**
     * loads tokens from local storage or retrieves tokens
     *
     * @param context
     * @return
     */
    @Override
    public Tokens load(Authentication context) {
        String url;

        url = context.getServer().getURL();

        //use Utility class to load any stored tokens from local storage
        tokensMap = Utility.loadTokens();

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

    @Override
    public String store(Authentication context, Tokens tokens) {
        String url;

        if (tokens.isValid()) {
            url = context.getServer().getURL();

            tokensMap = Utility.loadTokens();

            if (tokensMap == null)
                tokensMap = new HashMap<>();

            getLogger().info("Storing tokens for: " + url);
            tokensMap.put(url, tokens);
            System.out.println("HELLLOOOO");
            Utility.storeTokens((HashMap) tokensMap);
        }

        return null;
    }

    @Override
    public String toString() {
        return tokensMap.toString();
    }
}
