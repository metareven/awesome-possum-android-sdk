package com.telenor.possumgather.detectors;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.JsonArray;
import com.telenor.possumgather.R;
import com.telenor.possumgather.utils.CountingOutputStream;
import com.telenor.possumgather.utils.GatherUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * An accelerometer handling zipping and storing all data to file so that data can be uploaded when
 * needed. Files stored will be saved in a common directory for all detectors.
 */
public class Accelerometer extends com.telenor.possumcore.detectors.Accelerometer {
    public Accelerometer(@NonNull Context context) {
        super(context);
    }

    @Override
    public void terminate() {
        super.terminate();
        File storedCatalogue = GatherUtils.storageCatalogue(context());
        //        return "possumlibdata/" + AwesomePossum.versionName(context()) + "/" + detectorName() + "/" + uniqueUserId + "/" + now() + ".zip";
        String version = context().getString(R.string.possumgather_version);
        String fileName = String.format(Locale.US, "possum-data#%s#%s#%s#%s.zip",version, detectorName(), getUserId(), now());
        File uploadFile = new File(storedCatalogue, fileName);
        try {
            CountingOutputStream innerStream = new CountingOutputStream(new FileOutputStream(uploadFile));
            for (String dataSet : dataStored.keySet()) {
                List<JsonArray> data = dataStored.get(dataSet);
                if (data.size() > 0) {
                    try {
                        ZipOutputStream outerStream = GatherUtils.createZipStream(innerStream, dataSet);
                        for (JsonArray value : data) {
                            try {
                                if (outerStream != null) {
                                    outerStream.write(value.toString().getBytes());
                                    outerStream.write("\r\n".getBytes());
                                }
                            } catch (Exception e) {
                                Log.e(tag, "AP: FailedToWrite:", e);
                            }
                        }
                        if (outerStream != null) outerStream.close();
                    } catch (Exception e) {
                        Log.i(tag, "AP: Failed to create zipStream:",e);
                    }
                }
            }
            innerStream.close();
        } catch (Exception e) {
            Log.e(tag, "AP: Failed to store file:", e);
        }
    }
}