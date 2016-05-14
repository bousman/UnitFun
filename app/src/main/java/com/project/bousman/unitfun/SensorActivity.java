package com.project.bousman.unitfun;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.List;


public class SensorActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mSensor;

    public TextView mRandomView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        // up/back in the toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.app_bar);
        myToolbar.setTitle("Random Draw");
        setSupportActionBar(myToolbar);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }

/*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        }); */
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRandomView = (TextView)findViewById(R.id.uxSensorText);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Log.d("mSensor=", mSensor.getName());

        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        for (Sensor sensor : deviceSensors){
            Log.d("Sensor:" + sensor.getStringType(), sensor.getName());
        }
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        Log.e("onAccuracyChanged", "");
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        Log.e("onSensorChanged", "");

        String str = "";
        for (float v : event.values){
            str = str + Float.toString(v) + " : ";
        }

        mRandomView.setText(str);
    }

    @Override
    protected void onResume() {
        Log.d("registerListener", "");
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        Log.d("unregisterListener", "");
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
}
