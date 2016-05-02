package com.project.bousman.unitfun;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.util.Iterator;

public class WikiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wiki);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.app_bar);
        myToolbar.setTitle("Wikipedia");
        setSupportActionBar(myToolbar);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }

        TextView tv = (TextView)findViewById(R.id.uxWikiText);
        String data = getIntent().getStringExtra("html");
        if (data != null)
        {
            tv.setText(Html.fromHtml(data));
        }
        else
        {
            tv.setText("Error - no results available");
        }
    }
}
