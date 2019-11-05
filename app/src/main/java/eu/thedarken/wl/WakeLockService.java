package eu.thedarken.wl;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import eu.thedarken.wl.locks.Lock;
import eu.thedarken.wl.locks.Lock.Type;
import eu.thedarken.wl.locks.LockBright;
import eu.thedarken.wl.locks.LockDim;
import eu.thedarken.wl.locks.LockFull;
import eu.thedarken.wl.locks.LockNone;
import eu.thedarken.wl.locks.LockPartial;
import eu.thedarken.wl.locks.LockWifiFull;
import eu.thedarken.wl.locks.LockWifiFullPerf;
import eu.thedarken.wl.locks.LockWifiScan;
import eu.thedarken.wl.widget.WidgetProvider;

public class WakeLockService extends Service {
    private final String TAG = WakeLockService.class.getCanonicalName();
    private Lock lock = new LockNone();
    private SharedPreferences settings;
    private boolean use_notifications = true;
    private final static int NOTIFICATION_ID = 88;
    private String current_lock;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "creating");
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        current_lock = settings.getString("current_lock", Type.NO_LOCK.name());
        setLock(Type.valueOf(current_lock));

        lock.aquire();

        use_notifications = settings.getBoolean("notifaction.enabled", true);
        if (use_notifications) {

            this.startForeground(NOTIFICATION_ID, createNotification());
        }

    }

    private Notification createNotification() {
        String notificationChannelId = "WAKELOCK SERVICE CHANNEL";

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
                notificationChannelId,
                "WakeLock Service notifications channel",
                NotificationManager.IMPORTANCE_HIGH
        );

        {
            channel.setDescription("WakeLock Service channel");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            long[] a = {100, 200, 300, 400, 500, 400, 300, 200, 400};
            channel.setVibrationPattern(a);

        }
        notificationManager.createNotificationChannel(channel);

        Intent i = new Intent(this, MainActivity.class);

        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

        Notification.Builder builder = new Notification.Builder(this, notificationChannelId);
        builder.setAutoCancel(false);
        builder.setTicker("WakeLock active!");
        builder.setContentTitle("WhatsApp Notification");
        builder.setContentText("You have a new message");
        builder.setOngoing(true);
        return builder

                .setSmallIcon(R.drawable.note)
                .setContentIntent(pi)
                //.setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
                .build();
    }

    public void setLock(Type locktype) {
        if (locktype == Type.NO_LOCK) {
            lock = new LockNone();
        } else if (locktype == Type.WIFI_MODE_SCAN_ONLY) {
            lock = new LockWifiScan(this);
        } else if (locktype == Type.WIFI_MODE_FULL) {
            lock = new LockWifiFull(this);
        } else if (locktype == Type.WIFI_MODE_FULL_HIGH_PERF) {
            lock = new LockWifiFullPerf(this);
        } else if (locktype == Type.PARTIAL_WAKE_LOCK) {
            lock = new LockPartial(this);
        } else if (locktype == Type.SCREEN_DIM_WAKE_LOCK) {
            lock = new LockDim(this);
        } else if (locktype == Type.SCREEN_BRIGHT_WAKE_LOCK) {
            lock = new LockBright(this);
        } else if (locktype == Type.FULL_WAKE_LOCK) {
            lock = new LockFull(this);
        }
        Log.i(TAG, "Aquiring " + lock.getType());

        updateWidget(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "destroying");

        Log.i(TAG, "Releasing " + lock.getType());
        lock.release();

        current_lock = settings.getString("current_lock", Type.NO_LOCK.name());
        setLock(Type.valueOf(current_lock));

        stopForeground(true);
        Log.d(TAG, "bye...");
        updateWidget(true);
    }

    public static boolean isMyServiceRunning(Context c) {
        ActivityManager manager = (ActivityManager) c.getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (WakeLockService.class.getName().equals(service.service.getClassName())) {
                Log.d(c.getClass().getCanonicalName(), "WakeLock service active");
                return true;
            }
        }
        return false;
    }

    private void updateWidget(boolean destroying) {
        Intent i = new Intent();
        i.setAction(WidgetProvider.UPDATE_WIDGET);
        i.putExtra("destroying", destroying);
        i.putExtra("locktype", lock.getShortType());
        sendBroadcast(i);
    }

}
