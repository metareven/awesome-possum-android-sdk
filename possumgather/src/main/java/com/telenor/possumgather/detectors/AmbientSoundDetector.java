package com.telenor.possumgather.detectors;

import android.content.Context;

/**
 * An Ambient Sound detector handling zipping and storing all data to file so that data can be uploaded when
 * needed. Files stored will be saved in a common directory for all detectors.
 */
public class AmbientSoundDetector extends com.telenor.possumcore.detectors.AmbientSoundDetector {
    public AmbientSoundDetector(Context context) {
        super(context);
    }
}