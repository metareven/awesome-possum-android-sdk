package com.telenor.possumauth.example.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import com.telenor.possumcore.abstractdetectors.AbstractDetector;
import com.telenor.possumcore.interfaces.IDetectorChange;

public abstract class TrustFragment extends Fragment implements IDetectorChange {
    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
    }

    @Override
    public void detectorChanged(AbstractDetector detector) {

    }
    public abstract void newTrustScore(String graphName, int graphPos, float newScore);
    public abstract void detectorValues(String graphName, int graphPos, float score, float training);
    public abstract void updateVisibility(String graphName, boolean visible);
}