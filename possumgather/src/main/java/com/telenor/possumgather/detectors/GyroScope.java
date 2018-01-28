package com.telenor.possumgather.detectors;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * A gyroscope handling zipping and storing all data to file so that data can be uploaded when
 * needed. Files stored will be saved in a common directory for all detectors.
 */
public class GyroScope extends com.telenor.possumcore.detectors.GyroScope {
    public GyroScope(@NonNull Context context) {
        super(context);
    }
}