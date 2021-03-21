package io.github.waikato_ufdl;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

/**
 *  A class to monitor network connectivity through live data
 *  Boolean of True indicates that the device is connected to the internet
 */

public class NetworkConnectivityMonitor extends LiveData<Boolean> {

    /** The application context*/
    Context context;

    /** The network callback*/
    ConnectionNetworkCallback callback;


    /**
     * Constructor for NetworkConnectivityMonitory
     * @param context the application context
     */
    public NetworkConnectivityMonitor(Context context) {
        this.context = context;
    }

    /**
     * Unregister the network callback
     */
    public void unregisterDefaultNetworkCallback(){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        connectivityManager.unregisterNetworkCallback(callback);
    }

    /**
     * Register the network call back
     */
    public void registerDefaultNetworkCallback() {

        try {
            //create the connectivity manager
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            //check the connection and update the live data boolean
            postValue(checkConnection(connectivityManager));

            //create a network callback
            callback = new ConnectionNetworkCallback();

            //register the connectivity manager on the network callback
            connectivityManager.registerDefaultNetworkCallback(callback);
        } catch (Exception e) {
            Log.e("Exception: ", "Exception in registerDefaultNetworkCallback");
            postValue(false);
        }
    }

    /**
     * Method to check the network connection
     * @param connectivityManager the connectivity manager
     * @return true if there is a network connection
     */
    private boolean checkConnection(@NonNull ConnectivityManager connectivityManager){
        Network network = connectivityManager.getActiveNetwork();

        //no active network so return false
        if (network == null){
            return false;
        }
        else{
            //there is an active network so return true
            NetworkCapabilities activeNetwork = connectivityManager.getNetworkCapabilities(network);
            return activeNetwork != null
                    && (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }
    }

    private class ConnectionNetworkCallback extends ConnectivityManager.NetworkCallback{

        /**
         * Device is connected to the network
         * @param network the active network
         */
        @Override
        public void onAvailable(@NonNull android.net.Network network) {
            super.onAvailable(network);

            //set the live data value to true
            postValue(true);
            Log.d("Connection:", "onAvailable");
        }

        /**
         * Device is not connected to the network
         * @param network the active network
         */
        @Override
        public void onLost(@NonNull android.net.Network network) {
            super.onLost(network);

            //set the live data value to false
            postValue(false);
            Log.d("Connection:", "onLost");
        }

        /**
         * called when access to the specific network is blocked/unblocked
         * @param network the active network
         * @param blocked true if blocked
         */
        @Override
        public void onBlockedStatusChanged(@NonNull Network network, boolean blocked) {
            super.onBlockedStatusChanged(network, blocked);
            Log.d("Connection:", "onBlockedStatusChanged");
        }

        /**
         * called when the network corresponding to this request changes capabilities but still satisfies the requested criteria
         * @param network the active network
         * @param networkCapabilities the capabilities of an active network
         */
        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            Log.d("Connection:", "onCapabilitiesChanged");
        }

        /**
         * called when the network corresponding to this request changes link properties
         * @param network the active network
         * @param linkProperties the link properties of the active network
         */
        @Override
        public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties);
            Log.d("Connection:", "onLinkPropertiesChanged");
        }

        /**
         * called when the network is about to be lost
         * @param network the active network
         * @param maxMsToLive time remaining till network is lost
         */
        @Override
        public void onLosing(@NonNull Network network, int maxMsToLive) {
            super.onLosing(network, maxMsToLive);
            Log.d("Connection:", "onLosing");
        }

        /**
         * called if no network is found or if the network request cannot be fulfilled
         */
        @Override
        public void onUnavailable() {
            super.onUnavailable();
            Log.d("Connection:", "onUnavailable");
        }
    }

    @Override
    protected void onActive() {
        super.onActive();
        registerDefaultNetworkCallback();
        Log.d("ConnectionManager: ", "Registered");
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        unregisterDefaultNetworkCallback();
        Log.d("ConnectionManager: ", "Unregistered");
    }
}