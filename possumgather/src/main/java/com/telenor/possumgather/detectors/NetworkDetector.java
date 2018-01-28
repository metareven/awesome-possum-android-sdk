package com.telenor.possumgather.detectors;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * A network detector handling zipping and storing all data to file so that data can be uploaded when
 * needed. Files stored will be saved in a common directory for all detectors.
 */
public class NetworkDetector extends com.telenor.possumcore.detectors.NetworkDetector {
    public NetworkDetector(@NonNull Context context) {
        super(context);
    }
}