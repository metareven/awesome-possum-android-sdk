package com.telenor.possumcore.detectors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.support.annotation.NonNull;

import com.google.gson.JsonArray;
import com.telenor.possumcore.abstractdetectors.AbstractSensorDetector;
import com.telenor.possumcore.constants.DetectorType;

/**
 * Uses accelerometer to determine the movement/gait of the user, as well as detecting motion
 * and how the phone is held
 */
public class Accelerometer extends AbstractSensorDetector {
    public Accelerometer(@NonNull Context context) {
        super(context, Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public int detectorType() {
        return DetectorType.Accelerometer;
    }

    @Override
    public String detectorName() {
        return "accelerometer";
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