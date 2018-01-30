package com.telenor.possumgather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.util.Log;

import com.telenor.possumcore.PossumCore;
import com.telenor.possumgather.detectors.Accelerometer;
import com.telenor.possumgather.interfaces.IDataUploadComplete;
import com.telenor.possumgather.upload.AmazonUploadService;
import com.telenor.possumgather.utils.GatherUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles all data gather and upload to the Amazon data storage
 */
public class PossumGather extends PossumCore {
    private static final String tag = PossumGather.class.getName();
    private IDataUploadComplete listener;
    private AtomicBoolean isUploading = new AtomicBoolean();
    private BroadcastReceiver receiver;
    /**
     * Constructor for the gather library. Creating this instance will enable you to access and
     * gather data for further upload
     * @param context a valid android context
     * @param uniqueUserId the unique user id of the person you want to gather data about
     */
    public PossumGather(Context context, String uniqueUserId) {
        super(context, uniqueUserId);
        setTimeOut(600000); // Maximum 10 minutes of listening before session is ended
        isUploading.set(false);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(tag, "AP: Got intent:"+intent.getAction());
                context.unregisterReceiver(receiver);
                isUploading.set(false);
                // TODO: Should unregister as well
                if (listener != null) {
                    listener.dataUploadSuccess();
                    listener = null;
                }
            }
        };
    }

    /**
     * Default abstraction of which detectors are to be included. Each subset of PossumCore needs
     * to implement this to add all detectors it desires to listen to. This can also be overridden
     * by sub-methods to reduce the number of detectors if so desired.
     * @param context a valid android context
     */
    @Override
    protected void addAllDetectors(Context context) {
        // TODO: Removed rest to make it easier to test. Remember to take in again
//        addDetector(new HardwareDetector(context));
        addDetector(new Accelerometer(context));
//        addDetector(new AmbientSoundDetector(context));
//        addDetector(new GyroScope(context));
//        addDetector(new NetworkDetector(context));
//        addDetector(new LocationDetector(context));
//        addDetector(new ImageDetector(context));
//        addDetector(new BluetoothDetector(context));
    }

    /**
     * Handles upload of data to the amazon. Must be called manually after stopListening has been
     * called. Will take all files stored locally and push to
     *
     * @param context a valid android context
     * @param identityPoolId the amazon identityPool id used for upload
     * @param bucket the bucket it should upload to on Amazon
     * @param listener an interface for listening to its completion. Can be null
     */
    public void upload(@NonNull Context context, @NonNull String identityPoolId, @NonNull String bucket, IDataUploadComplete listener) {
        // TODO: Copy and refactor upload code/functionality from old version
        // TODO: Check present files and confirm they exist and is not missing
        Intent intent = new Intent(context, AmazonUploadService.class);
        intent.putExtra("bucket", bucket);
        intent.putExtra("identityPoolId", identityPoolId);
        if (!isUploading.get()) {
            this.listener = listener;
            context.registerReceiver(receiver, new IntentFilter("NotificationFTW"));
            context.startService(intent);
            isUploading.set(true);
        }
    }

    /**
     * Function for determining how much data is stored as files
     * @return the bytes stored in all saved datafiles
     */
    public long spaceUsed(@NonNull Context context) {
        List<File> files = GatherUtils.getFiles(context);
        long size = 0;
        for (File file : files) {
            size += file.length();
        }
        return size;
    }

    /**
     * Used for emergency deletion of all stored data files. Only use if absolutely necessary as it
     * will cause a loss of data. All uploaded files are automatically deleted, so try to be
     * vigilant in uploading instead.
     */
    public void deleteStored(@NonNull Context context) {
        List<File> files = GatherUtils.getFiles(context);
        for (File file : files) {
            if (!file.delete()) {
                Log.e(tag, "AP: Failed to delete file:"+file.getName());
            }
        }
    }
}