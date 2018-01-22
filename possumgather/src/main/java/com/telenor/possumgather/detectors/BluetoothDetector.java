package com.telenor.possumgather.detectors;

import android.content.Context;
import android.support.annotation.NonNull;

public class BluetoothDetector extends com.telenor.possumcore.detectors.BluetoothDetector {
    public BluetoothDetector(@NonNull Context context) {
        super(context);
    }
    @Override
    public boolean isLongScanDoable() {
        return true;
    }
}
