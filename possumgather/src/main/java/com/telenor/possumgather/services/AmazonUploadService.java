package com.telenor.possumgather.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Handles the actual upload of the files stored to Amazon cloud
 */
public class AmazonUploadService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
