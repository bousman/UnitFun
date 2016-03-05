package com.project.bousman.unitfun;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;


public class SplashActivity extends AppCompatActivity implements SoundPool.OnLoadCompleteListener {

    private final long  SPLASH_DISPLAY_LENGTH = 1000;
    private final float ROTATION_ANGLE = 360;
    private final float SCALE_FACTOR = 4;

    // for sound effect on splash load
    private int soundId1;
    private static SoundPool soundPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);    // Removes title bar
        //this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);    // Removes notification bar

        setContentView(R.layout.activity_splash);
        TextView titleView = (TextView)findViewById(R.id.title_text);
        titleView.setAlpha(0);

        // check shared preferences to see if sound should be played
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (settings.getBoolean(getString(R.string.pref_sound), false)) {
            soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
            soundId1 = soundPool.load(this, R.raw.beep_sound, 1);
            soundPool.setOnLoadCompleteListener(this);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                /* Create an Intent that will start the Menu-Activity. */
                Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(mainIntent);
                finish();
            }
        }, SPLASH_DISPLAY_LENGTH);

        ImageView graphic = (ImageView)findViewById(R.id.uxGraphic);
        graphic.animate().rotation(ROTATION_ANGLE).scaleX(SCALE_FACTOR).scaleY(SCALE_FACTOR).setDuration(SPLASH_DISPLAY_LENGTH).start();
        titleView.animate().alpha(1).setDuration(SPLASH_DISPLAY_LENGTH).start();


    }


    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        final float volume = 0.2f;
        soundPool.play(sampleId, volume, volume, 0, 0, 1);
    }
}
