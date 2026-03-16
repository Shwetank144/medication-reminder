package com.eyedrop.reminder;

import android.app.KeyguardManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class AlarmActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private ToneGenerator toneGen;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Vibrator vibrator;
    private String medName;
    private boolean stopped = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );
        }

        setContentView(R.layout.activity_alarm);

        medName = getIntent().getStringExtra("med_name");
        String medSub = getIntent().getStringExtra("med_sub");
        if (medName == null) medName = "Eye Drop";
        if (medSub == null) medSub = "1 drop · Right Eye";

        TextView titleView = findViewById(R.id.alarmTitle);
        TextView subView = findViewById(R.id.alarmSub);
        Button dismissBtn = findViewById(R.id.alarmDismiss);

        titleView.setText(medName);
        subView.setText(medSub + "\nRight Eye");

        dismissBtn.setOnClickListener(v -> dismiss());

        // Init TTS
        tts = new TextToSpeech(this, this);

        // Start alarm sound
        playAlarmChime();

        // Start vibration
        startVibration();

        // Auto-dismiss after 60 seconds
        handler.postDelayed(this::dismiss, 60000);
    }

    private void playAlarmChime() {
        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

            // Play chime pattern 3 times
            int[] tones = {ToneGenerator.TONE_DTMF_A, ToneGenerator.TONE_DTMF_B, ToneGenerator.TONE_DTMF_D};
            for (int rep = 0; rep < 3; rep++) {
                for (int t = 0; t < tones.length; t++) {
                    int delay = rep * 800 + t * 200;
                    int tone = tones[t];
                    handler.postDelayed(() -> {
                        if (!stopped && toneGen != null) {
                            try { toneGen.startTone(tone, 400); } catch (Exception e) {}
                        }
                    }, delay);
                }
            }
        } catch (Exception e) {
            // Tone generation failed, vibration will still work
        }
    }

    private void startVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
            if (vm != null) vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 500, 200, 500, 200, 500, 1000, 500, 200, 500, 200, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
            tts.setSpeechRate(0.9f);

            // Speak after chime finishes (about 2.5 seconds)
            handler.postDelayed(() -> {
                if (!stopped && tts != null) {
                    String speech = "Time for " + medName + ". One drop. Right eye.";
                    tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "alarm_speak");

                    // Repeat once more after 5 seconds
                    handler.postDelayed(() -> {
                        if (!stopped && tts != null) {
                            tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "alarm_speak2");
                        }
                    }, 5000);
                }
            }, 2500);
        }
    }

    private void dismiss() {
        stopped = true;
        handler.removeCallbacksAndMessages(null);

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        if (toneGen != null) {
            toneGen.release();
            toneGen = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }

        finish();
    }

    @Override
    protected void onDestroy() {
        dismiss();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        dismiss();
    }
}
