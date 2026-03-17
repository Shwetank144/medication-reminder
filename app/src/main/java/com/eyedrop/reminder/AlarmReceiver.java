package com.eyedrop.reminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "eyedrop_alarm";

    @Override
    public void onReceive(Context context, Intent intent) {
        String medName = intent.getStringExtra("med_name");
        String medSub = intent.getStringExtra("med_sub");
        int medIndex = intent.getIntExtra("med_index", 0);

        if (medName == null) medName = "Eye Drop";
        if (medSub == null) medSub = "1 drop · Right Eye";

        // Wake up the screen
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
            "eyedrop:alarm"
        );
        wl.acquire(30000); // 30 seconds

        // Show notification with fullScreenIntent — this is the correct way to launch
        // an activity from background on Android 10+. Android will auto-launch the
        // AlarmActivity as a full-screen overlay (like an incoming call screen).
        createNotificationChannel(context);
        showNotification(context, medName, medSub, medIndex);

        wl.release();
    }

    private void createNotificationChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                ctx.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(ctx.getString(R.string.channel_desc));
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});

            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void showNotification(Context ctx, String medName, String medSub, int medIndex) {
        // Full-screen intent: launches AlarmActivity as an overlay (works on locked/sleeping screen)
        Intent alarmIntent = new Intent(ctx, AlarmActivity.class);
        alarmIntent.putExtra("med_name", medName);
        alarmIntent.putExtra("med_sub", medSub);
        alarmIntent.putExtra("med_index", medIndex);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent fullScreenPi = PendingIntent.getActivity(
            ctx, (int)(System.currentTimeMillis() % 10000), alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Tap intent: opens main app if user taps the notification banner
        Intent tapIntent = new Intent(ctx, MainActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingTap = PendingIntent.getActivity(
            ctx, 0, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("💧 " + medName)
            .setContentText(medSub)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingTap)
            .setFullScreenIntent(fullScreenPi, true)   // ← this pops up AlarmActivity
            .setVibrate(new long[]{0, 500, 200, 500});

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify((int)(System.currentTimeMillis() % 10000), builder.build());
        }
    }
}
