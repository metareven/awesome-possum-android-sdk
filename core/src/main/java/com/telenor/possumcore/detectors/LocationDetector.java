package com.telenor.possumcore.detectors;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.google.gson.JsonArray;
import com.telenor.possumcore.abstractdetectors.AbstractReceiverDetector;
import com.telenor.possumcore.constants.DetectorType;

/***
 * Uses gps or network to retrieve your location and time
 */
public class LocationDetector extends AbstractReceiverDetector implements LocationListener {
    private LocationManager locationManager;

    public LocationDetector(@NonNull Context context) {
        super(context, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Determines whether the location detector can be used, in effect whether the permission is
     * valid and a provider is available to peruse (only gps and network can be used)
     *
     * @return true if permission is granted and a provider is available
     */
    @Override
    public boolean isAvailable() {
        return super.isAvailable() && (isProviderAvailable(LocationManager.GPS_PROVIDER) || isProviderAvailable(LocationManager.NETWORK_PROVIDER));
    }

    /**
     * Confirms that the device has location capabilities and that at least one provider is
     * available (whether or not it is permitted)
     *
     * @return true if this detector is enabled
     */
    @Override
    public boolean isEnabled() {
        return locationManager != null && !locationManager.getAllProviders().isEmpty();
    }

    /**
     * Confirms whether use has allowed to use a specific provider
     *
     * @param provider the provider to check for
     * @return true if provider is permitted and available, false if not
     */
    private boolean isProviderAvailable(String provider) {
        return isPermitted() && locationManager != null && locationManager.isProviderEnabled(provider);
    }

    /**
     * Ensures no updates are running, removing them if it is already being used. This can cause
     * a problem for authentication when the low timespan of auth can cause a lot of attempts to
     * fail to get a position when it needs to
     */
    @Override
    public void terminate() {
        super.terminate();
        locationManager.removeUpdates(this);
    }

    /**
     * The actual gathering of a location. Atm it will consistently
     */
    @Override
    public void run() {
        // Only scan if enabled and available, a last location is missing or the lastLocation is at least 10 minutes since
        if (isEnabled() && isAvailable()) {
            Location lastLocation = lastLocation();
            if (lastLocation == null || lastLocation.getTime() < (now() - 10 * 60 * 1000)) { //
                if (isProviderAvailable(LocationManager.GPS_PROVIDER))
                    requestProviderPosition(LocationManager.GPS_PROVIDER);
                if (isProviderAvailable(LocationManager.NETWORK_PROVIDER))
                    requestProviderPosition(LocationManager.NETWORK_PROVIDER);
            } else {
                onLocationChanged(lastLocation);
            }
        }
    }

    /**
     * The actual requester of position. Can be overridden for use-cases where a single position is
     * not valid, in effect for the data gathering component. Caution, this method ignores
     * permissions as it is intended to be used in the run() method where permission is confirmed
     * before it is called. Do not call this method by itself.
     *
     * @param provider the provider it should gather for, either LocationManager.GPS_PROVIDER or
     *                 LocationManager.NETWORK_PROVIDER
     */
    @SuppressWarnings("MissingPermission")
    protected void requestProviderPosition(@NonNull String provider) {
        if (locationManager != null) {
            locationManager.requestSingleUpdate(provider, this, Looper.myLooper());
        }
    }

    /**
     * Finds the last recorded scanResult. Focuses on time and ignores accuracy. The best and most
     * recent time fitting location is
     *
     * @return the last known location or null
     */
    @SuppressWarnings("MissingPermission")
    private Location lastLocation() {
        Location lastLocation = null;
        if (locationManager != null && isPermitted()) {
            lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLocation == null) {
                lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            } else {
                Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (networkLocation != null && networkLocation.getTime() < lastLocation.getTime())
                    lastLocation = networkLocation;
            }
        }
        return lastLocation;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) return;
        JsonArray array = new JsonArray();
        array.add("" + location.getTime());
        array.add("" + location.getLatitude());
        array.add("" + location.getLongitude());
        array.add("" + location.getAltitude());
        array.add("" + location.getAccuracy());
        array.add(location.getProvider());
        streamData(array);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        detectorStatusChanged();
    }

    @Override
    public void onProviderEnabled(String provider) {
        detectorStatusChanged();
    }

    @Override
    public void onProviderDisabled(String provider) {
        detectorStatusChanged();
    }

    @Override
    public int detectorType() {
        return DetectorType.Position;
    }

    @Override
    public String detectorName() {
        return "position";
    }

    @Override
    protected void onReceiveData(Intent intent) {
        detectorStatusChanged();
    }

    @Override
    public String requiredPermission() {
        return Manifest.permission.ACCESS_FINE_LOCATION;
    }
}