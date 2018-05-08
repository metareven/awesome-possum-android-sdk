package com.telenor.possumauth;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.telenor.possumauth.interfaces.IAuthCompleted;
import com.telenor.possumcore.PossumCore;
import com.telenor.possumcore.abstractdetectors.AbstractDetector;
import com.telenor.possumcore.constants.DetectorType;
import com.telenor.possumcore.detectors.Accelerometer;
import com.telenor.possumcore.detectors.AmbientSoundDetector;
import com.telenor.possumcore.detectors.BluetoothDetector;
import com.telenor.possumcore.detectors.GyroScope;
import com.telenor.possumcore.detectors.HardwareDetector;
import com.telenor.possumcore.detectors.ImageDetector;
import com.telenor.possumcore.detectors.LocationDetector;
import com.telenor.possumcore.detectors.NetworkDetector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic class for handling authentication based on data gathering from PossumCore
 */
public class PossumAuth extends PossumCore implements IAuthCompleted {
    private final String uploadUrl;
    private final String apiKey;
    private List<IAuthCompleted> listeners;
    private static final String tag = PossumAuth.class.getName();

    /**
     * Constructor for the PossumAuth used to authenticate/retrieve information about user with a
     * REST api. Required is a url to connect to as well as an apiKey for the
     *
     * @param context      a valid android context
     * @param uniqueUserId the user you want to authenticate. Can be switched later.
     * @param uploadUrl    the url you want to connect to. Must create new instance to change.
     * @param apiKey       the api key for the REST. Must create new instance to change.
     */
    public PossumAuth(Context context, String uniqueUserId, String uploadUrl, String apiKey) {
        super(context, uniqueUserId);
        this.uploadUrl = uploadUrl;
        this.apiKey = apiKey;
        listeners = new ArrayList<>();
    }

    @Override
    protected void addAllDetectors(Context context) {
        addDetector(new HardwareDetector(context, this));
        addDetector(new Accelerometer(context, this));
        addDetector(new AmbientSoundDetector(context, this));
        addDetector(new GyroScope(context, this));
        addDetector(new NetworkDetector(context, this));
        addDetector(new LocationDetector(context, this));
        addDetector(new ImageDetector(context, this));
        addDetector(new BluetoothDetector(context, this));
    }

    /**
     * A handy way to get the version of the possumAuth library
     *
     * @param context a valid android context
     * @return a string representing the current version of the library
     */
    public static String version(@NonNull Context context) {
        return context.getString(R.string.possum_auth_version_name);
    }

    /**
     * Adds a listener for an authentication attempt
     *
     * @param listener a listener for authentication
     */
    public void addAuthListener(IAuthCompleted listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener for an authentication attempt
     *
     * @param listener a listener for authentication
     */
    public void removeAuthListener(IAuthCompleted listener) {
        listeners.remove(listener);
    }

    /**
     * Used to send all in-memory stored data from a data listening to the authentication servers
     *
     * @return false if no data is available to send or it failed to send data
     */
    public boolean authenticate() {
//        Log.i(tag, "AP: Start authenticate");
        JsonObject jsonData = new JsonObject();
        jsonData.addProperty("connectId", userId());
        boolean isEmpty = true;
        for (AbstractDetector detector : detectors()) {
            for (String dataSet : detector.dataStored().keySet()) {
                String dataSetName = dataSet.equals("default") ? detector.detectorName() : dataSet;
                JsonArray data = detector.jsonData(dataSet);
                if (data.size() > 0) {
                    jsonData.add(dataSetName, data);
                    isEmpty = false;
                }
            }
        }
        if (isEmpty) {
            Log.i(tag, "AP: No data, preventing authentication");
            return false;
        }
        //storeToFile(jsonData);
        try {
            new AsyncRestAuthentication(uploadUrl, apiKey, this).execute(jsonData);
            return true;
        } catch (MalformedURLException e) {
            Log.e(tag, "Malformed url:", e);
        }
        return false;
    }

    public static int detectorTypeFromName(String detectorName) {
        switch (detectorName) {
            case "accelerometer":
                return DetectorType.Accelerometer;
            case "bluetooth":
                return DetectorType.Bluetooth;
            case "gyroscope":
                return DetectorType.Gyroscope;
            case "image":
                return DetectorType.Image;
            case "network":
                return DetectorType.Network;
            case "position":
                return DetectorType.Position;
            case "sound":
                return DetectorType.Audio;
            default:
                return 0;
        }
    }

    @SuppressWarnings("unused")
    private void storeToFile(JsonObject object) {
        File outputFile = new File(Environment.getExternalStorageDirectory().getPath() + "/datafile.txt");
        if (outputFile.exists()) {
            outputFile.delete();
        }
        try {
            outputFile.createNewFile();
        } catch (IOException e) {
            Log.i(tag, "Failed to create file (" + outputFile.getAbsolutePath() + "):", e);
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outputFile);
            fos.write(object.toString().getBytes());
        } catch (FileNotFoundException e) {
            Log.i(tag, "OutputFile not found:", e);
        } catch (IOException e) {
            Log.i(tag, "Failed to write to file:", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.i(tag, "Failed to close file:", e);
                }
            }
        }
    }

    @Override
    public void messageReturned(String message, String responseMessage, Exception e) {
        for (IAuthCompleted listener : listeners) {
            listener.messageReturned(message, responseMessage, e);
        }
    }
}