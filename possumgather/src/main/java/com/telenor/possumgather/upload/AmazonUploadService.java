package com.telenor.possumgather.upload;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWS3Signer;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.IdentityChangedListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.telenor.possumgather.R;
import com.telenor.possumgather.utils.GatherUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles the actual upload of the files stored to Amazon cloud
 */
public class AmazonUploadService extends Service implements TransferListener, IdentityChangedListener {
    private CognitoCachingCredentialsProvider cognitoProvider;
    private TransferUtility transferUtility;
    private List<File> files;
    private AtomicInteger fileCounter;
    private AtomicBoolean isUploading;
    private String bucket;
    private static final String tag = AmazonUploadService.class.getName();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        files = GatherUtils.getFiles(this);
//        System.setProperty(SDKGlobalConfiguration.ENABLE_S3_SIGV4_SYSTEM_PROPERTY, "true");
        if (files.size() == 0) {
            Log.i(tag, "AP: No files found, terminating service");
            stopSelf();
            return START_NOT_STICKY;
        }
        String identityPoolId = intent.getStringExtra("identityPoolId");
        bucket = intent.getStringExtra("bucket");
        String version = getResources().getString(R.string.possumgather_version);
        Log.i(tag, "AP: Version number:"+version);
        if (identityPoolId == null || bucket == null) {
            // Sending in invalid parameters terminates service as well as any already running
            // operation
            Log.i(tag, "AP: Service missing parameters, stopping:"+identityPoolId+","+bucket);
            stopSelf();
        } else if (isUploading.get()) {
            // Already started, let it continue
            Log.i(tag, "AP: Is already uploading, continuing process");
            return START_NOT_STICKY;
        } else {
            isUploading.set(true);
            fileCounter.set(0);
            cognitoProvider = new CognitoCachingCredentialsProvider(
                    this,
                    identityPoolId,
                    Regions.EU_CENTRAL_1 // Region
            );
            if (cognitoProvider.getCachedIdentityId() != null) {
                Log.i(tag, "AP: Found identity, starting");
                startUpload();
            } else {
                Log.i(tag, "AP: Did not find identity, checking for it");
                cognitoProvider.registerIdentityChangedListener(this);
                new AmazonIdentity().execute(cognitoProvider);
            }
        }
        return START_NOT_STICKY;
    }

    private static class AmazonIdentity extends AsyncTask<CognitoCachingCredentialsProvider, Void, String> {
        @Override
        protected String doInBackground(CognitoCachingCredentialsProvider... providers) {
            if (providers[0] == null) return null;
            return providers[0].getIdentityId();
        }
    }

    private void startUpload() {
        AmazonS3Client amazonS3Client = new AmazonS3Client(cognitoProvider);
        amazonS3Client.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));
        transferUtility = new TransferUtility(amazonS3Client, this);
        List<File> files = GatherUtils.getFiles(this);
        fileCounter.set(files.size());
        Log.i(tag, "AP: Files found:"+files.size());

        for (File file : files) {
            String key = file.getName().replace("#", "/"); // TODO: Get key from file name
            if (file.exists()) {
                Log.i(tag, "AP: Uploading:" + bucket + ", " + key + ", " + file.length()+", "+file.getAbsolutePath());
                transferUtility.upload(bucket, key, file).setTransferListener(this);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize all non-dependent variables
        isUploading = new AtomicBoolean(false);
        fileCounter = new AtomicInteger(0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(tag, "AP: Destroying service");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStateChanged(int id, TransferState state) {
        Log.i(tag, "AP: State change:"+id+", "+state.toString());
    }

    @Override
    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
    }

    @Override
    public void onError(int id, Exception ex) {
        Log.i(tag, "AP: Error:"+id+":",ex);
    }

    @Override
    public void identityChanged(String oldIdentityId, String newIdentityId) {
        if (cognitoProvider.getCachedIdentityId() != null) {
            cognitoProvider.unregisterIdentityChangedListener(this);
            Log.i(tag, "AP: Identity check found you, starting upload");
            startUpload();
        } else {
            Log.e(tag, "AP: Unable to get identity for upload: Find out what happens");
            stopSelf();
        }
    }
}
