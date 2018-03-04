package com.telenor.possumauth.example.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.telenor.possumauth.example.AppConstants;
import com.telenor.possumauth.example.GraphUtil;
import com.telenor.possumauth.example.R;

import java.util.Locale;

public class AllDetectorsChartFragment extends TrustFragment {
    private LineChart lineChart;
    private JsonParser parser;
    private Handler handler = new Handler(Looper.getMainLooper());
    private JsonArray presentArray;
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
        parser = new JsonParser();
        getContext().registerReceiver(receiver, new IntentFilter(AppConstants.UPDATE_GRAPHS));
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

        lineChart.setNoDataText("No trustScores yet");
        lineChart.setData(lineData);
    }

    @Override
    public void newTrustScore(String graphName, float newScore) {

    }

    @Override
    public void detectorValues(String detectorName, String dataSetName, float score, float training) {
        String graphName = String.format(Locale.US, "%s:%s", shortHand(detectorName), shortHand(dataSetName));
        if (isGraphVisible(graphName)) {
            addEntry(graphName, score);
        }
    }

    private boolean isGraphVisible(String graphName) {
        if (presentArray == null) return false;
        for (JsonElement el : presentArray) {
            JsonObject obj = el.getAsJsonObject();
            boolean isShown = obj.get("isShown").getAsBoolean();
            if (!isShown) continue;
            String originalName = obj.get("name").getAsString();
            String[] splits = originalName.split(":");
            String sName = String.format("%s:%s", shortHand(splits[0]), shortHand(splits[1]));
            if (sName.equals(graphName)) return true;
        }
        return false;
    }

    private void changeGraphs() {
        handler.post(() -> {
            SharedPreferences prefs = getContext().getSharedPreferences(AppConstants.SHARED_PREFERENCES, Context.MODE_PRIVATE);
            presentArray = (JsonArray)parser.parse(prefs.getString(AppConstants.STORED_GRAPH_DISPLAY, "[]"));
            for (JsonElement el : presentArray) {
                JsonObject obj = el.getAsJsonObject();
                String graphName = obj.get("name").getAsString();
                String[] names = graphName.split(":");
                String shortGraphName = String.format("%s:%s",shortHand(names[0]), shortHand(names[1]));
                boolean isShown = obj.get("isShown").getAsBoolean();
                ILineDataSet dataSet = lineChart.getLineData().getDataSetByLabel(shortGraphName, true);
                if (!isShown) {
                    if (dataSet != null) {
                        lineChart.getLineData().removeDataSet(dataSet);
                        lineChart.getLineData().notifyDataChanged();
                    }
                } else {
                    if (dataSet == null) {
                        ILineDataSet set = GraphUtil.lineDataSet(shortGraphName);
                        // TODO: Remove the adding of a 0 entry?
                        set.addEntry(new Entry(set.getEntryCount(), 0));
                        lineChart.getLineData().addDataSet(set);
                        lineChart.getLineData().notifyDataChanged();
                    }
                }
            }
            lineChart.notifyDataSetChanged();
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

    private String shortHand(String name) {
        return name.substring(0, 3);
    }

    private void addEntry(String graphName, float value) {
        LineData data = lineChart.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByLabel(graphName, true);
            if (set == null) {
                set = GraphUtil.lineDataSet(graphName);
                data.addDataSet(set);
            }
            set.addEntry(new Entry(set.getEntryCount(), value));
            data.notifyDataChanged();

            // move to the latest entry
            lineChart.moveViewToX(data.getEntryCount());
            // let the chart know it's data has changed
            lineChart.notifyDataSetChanged();
        }
    }
}