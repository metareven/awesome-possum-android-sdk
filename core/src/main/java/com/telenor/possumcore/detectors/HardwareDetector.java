package com.telenor.possumcore.detectors;

import android.content.Context;
import android.os.Build;

import com.google.gson.JsonArray;
import com.telenor.possumcore.abstractdetectors.AbstractDetector;
import com.telenor.possumcore.constants.DetectorType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Detector meant to detect hardware info
 */
public class HardwareDetector extends AbstractDetector {
    /**
     * Constructor for all detectors. Initializes a basic detector
     *
     * @param context a valid android context
     */
    public HardwareDetector(Context context) {
        super(context);
    }

    @Override
    public int detectorType() {
        return DetectorType.Hardware;
    }

    @Override
    public String detectorName() {
        return "hardware";
    }

    @Override
    public void run() {
        super.run();
        JsonArray array = new JsonArray();
        array.add("HARDWARE_INFO START");
        array.add("Board:" + Build.BOARD);
        array.add("Brand:" + Build.BRAND);
        array.add("Device:" + Build.DEVICE);
        array.add("Display:" + Build.DISPLAY);
        array.add("Fingerprint:" + Build.FINGERPRINT);
        array.add("Hardware:" + Build.HARDWARE);
        array.add("Host:" + Build.HOST);
        array.add("Id:" + Build.ID);
        array.add("Manufacturer:" + Build.MANUFACTURER);
        array.add("Model:" + Build.MODEL);
        array.add("Product:" + Build.PRODUCT);
        array.add("Serial:" + Build.SERIAL); // Not recommended method
        array.add("Version:" + Build.VERSION.SDK_INT + " (" + Build.VERSION.CODENAME + ")");
        StringBuilder output = new StringBuilder();
        List<String> supported = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Collections.addAll(supported, Build.SUPPORTED_ABIS);
        } else {
            supported.add(Build.CPU_ABI);
            supported.add(Build.CPU_ABI2);
        }
        for (int i = 0; i < supported.size(); i++) {
            if (i > 0) {
                output.append(", ");
            }
            output.append(supported.get(i));
        }
        array.add("SupportedABIS:" + output.toString());
        // TODO: Add information about which detectors are not enabled?
        array.add("HARDWARE_INFO STOP");
        dataStored.get(defaultSet).add(array);
    }

    @Override
    public void terminate() {

    }
}
