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
import com.telenor.possumcore.constants.CoreStatus;
import com.telenor.possumcore.detectors.AmbientSoundDetector;
import com.telenor.possumcore.detectors.ImageDetector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The core component for gathering data. This is the foundation for the Awesome Possum library.
 * It handles the detectors, starting them, stopping them and the subclasses handles whatever is
 * done with the data.
 */
public abstract class PossumCore {
    private Set<AbstractDetector> detectors = new HashSet<>();
    private Handler handler = new Handler();
    private AtomicInteger status = new AtomicInteger(CoreStatus.Idle);
    private ExecutorService executorService = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "PossumProcessing");
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    private AtomicBoolean deniedCamera = new AtomicBoolean(false);
    private long timeOut = 3000; // Default timeOut
    private static final String tag = PossumCore.class.getName();

    /**
     * Constructor for the PossumCore
     *
     * @param context      a valid android context
     * @param uniqueUserId the unique identifier of whoever this session will gather data from
     */
    public PossumCore(@NonNull Context context, @NonNull String uniqueUserId) {
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
     * Starts gathering data. Will not access image or sound if program has requested these to not
     * be called.
     *
     * @return false if no detectors available to start or one of them fails to start or already
     * listening, else true
     */
    public boolean startListening() {
        if (status.get() != CoreStatus.Idle || detectors == null || detectors.size() == 0)
            return false;
        for (AbstractDetector detector : detectors) {
            if (deniedCamera.get() && (detector instanceof ImageDetector || detector instanceof AmbientSoundDetector))
                continue;
            executorService.submit(detector);
        }
        Log.d(tag, "AP:Start listening");
        status.set(CoreStatus.Running);
        if (timeOut > 0)
            handler.postDelayed(this::stopListening, timeOut);
        return true;
    }

    /**
     * Sets the status according to CoreStatus constants
     *
     * @param newStatus the new status
     */
    protected void setStatus(int newStatus) {
        status.set(newStatus);
    }

    /**
     * Returns a synchronized status of the core system
     *
     * @return a CoreStatus integer
     */
    public int getStatus() {
        return status.get();
    }

    /**
     * Handles all changes in configuration from the app. This must be overridden in the activity
     * used and passed down to the possumCore
     *
     * @param newConfig the new configuration it is in
     */
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(tag, "AP: New Configuration:" + newConfig.toString());
        // TODO: Need a working configChanged representation
    }

    /**
     * Handles pausing of all detectors while the app is for some reason closed down - for example
     * a phone call interrupting or the user
     */
    public void onPause() {
        if (status.get() == CoreStatus.Running) {
            // TODO: Pause detectors
        }
    }

    /**
     * Handles an effective restart of eventual paused app due to interruption in progress
     */
    public void onResume() {
        if (status.get() == CoreStatus.Running) {
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
     * Prevents image detector from being used. This due to an issue
     * causing pre-lollipop phones (api 21) to not be able to detect whether camera is in use or
     * not. As a consequence, before any video conferences or microphone/camera uses, this method
     * should be called to prevent it from listening in on these sensors when this is needed.
     */
    public void denyCamera() {
        for (AbstractDetector detector : detectors) {
            if (detector instanceof ImageDetector) {
                detector.terminate();
            }
        }
        deniedCamera.set(true);
    }

    /**
     * Allows image detector and ambient sound detector to be used. This due to an issue
     * causing pre-lollipop phones (api 21) to not be able to detect whether camera is in use or
     * not. As a consequence, after any video conferences or microphone/camera uses, this method
     * should be called to allow it to listen in on these sensors when this is needed.
     * <p>
     * Note: This method only needs to be called if you previously denied image/sound
     */
    public void allowCamera() {
        if (isListening() && deniedCamera.get()) {
            for (AbstractDetector detector : detectors) {
                if (detector instanceof ImageDetector ) {
                    // Submit if already denied
                    executorService.submit(detector);
                }
            }
            deniedCamera.set(false);
        }
    }

    /**
     * Quick way to access the detectors registered from subclasses
     *
     * @return a set with the detectors
     */
    public Set<AbstractDetector> detectors() {
        return detectors;
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
        return dangerousPermissions;
    }

    /**
     * The present list of all permissions required, including the dangerous ones. Subtract the
     * missing permissions to get a list of allowed permissions.
     *
     * @return a list of all permissions that Awesome Possum wants
     */
    public static List<String> permissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        permissions.add(Manifest.permission.BLUETOOTH);
        permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.INTERNET);
        return permissions;
    }

    /**
     * Returns a list of which permissions are NOT granted by user
     *
     * @param context a valid android context
     * @return a list of denied permissions, empty array if none
     */
    public static List<String> missingPermissions(@NonNull Context context) {
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
     *
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
        if (status.get() == CoreStatus.Running) {
            for (AbstractDetector detector : detectors) {
                detector.terminate();
            }
            status.set(CoreStatus.Idle);
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
     * Quick check for whether the system is listening atm
     *
     * @return true if it is listening, false if in other state
     */
    public boolean isListening() {
        return status.get() == CoreStatus.Running;
    }
}