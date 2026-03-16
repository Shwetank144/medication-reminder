package com.eyedrop.reminder;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final int NOTIF_PERM_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        ws.setAllowFileAccess(true);
        ws.setDatabaseEnabled(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        // Add JavaScript bridge
        webView.addJavascriptInterface(new NativeBridge(), "NativeBridge");

        // Load the app from assets
        webView.loadUrl("file:///android_asset/index.html");

        // Request permissions
        requestPermissions();

        // Schedule alarms if start date is set
        if (!ScheduleManager.getStartDate(this).isEmpty()) {
            AlarmScheduler.scheduleAll(this);
        }
    }

    private void requestPermissions() {
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIF_PERM_CODE);
            }
        }

        // Exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    // Some devices don't support this intent
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reschedule alarms when app comes to foreground
        if (!ScheduleManager.getStartDate(this).isEmpty()) {
            AlarmScheduler.scheduleAll(this);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /** JavaScript bridge so the web app can set alarms natively */
    public class NativeBridge {

        @JavascriptInterface
        public void setStartDate(String date) {
            ScheduleManager.setStartDate(MainActivity.this, date);
            AlarmScheduler.scheduleAll(MainActivity.this);
        }

        @JavascriptInterface
        public String getStartDate() {
            return ScheduleManager.getStartDate(MainActivity.this);
        }

        @JavascriptInterface
        public void setWakeTime(int minutes) {
            ScheduleManager.setWakeTime(MainActivity.this, minutes);
            AlarmScheduler.scheduleAll(MainActivity.this);
        }

        @JavascriptInterface
        public void setSleepTime(int minutes) {
            ScheduleManager.setSleepTime(MainActivity.this, minutes);
            AlarmScheduler.scheduleAll(MainActivity.this);
        }

        @JavascriptInterface
        public int getWakeTime() {
            return ScheduleManager.getWakeTime(MainActivity.this);
        }

        @JavascriptInterface
        public int getSleepTime() {
            return ScheduleManager.getSleepTime(MainActivity.this);
        }

        @JavascriptInterface
        public void setAlarmsOn(boolean on) {
            ScheduleManager.setAlarmsOn(MainActivity.this, on);
            if (on) {
                AlarmScheduler.scheduleAll(MainActivity.this);
            } else {
                AlarmScheduler.cancelAll(MainActivity.this);
            }
        }

        @JavascriptInterface
        public boolean isAlarmsOn() {
            return ScheduleManager.isAlarmsOn(MainActivity.this);
        }

        @JavascriptInterface
        public void rescheduleAlarms() {
            AlarmScheduler.scheduleAll(MainActivity.this);
        }

        @JavascriptInterface
        public void testAlarm() {
            new Handler(Looper.getMainLooper()).post(() -> {
                Intent intent = new Intent(MainActivity.this, AlarmActivity.class);
                intent.putExtra("med_name", "Moxigram DM");
                intent.putExtra("med_sub", "Antibiotic · 1 drop");
                intent.putExtra("med_index", 0);
                startActivity(intent);
            });
        }

        @JavascriptInterface
        public boolean isNativeApp() {
            return true;
        }
    }
}
