package com.telenor.possumauth.example;

import android.content.SharedPreferences;
import android.graphics.Color;

import com.github.mikephil.charting.charts.LineChart;
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

    public static LineDataSet lineDataSet(String graphName) {
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
        set.addEntry(new Entry(graphPos, value));
        data.notifyDataChanged();
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

    public static JsonObject graphVisibility(SharedPreferences preferences) {
        JsonElement el = parser.parse(preferences.getString(AppConstants.STORED_GRAPH_DISPLAY, "{}"));
        if (el.isJsonArray()) {
            return new JsonObject();
        } else {
            return el.getAsJsonObject();
        }
    }

    public static String shortHand(String name) {
        return name.substring(0, 3);
    }

    public static List<String> sensorNames(JsonObject graphObject) {
        List<String> graphNames = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : graphObject.entrySet()) {
            graphNames.add(entry.getKey());
        }
        return graphNames;
    }

    public static void updateVisibility(LineChart lineChart, String graphName, boolean visible) {
        if (lineChart != null && lineChart.getData() != null) {
            ILineDataSet dataSet = lineChart.getData().getDataSetByLabel(graphName, true);
            if (dataSet != null) {
                dataSet.setVisible(visible);
            }
        }
    }
}