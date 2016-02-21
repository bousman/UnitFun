package com.project.bousman.unitfun;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;


public class SplashActivity extends AppCompatActivity {

    private final long  SPLASH_DISPLAY_LENGTH = 1000;
    private final float ROTATION_ANGLE = 360;
    private final float SCALE_FACTOR = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);    // Removes title bar
        //this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);    // Removes notification bar

        setContentView(R.layout.activity_splash);
        TextView titleView = (TextView)findViewById(R.id.title_text);
        titleView.setAlpha(0);

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
}
