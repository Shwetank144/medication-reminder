package com.eyedrop.reminder;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ScheduleManager {

    public static class Drop {
        public String medName;
        public String medSub;
        public int timeMinutes; // minutes from midnight
        public String slotLabel;
        public int medIndex; // 0=moxi, 1=micro, 2=fomi

        public Drop(String name, String sub, int time, String label, int idx) {
            this.medName = name;
            this.medSub = sub;
            this.timeMinutes = time;
            this.slotLabel = label;
            this.medIndex = idx;
        }

        public int getHour() { return timeMinutes / 60; }
        public int getMinute() { return timeMinutes % 60; }
    }

    private static final int MED_GAP = 15; // 15 min between drops
    private static final String PREFS = "eyedrop_prefs";

    // Moxigram weekly schedule
    private static final int[] MOXI_PER_DAY = {6, 5, 4, 3, 2, 1};

    public static SharedPreferences getPrefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getStartDate(Context ctx) {
        return getPrefs(ctx).getString("start_date", "");
    }

    public static void setStartDate(Context ctx, String date) {
        getPrefs(ctx).edit().putString("start_date", date).apply();
    }

    public static int getWakeTime(Context ctx) {
        return getPrefs(ctx).getInt("wake_time", 7 * 60); // default 7:00
    }

    public static int getSleepTime(Context ctx) {
        return getPrefs(ctx).getInt("sleep_time", 22 * 60); // default 22:00
    }

    public static void setWakeTime(Context ctx, int minutes) {
        getPrefs(ctx).edit().putInt("wake_time", minutes).apply();
    }

    public static void setSleepTime(Context ctx, int minutes) {
        getPrefs(ctx).edit().putInt("sleep_time", minutes).apply();
    }

    public static boolean isAlarmsOn(Context ctx) {
        return getPrefs(ctx).getBoolean("alarms_on", true);
    }

    public static void setAlarmsOn(Context ctx, boolean on) {
        getPrefs(ctx).edit().putBoolean("alarms_on", on).apply();
    }

    /** Get current week number (1-6) based on start date. 0 = not started, >6 = moxi done */
    public static int getCurrentWeek(Context ctx) {
        String startStr = getStartDate(ctx);
        if (startStr.isEmpty()) return 0;

        long startMs = dateStringToMs(startStr);
        long nowMs = todayMs();
        int days = (int)((nowMs - startMs) / 86400000L);
        if (days < 0) return 0;
        return (days / 7) + 1;
    }

    /** Days since start */
    public static int getDaysSinceStart(Context ctx) {
        String startStr = getStartDate(ctx);
        if (startStr.isEmpty()) return -1;
        long startMs = dateStringToMs(startStr);
        long nowMs = todayMs();
        return (int)((nowMs - startMs) / 86400000L);
    }

    /** Build today's full drop schedule with 15-min staggering */
    public static List<Drop> getTodayDrops(Context ctx) {
        List<Drop> drops = new ArrayList<>();
        int week = getCurrentWeek(ctx);
        int totalDays = getDaysSinceStart(ctx);
        int wake = getWakeTime(ctx);
        int sleep = getSleepTime(ctx);

        if (totalDays < 0) return drops;

        // Determine active meds and their slot counts
        List<int[]> activeMeds = new ArrayList<>(); // {medIndex, slotCount}

        if (week >= 1 && week <= 6) {
            activeMeds.add(new int[]{0, MOXI_PER_DAY[week - 1]});
        }
        if (totalDays < 45) {
            activeMeds.add(new int[]{1, 3});
        }
        if (totalDays >= 0) {
            activeMeds.add(new int[]{2, 3});
        }

        if (activeMeds.isEmpty()) return drops;

        // Find max slots to determine base time slots
        int maxSlots = 0;
        for (int[] m : activeMeds) maxSlots = Math.max(maxSlots, m[1]);

        int[] baseSlotTimes = getBaseSlotTimes(maxSlots, wake, sleep);
        String[] baseSlotLabels = getBaseSlotLabels(maxSlots);

        // For each base time slot, add staggered drops
        for (int s = 0; s < baseSlotTimes.length; s++) {
            int offset = 0;
            for (int[] med : activeMeds) {
                // Check if this med has a drop at this slot
                int[] medTimes = getBaseSlotTimes(med[1], wake, sleep);
                boolean matches = false;
                for (int mt : medTimes) {
                    if (Math.abs(mt - baseSlotTimes[s]) < 15) {
                        matches = true;
                        break;
                    }
                }
                if (matches) {
                    String name = getMedName(med[0]);
                    String sub = getMedSub(med[0]);
                    drops.add(new Drop(name, sub, baseSlotTimes[s] + offset, baseSlotLabels[s], med[0]));
                    offset += MED_GAP;
                }
            }
        }

        return drops;
    }

    private static int[] getBaseSlotTimes(int count, int wake, int sleep) {
        int total = sleep - wake;
        int[] times = new int[count];
        if (count == 1) {
            times[0] = wake;
        } else {
            int gap = total / (count - 1);
            for (int i = 0; i < count; i++) {
                times[i] = wake + gap * i;
            }
        }
        return times;
    }

    private static String[] getBaseSlotLabels(int count) {
        switch (count) {
            case 1: return new String[]{"Morning"};
            case 2: return new String[]{"Morning", "Night"};
            case 3: return new String[]{"Morning", "Afternoon", "Night"};
            case 4: return new String[]{"Morning", "Afternoon", "Evening", "Night"};
            case 5: return new String[]{"Dose 1", "Dose 2", "Dose 3", "Dose 4", "Dose 5"};
            case 6: return new String[]{"Dose 1", "Dose 2", "Dose 3", "Dose 4", "Dose 5", "Dose 6"};
            default: {
                String[] labels = new String[count];
                for (int i = 0; i < count; i++) labels[i] = "Dose " + (i + 1);
                return labels;
            }
        }
    }

    public static String getMedName(int index) {
        switch (index) {
            case 0: return "Moxigram DM";
            case 1: return "Micronac PF";
            case 2: return "Fomisa";
            default: return "Unknown";
        }
    }

    private static String getMedSub(int index) {
        switch (index) {
            case 0: return "Antibiotic · 1 drop";
            case 1: return "Anti-inflammatory · 1 drop";
            case 2: return "Lubricant · 1 drop";
            default: return "1 drop";
        }
    }

    private static long dateStringToMs(String dateStr) {
        try {
            String[] parts = dateStr.split("-");
            Calendar cal = Calendar.getInstance();
            cal.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]), 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    private static long todayMs() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
