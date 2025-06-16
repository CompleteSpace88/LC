package com.bitcoinminer.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Offline Manager for Bitcoin Miner App
 * Handles offline mining operations and data synchronization
 */
public class OfflineManager {
    
    private static final String TAG = "OfflineManager";
    private static final String PREFS_NAME = "offline_mining_data";
    private static final String KEY_OFFLINE_BALANCE = "offline_balance";
    private static final String KEY_OFFLINE_START_TIME = "offline_start_time";
    private static final String KEY_OFFLINE_DURATION = "offline_duration";
    private static final String KEY_PENDING_OPERATIONS = "pending_operations";
    
    private Context context;
    private SharedPreferences offlinePrefs;
    private Handler mainHandler;
    private ConcurrentLinkedQueue<OfflineOperation> pendingOperations;
    private List<OfflineStateListener> listeners;
    
    // Offline mining state
    private boolean isOfflineMiningActive = false;
    private double offlineBalance = 0.0;
    private long offlineStartTime = 0;
    private double offlineMiningRate = 0.00000001; // Base offline rate (1 satoshi per second)
    
    public enum OperationType {
        BALANCE_UPDATE, WITHDRAWAL_REQUEST, SETTINGS_CHANGE, MINING_START, MINING_STOP
    }
    
    public interface OfflineStateListener {
        void onOfflineMiningStateChanged(boolean isActive);
        void onOfflineBalanceUpdated(double balance);
        void onOfflineOperationQueued(OfflineOperation operation);
        void onOfflineDataSynced(int operationCount);
    }
    
    public OfflineManager(Context context) {
        this.context = context.getApplicationContext();
        this.offlinePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.pendingOperations = new ConcurrentLinkedQueue<>();
        this.listeners = new ArrayList<>();
        
        loadOfflineData();
    }
    
    /**
     * Start offline mining mode
     */
    public void startOfflineMining() {
        if (!isOfflineMiningActive) {
            isOfflineMiningActive = true;
            offlineStartTime = System.currentTimeMillis();
            
            Log.d(TAG, "Offline mining started");
            saveOfflineData();
            notifyOfflineMiningStateChanged();
            
            // Start offline mining calculation timer
            startOfflineMiningTimer();
        }
    }
    
    /**
     * Stop offline mining mode
     */
    public void stopOfflineMining() {
        if (isOfflineMiningActive) {
            // Calculate final offline earnings
            calculateOfflineEarnings();
            
            isOfflineMiningActive = false;
            offlineStartTime = 0;
            
            Log.d(TAG, "Offline mining stopped. Final balance: " + offlineBalance);
            saveOfflineData();
            notifyOfflineMiningStateChanged();
        }
    }
    
