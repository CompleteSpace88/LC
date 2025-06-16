package com.bitcoinminer.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * Network Manager for Bitcoin Miner App
 * Handles offline/online connectivity and network stability
 */
public class NetworkManager {
    
    private static final String TAG = "NetworkManager";
    
    private Context context;
    private ConnectivityManager connectivityManager;
    private NetworkCallback networkCallback;
    private BroadcastReceiver networkReceiver;
    private List<NetworkStateListener> listeners;
    
    // Network state tracking
    private boolean isOnline = false;
    private boolean isWiFiConnected = false;
    private boolean isMobileConnected = false;
    private NetworkType currentNetworkType = NetworkType.NONE;
    private long lastConnectedTime = 0;
    private int reconnectionAttempts = 0;
    
    public enum NetworkType {
        NONE, WIFI, MOBILE, ETHERNET, VPN
    }
    
    public enum ConnectionQuality {
        EXCELLENT, GOOD, FAIR, POOR, NO_CONNECTION
    }
    
    public interface NetworkStateListener {
        void onNetworkStateChanged(boolean isOnline, NetworkType networkType);
        void onConnectionQualityChanged(ConnectionQuality quality);
        void onNetworkError(String error);
    }
    
    public NetworkManager(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.listeners = new ArrayList<>();
        
        initializeNetworkMonitoring();
    }
    
