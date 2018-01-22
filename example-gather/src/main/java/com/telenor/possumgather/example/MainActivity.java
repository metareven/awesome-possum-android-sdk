package com.telenor.possumgather.example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.telenor.possumgather.PossumGather;

public class MainActivity extends AppCompatActivity {
    private PossumGather possumGather;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        possumGather = new PossumGather(this, "brynjeAndroid");
    }
}
