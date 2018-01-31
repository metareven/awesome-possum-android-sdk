package com.telenor.possumcore.detectors;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;

import com.google.gson.JsonArray;
import com.telenor.possumcore.abstractdetectors.AbstractReceiverDetector;
import com.telenor.possumcore.constants.DetectorType;

public class NetworkDetector extends AbstractReceiverDetector {
    private WifiManager wifiManager;
    private int wifiState = WifiManager.WIFI_STATE_DISABLED;

    public NetworkDetector(@NonNull Context context) {
        super(context);
        addFilterAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        ConnectivityManager mng = (ConnectivityManager)context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mng != null && mng.getActiveNetworkInfo() != null && ConnectivityManager.TYPE_WIFI == mng.getActiveNetworkInfo().getType()) {
            wifiState = WifiManager.WIFI_STATE_ENABLED;
        }
    }

    @Override
    public int detectorType() {
        return DetectorType.Network;
    }

    @Override
    public String detectorName() {
        return "network";
    }

    @Override
    public boolean isEnabled() {
        return wifiManager != null;
    }

    @Override
    public boolean isAvailable() {
        return super.isAvailable() && wifiManager != null && wifiManager.isWifiEnabled() && wifiState == WifiManager.WIFI_STATE_ENABLED;
    }

    @Override
    public void run() {
        super.run();
        for (ScanResult scanResult : wifiManager.getScanResults()) {
            JsonArray data = new JsonArray();
            data.add(""+now());
            data.add(scanResult.BSSID);
            data.add(""+scanResult.level);
            data.add(""+isConnectedToNetwork(scanResult.BSSID));
            streamData(data);
        }
    }

    private int isConnectedToNetwork(String BSSID) {
        // TODO: Implement
        // 1 = connected, 0 = not connected
        return 0;
    }

    @Override
    protected void onReceiveData(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
        detectorStatusChanged();
    }
}