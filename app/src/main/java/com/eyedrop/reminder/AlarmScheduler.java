package com.eyedrop.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;
import java.util.List;

public class AlarmScheduler {

    private static final int BASE_REQUEST_CODE = 5000;

    public static void scheduleAll(Context ctx) {
        cancelAll(ctx);

        if (!ScheduleManager.isAlarmsOn(ctx)) return;

        List<ScheduleManager.Drop> drops = ScheduleManager.getTodayDrops(ctx);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Calendar now = Calendar.getInstance();
        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        for (int i = 0; i < drops.size(); i++) {
            ScheduleManager.Drop drop = drops.get(i);

            // Only schedule future drops
            if (drop.timeMinutes <= nowMin) continue;

            Calendar alarmTime = Calendar.getInstance();
            alarmTime.set(Calendar.HOUR_OF_DAY, drop.getHour());
            alarmTime.set(Calendar.MINUTE, drop.getMinute());
            alarmTime.set(Calendar.SECOND, 0);
            alarmTime.set(Calendar.MILLISECOND, 0);

            Intent intent = new Intent(ctx, AlarmReceiver.class);
            intent.putExtra("med_name", drop.medName);
            intent.putExtra("med_sub", drop.medSub);
            intent.putExtra("med_index", drop.medIndex);
            intent.putExtra("drop_index", i);

            PendingIntent pi = PendingIntent.getBroadcast(
                ctx, BASE_REQUEST_CODE + i, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (am.canScheduleExactAlarms()) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pi);
                    } else {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pi);
                    }
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pi);
                }
            } catch (SecurityException e) {
                // Fall back to inexact alarm
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pi);
            }
        }
    }

    public static void cancelAll(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        // Cancel up to 30 possible alarms
        for (int i = 0; i < 30; i++) {
            Intent intent = new Intent(ctx, AlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(
                ctx, BASE_REQUEST_CODE + i, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pi != null) {
                am.cancel(pi);
                pi.cancel();
            }
        }
    }
}
