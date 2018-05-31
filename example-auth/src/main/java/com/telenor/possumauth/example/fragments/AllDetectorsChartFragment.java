package com.telenor.possumauth.example.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.gson.JsonElement;
import com.telenor.possumauth.example.AppConstants;
import com.telenor.possumauth.example.GraphUtil;
import com.telenor.possumauth.example.R;

import java.util.Map;

public class AllDetectorsChartFragment extends TrustFragment {
    private LineChart lineChart;
    private Handler handler = new Handler(Looper.getMainLooper());
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            changeGraphs();
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle bundle) {
        return inflater.inflate(R.layout.fragment_sub_all_sensors_chart, parent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        if (getContext() == null) throw new IllegalStateException("Context is null in detector fragment");
        getContext().registerReceiver(receiver, new IntentFilter(AppConstants.UPDATE_GRAPHS));
        lineChart = view.findViewById(R.id.lineChart);
        GraphUtil.configureChart(lineChart, "All sensors", "No data yet");
    }

    @Override
    public void graphUpdate(String graphName, int graphPos, float score, float trained) {
        if (getContext() == null) throw new IllegalStateException("Context is null in graphUpdate");
        GraphUtil.addEntry(lineChart, GraphUtil.graphVisibility(getContext().getSharedPreferences(AppConstants.SHARED_PREFERENCES, Context.MODE_PRIVATE), graphName), graphName, graphPos, score);
    }

    @Override
    public void updateVisibility(String graphName, boolean visible) {
        GraphUtil.updateVisibility(lineChart, graphName, visible);
    }

    private void changeGraphs() {
        if (lineChart.getLineData() == null) return;
        if (getContext() == null) throw new IllegalStateException("Context is null in changeGraphs");
        handler.post(() -> {
            for (Map.Entry<String, JsonElement> entry : GraphUtil.graphVisibility(getContext().getSharedPreferences(AppConstants.SHARED_PREFERENCES, Context.MODE_PRIVATE)).entrySet()) {
                String graphName = entry.getKey();
                ILineDataSet dataSet = lineChart.getLineData().getDataSetByLabel(graphName, true);
                if (dataSet != null)
                    dataSet.setVisible(entry.getValue().getAsBoolean());
            }
            lineChart.invalidate();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getContext() == null) throw new IllegalStateException("Context is null in onDestroy");
        getContext().unregisterReceiver(receiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        changeGraphs();
    }
}