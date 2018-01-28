package com.telenor.possumgather.detectors;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * A Bluetooth detector handling zipping and storing all data to file so that data can be uploaded when
 * needed. Files stored will be saved in a common directory for all detectors.
 */
public class BluetoothDetector extends com.telenor.possumcore.detectors.BluetoothDetector {
    public BluetoothDetector(@NonNull Context context) {
        super(context);
    }
    @Override
    public boolean isLongScanDoable() {
        return true;
    }
}
