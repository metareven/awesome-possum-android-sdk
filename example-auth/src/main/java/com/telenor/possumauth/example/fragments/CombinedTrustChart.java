package com.telenor.possumauth.example.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.telenor.possumauth.example.GraphUtil;
import com.telenor.possumauth.example.R;

public class CombinedTrustChart extends TrustFragment {
    private LineChart lineChart;

    @SuppressWarnings("unused")
    private static final String tag = CombinedTrustChart.class.getName();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle bundle) {
        return inflater.inflate(R.layout.fragment_sub_combined, parent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        lineChart = view.findViewById(R.id.lineChart);
        GraphUtil.configureChart(lineChart, "Trustscore pr auth", "No trustScores yet");
    }

    @Override
    public void graphUpdate(String graphName, int graphPos, float score, float trained) {
        GraphUtil.addEntry(lineChart, true, graphName, graphPos, score);
    }

    @Override
    public void updateVisibility(String graphName, boolean visible) {

    }
}