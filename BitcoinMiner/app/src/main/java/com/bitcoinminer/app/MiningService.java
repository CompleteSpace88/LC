package com.bitcoinminer.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

public class MiningService extends Service {
    
    private static final String CHANNEL_ID = "MiningServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    
    private PowerManager.WakeLock wakeLock;
    private NotificationManager notificationManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Create notification channel for Android 8.0+
        createNotificationChannel();
        
        // Acquire wake lock to keep mining active
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BitcoinMiner::MiningWakeLock");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            // Validate intent data
            String walletAddress = "";
            if (intent != null && intent.hasExtra("wallet_address")) {
                walletAddress = CodeValidator.validateIntentData(
                    intent.getStringExtra("wallet_address")
                );
            }
            
            // Validate Android compatibility
            CodeValidator.validateAndroidCompatibility();
            
            // Acquire wake lock safely
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire(10*60*1000L /*10 minutes*/); // Timeout to prevent battery drain
            }
            
            // Create notification with validated data
            Notification notification = createNotification();
            startForeground(NOTIFICATION_ID, notification);
            
            return START_STICKY; // Restart service if killed
            
        } catch (Exception e) {
            // Graceful error handling
            android.util.Log.e("MiningService", "Service start error", e);
            stopSelf(); // Stop service if critical error
            return START_NOT_STICKY;
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        try {
            // Release wake lock safely
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
            
            stopForeground(true);
            
        } catch (Exception e) {
            android.util.Log.e("MiningService", "Service destroy error", e);
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Bitcoin Mining Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps Bitcoin mining active in background");
            
            notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );
        
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        
        return builder
            .setContentTitle("Bitcoin Miner Active")
            .setContentText("Mining Bitcoin in background...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }
}
