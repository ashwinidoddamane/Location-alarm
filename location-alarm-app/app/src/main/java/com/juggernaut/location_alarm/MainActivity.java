package com.juggernaut.location_alarm;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button button1;
    private Button button2;
    private Button button3;
    private Button button4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the buttons by their IDs
        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        button4 = findViewById(R.id.button4);

        // Set click listeners for the buttons
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
        button4.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent;

        switch (view.getId()) {
            case R.id.button1:
                // Handle button 1 click
                intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
                break;

            case R.id.button2:
                // Handle button 2 click
                intent = new Intent(MainActivity.this, SetAlarmTuneActivity.class);
                startActivity(intent);
                break;

            case R.id.button3:
                // Handle button 3 click
                intent = new Intent(MainActivity.this, About.class);
                startActivity(intent);
                break;

            case R.id.button4:
                // Handle button 4 click
                finish();
                System.exit(0);
                break;
        }
    }
}
