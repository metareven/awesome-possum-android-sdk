package com.telenor.possumauth.example.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.gson.JsonObject;
import com.telenor.possumauth.example.GraphUtil;
import com.telenor.possumauth.example.R;

public class CombinedTrustChart extends TrustFragment {
    private LineChart lineChart;

    private static final String tag = CombinedTrustChart.class.getName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle bundle) {
        return inflater.inflate(R.layout.fragment_sub_combined, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        LineData lineData = new LineData();
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

        Description description = new Description();
        description.setText("Trustscore pr auth");
        lineChart.setDescription(description);

        lineChart.setDrawGridBackground(false);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getXAxis().setDrawLabels(false);
        lineChart.getAxisLeft().setTextSize(15f);
        lineChart.getXAxis().setDrawAxisLine(true);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
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

        lineChart.setNoDataText("No trustScores yet");
        lineChart.setData(lineData);
    }

    @Override
    public void newTrustScore(String graphName, float newScore) {
        addEntry(newScore, graphName);
    }

    @Override
    public void detectorValues(String detectorName, String dataSetName, float score, float training) {

    }

    private void addEntry(float combinedTrustScore, String graphName) {
        if (!graphName.equals("default")) return;
        if (lineChart == null) {
            Log.i(tag, "Crisis, got a null lineChart in allSensors");
            return;
        }

        LineData data = lineChart.getData();

        if (data != null) {

            LineDataSet set = (LineDataSet)data.getDataSetByIndex(0);
            if (set == null) {
                set = GraphUtil.lineDataSet(graphName, Color.rgb(244, 117, 117));
                data.addDataSet(set);
            }
            data.addEntry(new Entry(set.getEntryCount(), combinedTrustScore), 0);
            data.notifyDataChanged();

            // move to the latest entry
            lineChart.moveViewToX(data.getEntryCount());

            // let the chart know it's data has changed
            lineChart.notifyDataSetChanged();
        }
    }
}