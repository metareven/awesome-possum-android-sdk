package com.telenor.possumauth.example;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.telenor.possumauth.PossumAuth;

public class MainActivity extends AppCompatActivity {
    private PossumAuth possumAuth;
    private static final String tag = MainActivity.class.getName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Log.i(tag, "AP: Architecture:"+System.getProperty("os.arch"));
        possumAuth = new PossumAuth(this, "brynjeAndroid", "uploadUrl");
    }

    public void stopProcess(View view) {
        if (possumAuth.isListening()) {
            possumAuth.stopListening();
            Log.i(tag, "AP: Stopping listening");
        } else {
            if (possumAuth.hasMissingPermissions(this)) {
                possumAuth.requestNeededPermissions(this);
            } else {
                possumAuth.startListening();
                Log.i(tag, "AP: Starting listening");
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(tag, "AP:Config changed");
        possumAuth.onConfigurationChanged(newConfig);
    }

    @Override
    public void onResume() {
        super.onResume();
        possumAuth.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        possumAuth.onPause();
    }
}