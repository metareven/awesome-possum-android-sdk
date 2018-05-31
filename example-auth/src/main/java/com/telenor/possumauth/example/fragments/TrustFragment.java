package com.telenor.possumauth.example.fragments;

import android.support.v4.app.Fragment;

import com.telenor.possumcore.abstractdetectors.AbstractDetector;
import com.telenor.possumcore.interfaces.IDetectorChange;

public abstract class TrustFragment extends Fragment implements IDetectorChange {
    @Override
    public void detectorChanged(AbstractDetector detector) {

    }
    public abstract void graphUpdate(String graphName, int graphPos, float score, float trained);
    public void updateVisibility(String graphName, boolean visible) {

    }
}