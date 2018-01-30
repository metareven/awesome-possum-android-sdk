package com.telenor.possumauth.example;

import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;

import com.telenor.possumauth.PossumAuth;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private PossumAuth possumAuth;
    private SurfaceView surfaceView;
    private Camera camera;
    private static final String tag = MainActivity.class.getName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.camera_preview);
        possumAuth = new PossumAuth(this, "brynjeAndroid", getString(R.string.authentication_url));
    }

    public void toggleProcess(View view) {
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
        Log.i(tag, "AP: Config changed");
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

    public void toggleCam(View view) {
        if (camera == null) {
            try {
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                camera.setPreviewDisplay(surfaceView.getHolder());
                camera.setDisplayOrientation(90);
                camera.startPreview();
            } catch (IOException e) {
                Log.i(tag, "AP: Failed to preview camera:",e);
            } catch (RuntimeException e) {
                Log.i(tag, "AP: Failed to open camera");
            }
        } else {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }
}