package com.project.bousman.unitfun;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
    private boolean mSensorGrabbed = false;
    private double mSensorLast = 0.;
    private double mSensorSave = 0.;

    public TextView mRandomView1;
    public TextView mRandomView2;

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

        mRandomView1 = (TextView)findViewById(R.id.uxSensorText);
        mRandomView2 = (TextView)findViewById(R.id.uxSensorText2);

        //Log.d("Sensor", "Get sensor manager");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //Log.d("Sensor", "Get list of sensors");
        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        Log.d("Sensor", "show sensor list");
        for (Sensor sensor : deviceSensors){
            Log.d("Sensor:" + sensor.getName(), sensor.toString());
        }

        float density = getResources().getDisplayMetrics().density;
        //Log.d("Sensor", "density is " + Float.toString(density));

        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);  // YES
        //Log.d("mSensor=", mSensor.getName());
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        //Log.e("onAccuracyChanged", "");
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        //Log.e("onSensorChanged", "");
/*
        String str = "";
         for (float v : event.values) {
            str = str + Float.toString(v) + " : ";
        }
*/
        mSensorLast = -event.values[0];
       // mRandomView1.setText(str);
        mRandomView2.setText(Double.toString(mSensorLast));
    }

    @Override
    protected void onResume() {
        //Log.d("registerListener", "");
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }


    @Override
    protected void onPause() {
        //Log.d("unregisterListener", "");
        super.onPause();
        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("Sensor", "onSaveInstanceState");
    }


    public void onSensorButton(View v) {
        mSensorSave = mSensorLast;
        mSensorGrabbed = true;
        Global.mNewRefValue = mSensorLast;
        Global.mRefValueSet = true;
        //Log.d("Sensor","grabbed " + mSensorLast);

        TextView text = (TextView)findViewById(R.id.uxSensorGrabbed);
        text.setText(Double.toString(mSensorLast));
    }

}
