package com.telenor.possumauth.example.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

public abstract class TrustFragment extends Fragment {
    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
    }

    public void changeInCombinedTrust(float combinedTrustScore, String status, String graphName) {

    }
    public void changeInDetectorTrust(int detectorType, float newTrustScore, String status, String graphName) {

    }
    public void failedToAscertainTrust(Exception exception) {

    }

}