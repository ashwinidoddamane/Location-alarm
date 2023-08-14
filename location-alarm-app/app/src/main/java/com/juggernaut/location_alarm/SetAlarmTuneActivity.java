package com.juggernaut.location_alarm;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SetAlarmTuneActivity extends Activity {
    private static final int REQUEST_CODE_SELECT_TUNE = 1;
    private static Uri selectedCustomTuneUri;

    private Button btnSelectTune;
    private TextView tvSelectedTune;
    private Button btnSaveTune;
    private Ringtone playingRingtone;

    public static Uri getSelectedCustomTuneUri() {
        return selectedCustomTuneUri;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_alarm_tune);

        btnSelectTune = findViewById(R.id.btnSelectTune);
        tvSelectedTune = findViewById(R.id.tvSelectedTune);
        btnSaveTune = findViewById(R.id.btnSaveTune);

        btnSelectTune.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectAlarmTune();
            }
        });

        btnSaveTune.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAlarmTune();
            }
        });
    }

    private void selectAlarmTune() {
        if (playingRingtone != null && playingRingtone.isPlaying()) {
            playingRingtone.stop();
        }

        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tune");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedCustomTuneUri);
        startActivityForResult(intent, REQUEST_CODE_SELECT_TUNE);
    }


    private void startAlarm(Uri alarmTuneUri) {
        try {
            if (playingRingtone != null && playingRingtone.isPlaying()) {
                playingRingtone.stop();
            }

            playingRingtone = RingtoneManager.getRingtone(this, alarmTuneUri);
            if (playingRingtone != null) {
                playingRingtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error playing alarm tune", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAlarmTune() {
        if (selectedCustomTuneUri != null) {
            String ringtoneTitle = getRingtoneTitle(selectedCustomTuneUri);
            tvSelectedTune.setText("Selected Tune: " + ringtoneTitle);
            Toast.makeText(this, "Custom tune saved: " + selectedCustomTuneUri.toString(), Toast.LENGTH_SHORT).show();

            // Pass selectedCustomTuneUri to startAlarm()
            startAlarm(selectedCustomTuneUri);
        } else {
            tvSelectedTune.setText("Selected Tune: None");
            Toast.makeText(this, "No tune selected", Toast.LENGTH_SHORT).show();
        }
    }

    private String getRingtoneTitle(Uri ringtoneUri) {
        Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
        if (ringtone != null) {
            return ringtone.getTitle(this);
        }
        return "";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_TUNE && resultCode == RESULT_OK) {
            selectedCustomTuneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (selectedCustomTuneUri != null) {
                String ringtoneTitle = getRingtoneTitle(selectedCustomTuneUri);
                tvSelectedTune.setText("Selected Tune: " + ringtoneTitle);
            } else {
                tvSelectedTune.setText("Selected Tune: None");
                Toast.makeText(this, "Invalid tune selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playingRingtone != null && playingRingtone.isPlaying()) {
            playingRingtone.stop();
        }
    }
}

