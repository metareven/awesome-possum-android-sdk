package com.telenor.possumcore.detectors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.support.annotation.NonNull;

import com.google.gson.JsonArray;
import com.telenor.possumcore.R;
import com.telenor.possumcore.abstractdetectors.AbstractSensorDetector;
import com.telenor.possumcore.constants.DetectorType;
import com.telenor.possumcore.interfaces.IDetectorChange;

/**
 * Detects changes in the gyroscope, in effect how the phone is held/lies
 */
public class GyroScope extends AbstractSensorDetector {
    public GyroScope(@NonNull Context context) {
        this(context, null);
    }
    public GyroScope(@NonNull Context context, IDetectorChange listener) {
        super(context,Sensor.TYPE_GYROSCOPE, listener);
    }

    @Override
    public int detectorType() {
        return DetectorType.Gyroscope;
    }

    @Override
    public String detectorName() {
        return "gyroscope";
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (isInvalid(sensorEvent)) return;
        JsonArray data = new JsonArray();
        data.add(""+timestamp(sensorEvent));
        data.add(""+sensorEvent.values[0]);
        data.add(""+sensorEvent.values[1]);
        data.add(""+sensorEvent.values[2]);
        streamData(data);
    }
}