    /**
     * Initialize network monitoring based on Android version
     */
    private void initializeNetworkMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Modern approach for API 24+
            setupNetworkCallback();
        } else {
            // Legacy approach for older Android versions
            setupBroadcastReceiver();
        }
        
        // Initial network state check
        updateNetworkState();
    }
    
    /**
     * Setup network callback for modern Android versions
     */
    private void setupNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new NetworkCallback();
            
            NetworkRequest.Builder builder = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
        }
    }
    
    /**
     * Setup broadcast receiver for legacy Android versions
     */
    private void setupBroadcastReceiver() {
        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                    updateNetworkState();
                }
            }
        };
        
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(networkReceiver, filter);
    }
    
    /**
     * Network callback for modern Android versions
     */
    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(Network network) {
            Log.d(TAG, "Network available: " + network);
            updateNetworkState();
            reconnectionAttempts = 0;
        }
        
        @Override
        public void onLost(Network network) {
            Log.d(TAG, "Network lost: " + network);
            updateNetworkState();
        }
        
        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            Log.d(TAG, "Network capabilities changed");
            updateNetworkState();
        }
    }
    
    /**
     * Update current network state
     */
    private void updateNetworkState() {
        boolean wasOnline = isOnline;
        NetworkType previousType = currentNetworkType;
        
        // Reset connection flags
        isOnline = false;
        isWiFiConnected = false;
        isMobileConnected = false;
        currentNetworkType = NetworkType.NONE;
        
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Modern approach
                Network activeNetwork = connectivityManager.getActiveNetwork();
                if (activeNetwork != null) {
                    NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                    if (capabilities != null) {
                        isOnline = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                  capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                        
                        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            isWiFiConnected = true;
                            currentNetworkType = NetworkType.WIFI;
                        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            isMobileConnected = true;
                            currentNetworkType = NetworkType.MOBILE;
                        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                            currentNetworkType = NetworkType.ETHERNET;
                        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                            currentNetworkType = NetworkType.VPN;
                        }
                    }
                }
            } else {
                // Legacy approach
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo != null) {
                    isOnline = activeNetworkInfo.isConnected();
                    
                    switch (activeNetworkInfo.getType()) {
                        case ConnectivityManager.TYPE_WIFI:
                            isWiFiConnected = true;
                            currentNetworkType = NetworkType.WIFI;
                            break;
                        case ConnectivityManager.TYPE_MOBILE:
                            isMobileConnected = true;
                            currentNetworkType = NetworkType.MOBILE;
                            break;
                        case ConnectivityManager.TYPE_ETHERNET:
                            currentNetworkType = NetworkType.ETHERNET;
                            break;
                        case ConnectivityManager.TYPE_VPN:
                            currentNetworkType = NetworkType.VPN;
                            break;
                    }
                }
            }
        }
        
        // Update last connected time
        if (isOnline) {
            lastConnectedTime = System.currentTimeMillis();
        }
        
        // Notify listeners if state changed
        if (wasOnline != isOnline || previousType != currentNetworkType) {
            notifyNetworkStateChanged();
        }
        
        // Update connection quality
        updateConnectionQuality();
        
        Log.d(TAG, String.format("Network State - Online: %s, Type: %s, WiFi: %s, Mobile: %s", 
            isOnline, currentNetworkType, isWiFiConnected, isMobileConnected));
    }
    
    /**
     * Assess and update connection quality
     */
    private void updateConnectionQuality() {
        ConnectionQuality quality = ConnectionQuality.NO_CONNECTION;
        
        if (isOnline) {
            if (isWiFiConnected) {
                quality = ConnectionQuality.EXCELLENT;
            } else if (isMobileConnected) {
                // Assess mobile connection quality based on network type
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Network activeNetwork = connectivityManager.getActiveNetwork();
                    if (activeNetwork != null) {
                        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                        if (capabilities != null) {
                            int linkDownstreamBandwidthKbps = capabilities.getLinkDownstreamBandwidthKbps();
                            if (linkDownstreamBandwidthKbps > 10000) { // > 10 Mbps
                                quality = ConnectionQuality.GOOD;
                            } else if (linkDownstreamBandwidthKbps > 1000) { // > 1 Mbps
                                quality = ConnectionQuality.FAIR;
                            } else {
                                quality = ConnectionQuality.POOR;
                            }
                        } else {
                            quality = ConnectionQuality.FAIR; // Default for mobile
                        }
                    }
                } else {
                    quality = ConnectionQuality.FAIR; // Default for older Android
                }
            } else {
                quality = ConnectionQuality.GOOD; // Ethernet/VPN
            }
        }
        
        notifyConnectionQualityChanged(quality);
    }
    
    /**
     * Get current network status
     */
    public NetworkStatus getNetworkStatus() {
        return new NetworkStatus(isOnline, currentNetworkType, isWiFiConnected, 
                                isMobileConnected, lastConnectedTime, reconnectionAttempts);
    }
    
    /**
     * Check if device is online
     */
    public boolean isOnline() {
        return isOnline;
    }
    
    /**
     * Check if WiFi is connected
     */
    public boolean isWiFiConnected() {
        return isWiFiConnected;
    }
    
    /**
     * Check if mobile data is connected
     */
    public boolean isMobileConnected() {
        return isMobileConnected;
    }
    
    /**
     * Get current network type
     */
    public NetworkType getCurrentNetworkType() {
        return currentNetworkType;
    }
    
    /**
     * Get time since last connection (in milliseconds)
     */
    public long getTimeSinceLastConnection() {
        if (isOnline) {
            return 0;
        }
        return System.currentTimeMillis() - lastConnectedTime;
    }
    
    /**
     * Attempt to reconnect to network
     */
    public void attemptReconnection() {
        reconnectionAttempts++;
        Log.d(TAG, "Attempting reconnection #" + reconnectionAttempts);
        
        // Force network state update
        updateNetworkState();
        
        if (!isOnline) {
            notifyNetworkError("Reconnection attempt #" + reconnectionAttempts + " failed");
        }
    }
    
    /**
     * Add network state listener
     */
    public void addNetworkStateListener(NetworkStateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Remove network state listener
     */
    public void removeNetworkStateListener(NetworkStateListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notify listeners of network state change
     */
    private void notifyNetworkStateChanged() {
        for (NetworkStateListener listener : listeners) {
            try {
                listener.onNetworkStateChanged(isOnline, currentNetworkType);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying network state listener", e);
            }
        }
    }
    
    /**
     * Notify listeners of connection quality change
     */
    private void notifyConnectionQualityChanged(ConnectionQuality quality) {
        for (NetworkStateListener listener : listeners) {
            try {
                listener.onConnectionQualityChanged(quality);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying connection quality listener", e);
            }
        }
    }
    
    /**
     * Notify listeners of network error
     */
    private void notifyNetworkError(String error) {
        for (NetworkStateListener listener : listeners) {
            try {
                listener.onNetworkError(error);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying network error listener", e);
            }
        }
    }
    
    /**
     * Clean up resources
     */
    public void destroy() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
            
            if (networkReceiver != null) {
                context.unregisterReceiver(networkReceiver);
            }
            
            listeners.clear();
            
        } catch (Exception e) {
            Log.e(TAG, "Error during NetworkManager cleanup", e);
        }
    }
    
    /**
     * Network status data class
     */
    public static class NetworkStatus {
        public final boolean isOnline;
        public final NetworkType networkType;
        public final boolean isWiFiConnected;
        public final boolean isMobileConnected;
        public final long lastConnectedTime;
        public final int reconnectionAttempts;
        
        public NetworkStatus(boolean isOnline, NetworkType networkType, boolean isWiFiConnected,
                           boolean isMobileConnected, long lastConnectedTime, int reconnectionAttempts) {
            this.isOnline = isOnline;
            this.networkType = networkType;
            this.isWiFiConnected = isWiFiConnected;
            this.isMobileConnected = isMobileConnected;
            this.lastConnectedTime = lastConnectedTime;
            this.reconnectionAttempts = reconnectionAttempts;
        }
        
        @Override
        public String toString() {
            return String.format("NetworkStatus{online=%s, type=%s, wifi=%s, mobile=%s, attempts=%d}", 
                isOnline, networkType, isWiFiConnected, isMobileConnected, reconnectionAttempts);
        }
    }
}

