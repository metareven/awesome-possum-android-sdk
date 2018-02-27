package com.telenor.possumauth;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.telenor.possumcore.PossumCore;
import com.telenor.possumcore.detectors.Accelerometer;
import com.telenor.possumcore.detectors.AmbientSoundDetector;
import com.telenor.possumcore.detectors.BluetoothDetector;
import com.telenor.possumcore.detectors.GyroScope;
import com.telenor.possumcore.detectors.HardwareDetector;
import com.telenor.possumcore.detectors.ImageDetector;
import com.telenor.possumcore.detectors.LocationDetector;
import com.telenor.possumcore.detectors.NetworkDetector;

/**
 * Basic class for handling authentication based on data gathering from PossumCore
 */
public class PossumAuth extends PossumCore {
    private final String uploadUrl;
    private static final String tag = PossumAuth.class.getName();
    public PossumAuth(Context context, String uniqueUserId, String uploadUrl) {
        super(context, uniqueUserId);
        this.uploadUrl = uploadUrl;
    }

    @Override
    protected void addAllDetectors(Context context) {
        addDetector(new HardwareDetector(context));
        addDetector(new Accelerometer(context));
        addDetector(new AmbientSoundDetector(context));
        addDetector(new GyroScope(context));
        addDetector(new NetworkDetector(context));
        addDetector(new LocationDetector(context));
        addDetector(new ImageDetector(context));
        addDetector(new BluetoothDetector(context));
    }

    @Override
    public void stopListening() {
        super.stopListening();
        if (!verify()) {
            Log.i(tag, "AP: Failed to upload to auth server");
        }
    }

    /**
     * A handy way to get the version of the possumAuth library
     * @param context a valid android context
     * @return a string representing the current version of the library
     */
    public static String version(@NonNull Context context) {
        return context.getString(R.string.possum_auth_version_name);
    }


    /**
     * Shorthand method for verify. Starts upload
     * @return false if no data is available to send or it failed to send data
     */
    public boolean verify() {
        return verify(uploadUrl);
    }
    /**
     * Used to send all in-memory stored data from a data listening to the authentication servers
     * @param uploadUrl the url for the
     * @return false if no data is available to send or it failed to send data
     */
    public boolean verify(@NonNull String uploadUrl) {
        return false;
    }
}