    /**
     * Start offline mining timer for continuous calculation
     */
    private void startOfflineMiningTimer() {
        if (isOfflineMiningActive) {
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isOfflineMiningActive) {
                        calculateOfflineEarnings();
                        saveOfflineData();
                        notifyOfflineBalanceUpdated();
                        
                        // Continue timer
                        startOfflineMiningTimer();
                    }
                }
            }, 1000); // Update every second
        }
    }
    
    /**
     * Calculate offline mining earnings
     */
    private void calculateOfflineEarnings() {
        if (isOfflineMiningActive && offlineStartTime > 0) {
            long currentTime = System.currentTimeMillis();
            long miningDuration = currentTime - offlineStartTime;
            
            // Calculate earnings based on time elapsed
            double secondsElapsed = miningDuration / 1000.0;
            double earnings = secondsElapsed * offlineMiningRate;
            
            // Apply offline mining efficiency (reduced rate when offline)
            double offlineEfficiency = 0.7; // 70% efficiency when offline
            earnings *= offlineEfficiency;
            
            offlineBalance += earnings;
            
            // Reset start time for next calculation
            offlineStartTime = currentTime;
            
            Log.d(TAG, String.format("Offline earnings calculated: +%.8f BTC (Total: %.8f BTC)", 
                earnings, offlineBalance));
        }
    }
    
    /**
     * Queue operation for when connection is restored
     */
    public void queueOperation(OperationType type, String data) {
        OfflineOperation operation = new OfflineOperation(type, data, System.currentTimeMillis());
        pendingOperations.offer(operation);
        
        Log.d(TAG, "Operation queued: " + operation);
        saveOfflineData();
        notifyOfflineOperationQueued(operation);
    }
    
    /**
     * Sync offline data when connection is restored
     */
    public void syncOfflineData() {
        Log.d(TAG, "Starting offline data synchronization...");
        
        int operationCount = 0;
        
        // Process pending operations
        while (!pendingOperations.isEmpty()) {
            OfflineOperation operation = pendingOperations.poll();
            if (operation != null) {
                processOfflineOperation(operation);
                operationCount++;
            }
        }
        
        // Save synchronized data
        saveOfflineData();
        
        Log.d(TAG, "Offline data sync completed. Processed " + operationCount + " operations");
        notifyOfflineDataSynced(operationCount);
    }
    
    /**
     * Process individual offline operation
     */
    private void processOfflineOperation(OfflineOperation operation) {
        try {
            switch (operation.type) {
                case BALANCE_UPDATE:
                    // Balance updates are handled automatically
                    break;
                case WITHDRAWAL_REQUEST:
                    // Queue withdrawal for processing
                    Log.d(TAG, "Processing offline withdrawal request: " + operation.data);
                    break;
                case SETTINGS_CHANGE:
                    // Apply settings changes
                    Log.d(TAG, "Processing offline settings change: " + operation.data);
                    break;
                case MINING_START:
                    // Mining start handled automatically
                    break;
                case MINING_STOP:
                    // Mining stop handled automatically
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing offline operation: " + operation, e);
        }
    }
    
    /**
     * Get offline mining status
     */
    public OfflineStatus getOfflineStatus() {
        return new OfflineStatus(isOfflineMiningActive, offlineBalance, offlineStartTime, 
                                pendingOperations.size(), getOfflineMiningDuration());
    }
    
    /**
     * Get current offline balance
     */
    public double getOfflineBalance() {
        if (isOfflineMiningActive) {
            calculateOfflineEarnings(); // Update to current time
        }
        return offlineBalance;
    }
    
    /**
     * Add offline balance to main balance
     */
    public double transferOfflineBalance() {
        double balance = getOfflineBalance();
        offlineBalance = 0.0;
        saveOfflineData();
        
        Log.d(TAG, "Transferred offline balance: " + balance + " BTC");
        notifyOfflineBalanceUpdated();
        
        return balance;
    }
    
    /**
     * Get offline mining duration in milliseconds
     */
    public long getOfflineMiningDuration() {
        if (isOfflineMiningActive && offlineStartTime > 0) {
            return System.currentTimeMillis() - offlineStartTime;
        }
        return 0;
    }
    
    /**
     * Check if offline mining is active
     */
    public boolean isOfflineMiningActive() {
        return isOfflineMiningActive;
    }
    
    /**
     * Get number of pending operations
     */
    public int getPendingOperationsCount() {
        return pendingOperations.size();
    }
    
    /**
     * Set offline mining rate
     */
    public void setOfflineMiningRate(double rate) {
        this.offlineMiningRate = Math.max(0.0, rate);
        Log.d(TAG, "Offline mining rate set to: " + offlineMiningRate);
    }
    
    /**
     * Load offline data from SharedPreferences
     */
    private void loadOfflineData() {
        try {
            offlineBalance = Double.parseDouble(offlinePrefs.getString(KEY_OFFLINE_BALANCE, "0.0"));
            offlineStartTime = offlinePrefs.getLong(KEY_OFFLINE_START_TIME, 0);
            
            // Restore offline mining state if it was active
            if (offlineStartTime > 0) {
                isOfflineMiningActive = true;
                calculateOfflineEarnings(); // Calculate earnings since last save
            }
            
            // Load pending operations (simplified - in real app would use JSON)
            String pendingOpsData = offlinePrefs.getString(KEY_PENDING_OPERATIONS, "");
            if (!pendingOpsData.isEmpty()) {
                // Parse and restore pending operations
                Log.d(TAG, "Restored pending operations: " + pendingOpsData);
            }
            
            Log.d(TAG, "Offline data loaded - Balance: " + offlineBalance + 
                      ", Active: " + isOfflineMiningActive);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading offline data", e);
            // Reset to safe defaults
            offlineBalance = 0.0;
            offlineStartTime = 0;
            isOfflineMiningActive = false;
        }
    }
    
    /**
     * Save offline data to SharedPreferences
     */
    private void saveOfflineData() {
        try {
            SharedPreferences.Editor editor = offlinePrefs.edit();
            editor.putString(KEY_OFFLINE_BALANCE, String.valueOf(offlineBalance));
            editor.putLong(KEY_OFFLINE_START_TIME, isOfflineMiningActive ? offlineStartTime : 0);
            editor.putLong(KEY_OFFLINE_DURATION, getOfflineMiningDuration());
            
            // Save pending operations count (simplified)
            editor.putString(KEY_PENDING_OPERATIONS, String.valueOf(pendingOperations.size()));
            
            editor.apply();
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving offline data", e);
        }
    }
    
    /**
     * Add offline state listener
     */
    public void addOfflineStateListener(OfflineStateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Remove offline state listener
     */
    public void removeOfflineStateListener(OfflineStateListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notify listeners of offline mining state change
     */
    private void notifyOfflineMiningStateChanged() {
        for (OfflineStateListener listener : listeners) {
            try {
                listener.onOfflineMiningStateChanged(isOfflineMiningActive);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying offline state listener", e);
            }
        }
    }
    
    /**
     * Notify listeners of offline balance update
     */
    private void notifyOfflineBalanceUpdated() {
        for (OfflineStateListener listener : listeners) {
            try {
                listener.onOfflineBalanceUpdated(offlineBalance);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying offline balance listener", e);
            }
        }
    }
    
    /**
     * Notify listeners of queued operation
     */
    private void notifyOfflineOperationQueued(OfflineOperation operation) {
        for (OfflineStateListener listener : listeners) {
            try {
                listener.onOfflineOperationQueued(operation);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying offline operation listener", e);
            }
        }
    }
    
    /**
     * Notify listeners of data sync completion
     */
    private void notifyOfflineDataSynced(int operationCount) {
        for (OfflineStateListener listener : listeners) {
            try {
                listener.onOfflineDataSynced(operationCount);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying offline sync listener", e);
            }
        }
    }
    
    /**
     * Clean up resources
     */
    public void destroy() {
        stopOfflineMining();
        listeners.clear();
        pendingOperations.clear();
    }
    
    /**
     * Offline operation data class
     */
    public static class OfflineOperation {
        public final OperationType type;
        public final String data;
        public final long timestamp;
        
        public OfflineOperation(OperationType type, String data, long timestamp) {
            this.type = type;
            this.data = data;
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("OfflineOperation{type=%s, data=%s, timestamp=%d}", 
                type, data, timestamp);
        }
    }
    
    /**
     * Offline status data class
     */
    public static class OfflineStatus {
        public final boolean isActive;
        public final double balance;
        public final long startTime;
        public final int pendingOperations;
        public final long duration;
        
        public OfflineStatus(boolean isActive, double balance, long startTime, 
                           int pendingOperations, long duration) {
            this.isActive = isActive;
            this.balance = balance;
            this.startTime = startTime;
            this.pendingOperations = pendingOperations;
            this.duration = duration;
        }
        
        @Override
        public String toString() {
            return String.format("OfflineStatus{active=%s, balance=%.8f, pending=%d, duration=%d}", 
                isActive, balance, pendingOperations, duration);
        }
    }
}

