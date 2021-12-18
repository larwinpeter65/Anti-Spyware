package com.fyp.smartdoorapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.fyp.smartdoorapp.R;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.fyp.smartdoorapp.activities.MainActivity;
import com.fyp.smartdoorapp.scanners.AppScanner;

public class RealTimeService extends Service {
    private BroadcastReceiver receiver;

    @Override
    public void onCreate() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addDataScheme("package");
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        String CHANNEL_ID = "channel_100";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_NAME = "SPYWARE SCANNER";
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(mChannel);
            notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                    .setOngoing(true)
                    .setContentTitle("Spyware Scanner")
                    .setContentText(getApplicationContext().getString(R.string.real_time_scan_on));
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            notificationBuilder.setContentIntent(contentIntent);
            Notification notification = notificationBuilder.build();
            int NOTIFICATION_ID = 200;
            startForeground(NOTIFICATION_ID, notification);
        }
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences sharedPreferences;
                sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                if (sharedPreferences.getBoolean("realtime", true)) {
                    String packageName;
                    if (intent.getAction() != null) {
                        if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED") && intent.getDataString() != null) {
                            packageName = intent.getDataString().replace("package:", "");
                            final AppScanner scanner = new AppScanner(context, packageName);
                            scanner.execute();
                        }
                    }
                }
            }
        };
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
