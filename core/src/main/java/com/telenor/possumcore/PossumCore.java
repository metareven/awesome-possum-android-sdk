package com.telenor.possumcore;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.telenor.possumcore.abstractdetectors.AbstractDetector;
import com.telenor.possumcore.constants.Constants;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The core component for gathering data
 */
public abstract class PossumCore {
    private Set<AbstractDetector> detectors;
    private AtomicBoolean isListening;
    private Handler handler;
    private ExecutorService executorService = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "PossumProcessing");
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    private long timeOut = 3000; // Default timeOut
    private static final String tag = PossumCore.class.getName();

    public PossumCore(Context context, String uniqueUserId) {
        detectors = new HashSet<>();
        handler = new Handler();
        isListening = new AtomicBoolean(false);
        addAllDetectors(context);
        for (AbstractDetector detector : detectors)
            detector.setUniqueUserId(uniqueUserId);
    }

    /**
     * Add a detector to the list
     *
     * @param detector a detector to gather/authenticate data from
     */
    protected void addDetector(AbstractDetector detector) {
        detectors.add(detector);
    }

    /**
     * Method for quickly adding the relevant detectors, must be overridden
     */
    protected abstract void addAllDetectors(Context context);

    /**
     * Starts gathering data
     *
     * @return false if no detectors available to start or one of them fails to start or already
     * listening, else true
     */
    public boolean startListening() {
        if (isListening.get() || detectors == null || detectors.size() == 0)
            return false;
        for (AbstractDetector detector : detectors) {
            executorService.submit(detector);
        }
        Log.d(tag, "AP:Start listening");
        isListening.set(true);
        if (timeOut > 0) {
            handler.postDelayed(this::stopListening, timeOut);
        }
        return true;
    }

    /**
     * Handles all changes in configuration from the app. This must be overridden in the activity
     * used and passed down to the possumCore
     * @param newConfig the new configuration it is in
     */
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(tag, "AP: New Configuration:"+newConfig.toString());
    }

    /**
     * Handles pausing of all detectors while the app is for some reason closed down - for example
     * a phone call interrupting or the user
     */
    public void onPause() {
        if (isListening()) {
            // TODO: Pause detectors
        }
    }

    /**
     * Handles an effective restart of eventual paused app due to interruption in progress
     */
    public void onResume() {
        if (isListening()) {
            // TODO: Resume detectors
        }
    }

    /**
     * Method for retrieving the needed permissions. Will not be called if no permissions are
     * missing. If an app using the sdk wants the sdk to start when permissions are granted, they
     * should override the onRequestPermissionsResult in the activity
     *
     * @param activity an android activity
     */
    public void requestNeededPermissions(@NonNull Activity activity) {
        if (hasMissingPermissions(activity)) {
            ActivityCompat.requestPermissions(activity, missingPermissions(activity).toArray(new String[]{}), Constants.PermissionsRequestCode);
        }
    }

    /**
     * The present list of dangerous permissions. Can be extended and expanded if necessary.
     *
     * @return a list of used dangerous permissions
     */
    protected static List<String> dangerousPermissions() {
        List<String> dangerousPermissions = new ArrayList<>();
        dangerousPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        dangerousPermissions.add(Manifest.permission.CAMERA);
        dangerousPermissions.add(Manifest.permission.RECORD_AUDIO);
//        dangerousPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE); // debug
        return dangerousPermissions;
    }

    private static List<String> missingPermissions(@NonNull Context context) {
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : dangerousPermissions()) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        return missingPermissions;
    }

    /**
     * A quick method to check if there are some permissions that are missing or needed
     * @param context a valid android context
     * @return true if some permissions are missing, false if not
     */
    public boolean hasMissingPermissions(@NonNull Context context) {
        return missingPermissions(context).size() > 0;
    }

    /**
     * Stops any actual listening. Only fired if it is actually listening
     */
    public void stopListening() {
        if (isListening.get()) {
            for (AbstractDetector detector : detectors) {
                detector.terminate();
            }
            isListening.set(false);
            Log.d(tag, "AP:Stop listening");
        }
    }

    /**
     * Changes the timeout to a value you want to use
     *
     * @param timeOut time in milliseconds the detectors should run before terminating
     */
    public void setTimeOut(long timeOut) {
        this.timeOut = timeOut;
    }

    /**
     * Gets the core listening status
     *
     * @return true if is actually listening, false if not
     */
    public boolean isListening() {
        return isListening.get();
    }
}