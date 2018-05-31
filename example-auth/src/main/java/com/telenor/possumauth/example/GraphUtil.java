package com.telenor.possumauth.example;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GraphUtil {
    private static JsonParser parser = new JsonParser();
    private static int randomColor() {
        Random rand = new Random();
        int r = rand.nextInt(255);
        int g = rand.nextInt(255);
        int b = rand.nextInt(255);
        return Color.rgb(r, g, b);
    }

    private static LineDataSet lineDataSet(String graphName) {
        LineDataSet set = new LineDataSet(null, graphName);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setMode(LineDataSet.Mode.LINEAR); //CUBIC_BEZIER
        set.setLineWidth(2f);
        set.setColor(colorWithName(graphName));
        set.setDrawValues(false);
        set.setDrawCircles(false);
        return set;
    }

    public static void addEntry(LineChart lineChart, boolean visibility, String graphName, int graphPos, float value) {
        LineData data = lineChart.getData();
        if (data == null) {
            data = new LineData();
            lineChart.setData(data);
        }
        ILineDataSet set = data.getDataSetByLabel(graphName, true);
        if (set == null) {
            set = GraphUtil.lineDataSet(graphName);
            data.addDataSet(set);
            set.setVisible(visibility);
        }
        boolean added = set.addEntry(new Entry(graphPos, value));
        if (added) {
            data.notifyDataChanged();
        }
        // move to the latest entry
        lineChart.moveViewToX(graphPos);
        // let the chart know it's data has changed
        lineChart.notifyDataSetChanged();
    }

    private static int colorWithName(String graphName) {
        if ("gac:def".equals(graphName)) return Color.RED;
        if ("sou:cla".equals(graphName)) return Color.GREEN;
        if ("ima:def".equals(graphName)) return Color.BLUE;
        if ("net:def".equals(graphName)) return Color.YELLOW;
        if ("net:bin".equals(graphName)) return Color.CYAN;
        if ("blu:def".equals(graphName)) return Color.MAGENTA;
        return randomColor();
    }

    /**
     * Configures a given chart with a set of standard options
     *
     * @param lineChart the lineChart you wish to configure
     * @param description its description
     * @param noDataText its no data text
     */
    public static void configureChart(@NonNull LineChart lineChart, String description, String noDataText) {
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
        Description desc = new Description();
        desc.setText(description);
        lineChart.setDescription(desc);
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

        lineChart.setNoDataText(noDataText);

    }

    /**
     * Gets you the json object with the graphs current visibility. Format is:
     *  {
     *      "name" : visibility (boolean)
     *  }
     *
     * @param preferences the preferences where graph visibility is stored
     * @return a jsonObject with all the graphs currently stored
     */
    public static JsonObject graphVisibility(SharedPreferences preferences) {
        JsonElement el = parser.parse(preferences.getString(AppConstants.STORED_GRAPH_DISPLAY, "{}"));
        if (el.isJsonArray()) {
            return new JsonObject();
        } else {
            return el.getAsJsonObject();
        }
    }

    /**
     * Gets you the visibility of a given graph
     *
     * @param preferences the shared preferences where graph visibility is stored
     * @param graphName the name of the graph you wish to get visibility of
     * @return true if the graph is visible, false if not
     */
    public static boolean graphVisibility(SharedPreferences preferences, String graphName) {
        JsonObject obj = graphVisibility(preferences);
        return obj != null && !obj.get(graphName).isJsonNull() && obj.get(graphName).getAsBoolean();
    }

    /**
     * Retrieves the first three letters of the given name
     *
     * @param name the name you wish to shorten
     * @return the three first letters of the name
     */
    public static String shortHand(String name) {
        return name.substring(0, 3);
    }

    /**
     * Gets you a list of all keys in a given jsonObject
     *
     * @param graphObject the object you wish to get keys from
     * @return a list with the names of all keys
     */
    public static List<String> sensorNames(JsonObject graphObject) {
        List<String> graphNames = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : graphObject.entrySet()) {
            graphNames.add(entry.getKey());
        }
        return graphNames;
    }

    /**
     * Updates a given graphs visibility in a chart
     *
     * @param lineChart the linechart you wish to update
     * @param graphName the graph you wish to update
     * @param visible its new visibility
     */
    public static void updateVisibility(LineChart lineChart, String graphName, boolean visible) {
        if (lineChart == null || lineChart.getData() == null) return;
        ILineDataSet dataSet = lineChart.getData().getDataSetByLabel(graphName, true);
        if (dataSet != null) {
            dataSet.setVisible(visible);
        }
    }
}