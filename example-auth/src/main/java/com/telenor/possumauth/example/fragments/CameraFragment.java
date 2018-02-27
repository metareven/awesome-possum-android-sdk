package com.telenor.possumauth.example.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.telenor.possumauth.example.MainActivity;
import com.telenor.possumauth.example.R;
import com.telenor.possumcore.constants.DetectorType;
import com.telenor.possumcore.detectors.ImageDetector;

public class CameraFragment extends TrustFragment {
    private SurfaceView surfaceView;
    private ImageView faceFoundView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle bundle) {
        return inflater.inflate(R.layout.fragment_camera, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        surfaceView = view.findViewById(R.id.previewView);
        //surfaceView.setCamera(Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT));
        faceFoundView = view.findViewById(R.id.faceFoundView);
        setCameraView();
    }

    private void setCameraView() {
        if (surfaceView != null) {
            ImageDetector imageDetector = (ImageDetector)((MainActivity)getActivity()).possumAuth().detectorWithType(DetectorType.Image);
//            imageDetector.setPreviewHolder(surfaceView.getHolder());
//            imageDetector.setFaceFoundView(faceFoundView);
        }
    }
}