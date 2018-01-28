package com.telenor.possumgather.example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.telenor.possumgather.PossumGather;
import com.telenor.possumgather.interfaces.IDataUploadComplete;

public class MainActivity extends AppCompatActivity implements IDataUploadComplete {
    private PossumGather possumGather;
    private static final String tag = MainActivity.class.getName();

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
            possumGather.startListening();
        }
    }

    public void sendData(View view) {
        if (possumGather.isListening()) {
            possumGather.stopListening();
        }
//        possumGather.deleteStored(this);
        possumGather.upload(this, getString(R.string.identityPoolId), getString(R.string.bucket), this);
    }

    @Override
    public void dataUploadFailed(Exception e) {
        Log.e(tag, "AP: Failed to upload:",e);
    }

    @Override
    public void dataUploadSuccess() {
        Log.i(tag, "AP: Upload success");
    }
}
