package com.telenor.possumgather;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.telenor.possumcore.PossumCore;
import com.telenor.possumgather.detectors.Accelerometer;
import com.telenor.possumgather.detectors.AmbientSoundDetector;
import com.telenor.possumgather.detectors.BluetoothDetector;
import com.telenor.possumgather.detectors.GyroScope;
import com.telenor.possumgather.detectors.ImageDetector;
import com.telenor.possumgather.detectors.LocationDetector;
import com.telenor.possumgather.detectors.NetworkDetector;
import com.telenor.possumgather.services.AmazonUploadService;

/**
 * Handles all data gather and upload to the Amazon data storage
 */
public class PossumGather extends PossumCore {

    /**
     * Constructor for the gather library. Creating this instance will enable you to access and
     * gather data for further upload
     * @param context a valid android context
     * @param uniqueUserId the unique user id of the person you want to gather data about
     */
    public PossumGather(Context context, String uniqueUserId) {
        super(context, uniqueUserId);
    }

    /**
     * Default abstraction of which detectors are to be included. Each subset of PossumCore needs
     * to implement this to add all detectors it desires to listen to. This can also be overridden
     * by sub-methods to reduce the number of detectors if so desired.
     * @param context a valid android context
     */
    @Override
    protected void addAllDetectors(Context context) {
        addDetector(new Accelerometer(context));
        addDetector(new AmbientSoundDetector(context));
        addDetector(new GyroScope(context));
        addDetector(new NetworkDetector(context));
        addDetector(new LocationDetector(context));
        addDetector(new ImageDetector(context));
        addDetector(new BluetoothDetector(context));
    }

    /**
     * Handles upload of data to the amazon. Must be called manually after stopListening has been
     * called. Will take all files stored locally and push to
     *
     * @param context a valid android context
     * @param identityPoolId the amazon identityPool id used for upload
     * @return an exception if it fails to upload or no data is available, otherwise null if it is
     * successful
     */
    public Exception upload(@NonNull Context context, @NonNull String identityPoolId) {
        // TODO: Copy and refactor upload code/functionality from old version
        // TODO: Check present files and confirm they exist and is not missing
        Intent intent = new Intent(AmazonUploadService.class.getName());
        intent.putExtra("identityPoolId", identityPoolId);
        intent.putExtra("files", "filePath");
        context.startService(intent);
        return null;
    }

    /**
     * Function for determining how much data is stored as files
     * @return the bytes stored in all saved datafiles
     */
    public long spaceUsed() {
        return 0;
    }

    /**
     * Used for emergency deletion of all stored data files. Only use if absolutely necessary as it
     * will cause a loss of data. All uploaded files are automatically deleted, so try to be
     * vigilant in uploading instead.
     */
    public void deleteStored() {

    }
}