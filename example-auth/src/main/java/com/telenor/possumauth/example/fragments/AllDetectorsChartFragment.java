package com.telenor.possumauth.example.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.telenor.possumauth.example.AppConstants;
import com.telenor.possumauth.example.GraphUtil;
import com.telenor.possumauth.example.MainActivity;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle bundle) {
        return inflater.inflate(R.layout.fragment_sub_all_sensors_chart, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        getContext().registerReceiver(receiver, new IntentFilter(AppConstants.UPDATE_GRAPHS));
        lineChart = view.findViewById(R.id.lineChart);
        lineChart.setTouchEnabled(false);
        lineChart.setScaleEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.setDrawBorders(false);
        Legend l = lineChart.getLegend();
        l.setEnabled(true);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(true);
        l.setTypeface(Typeface.DEFAULT);
        l.setForm(Legend.LegendForm.CIRCLE);
        lineChart.setDrawGridBackground(false);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getXAxis().setDrawLabels(false);
        lineChart.getAxisLeft().setTextSize(15f);
        lineChart.getXAxis().setDrawAxisLine(true);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        Description description = new Description();
        description.setText("All sensors");
        lineChart.setDescription(description);
        lineChart.getAxisLeft().setAxisMaximum(1.1f);
        lineChart.getAxisLeft().setAxisMinimum(0);
        lineChart.getAxisLeft().setDrawLabels(true);
        lineChart.getAxisLeft().setDrawGridLines(false);
        lineChart.getAxisLeft().setDrawAxisLine(true);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getAxisRight().setDrawLabels(false);
        lineChart.getAxisRight().setDrawGridLines(false);
        lineChart.getAxisRight().setDrawAxisLine(false);

        // limit the number of visible entries
        //lineChart.setVisibleXRangeMaximum(20);
        lineChart.setMaxVisibleValueCount(20);

        lineChart.setNoDataText("No data yet");
    }

    @Override
    public void newTrustScore(String graphName, int graphPos, float newScore) {

    }

    @Override
    public void detectorValues(String graphName, int graphPos, float score, float training) {
        JsonObject graphs = ((MainActivity)getActivity()).graphVisibility();
        JsonElement el = graphs.get(graphName);
        boolean visible = !el.isJsonNull() && el.getAsBoolean();
        GraphUtil.addEntry(lineChart, visible, graphName, graphPos, score);
    }

    @Override
    public void updateVisibility(String graphName, boolean visible) {
        GraphUtil.updateVisibility(lineChart, graphName, visible);
    }

    private void changeGraphs() {
        handler.post(() -> {
            if (lineChart.getLineData() == null) return;
            for (Map.Entry<String, JsonElement> entry : ((MainActivity)getActivity()).graphVisibility().entrySet()) {
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
        getContext().unregisterReceiver(receiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        changeGraphs();
    }
}