package com.telenor.possumgather.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.telenor.possumgather.PossumGather;

public class MainActivity extends AppCompatActivity {
    private PossumGather possumGather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        possumGather = new PossumGather(this, "brynjeAndroid");
    }

    public void gatherToggle(View view) {
        if (possumGather.isListening()) {
            possumGather.stopListening();
        } else {
            if (possumGather.hasMissingPermissions(this)) {
                possumGather.requestNeededPermissions(this);
            } else {
                possumGather.startListening();
            }
        }
    }

    public void sendData(View view) {
        if (possumGather.isListening()) {
            possumGather.stopListening();
        }
        possumGather.upload(this, getString(R.string.identityPoolId), getString(R.string.bucket));
    }

    public void deleteLocal(View view) {
        possumGather.deleteStored(this);
    }
}