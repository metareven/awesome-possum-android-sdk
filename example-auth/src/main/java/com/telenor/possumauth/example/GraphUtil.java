package com.telenor.possumauth.example;

import android.graphics.Color;
import android.util.Log;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.Random;

public class GraphUtil {
    private static final String tag = GraphUtil.class.getName();
    public static int randomColor() {
        Random rand = new Random();
        int r = rand.nextInt(255);
        int g = rand.nextInt(255);
        int b = rand.nextInt(255);
        return Color.rgb(r, g, b);
    }

    public static LineDataSet lineDataSet(String graphName) {
        LineDataSet set = new LineDataSet(null, graphName);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setLineWidth(2f);
        set.setColor(colorWithName(graphName));
        set.setDrawValues(false);
        set.setDrawCircles(false);
        return set;
    }

    private static int colorWithName(String graphName) {
        if ("acc:def".equals(graphName)) {
            return Color.parseColor("#FF0000"); // Color.RED
        } else if ("ima:def".equals(graphName)) {
            return Color.parseColor("#0000FF"); // Color.BLUE
        } else if ("net:def".equals(graphName)) {
            return Color.YELLOW;
        } else if ("default".equals(graphName)) {
            return Color.rgb(244, 117, 117);
        } else if ("net:bin".equals(graphName)) {
            return Color.CYAN;
        } else if ("blu:def".equals(graphName)) {
            return Color.MAGENTA;
        } else if ("blu:bin".equals(graphName)) {
            return Color.parseColor("#661166");
        } else if ("gyr:def".equals(graphName)) {
            return Color.LTGRAY;
        } else if ("pos:def".equals(graphName)) {
            return Color.parseColor("#336688");
        } else if ("sou:def".equals(graphName)) {
            return Color.BLACK;
        } else if ("sou:cla".equals(graphName)) {
            return Color.parseColor("#00FF00"); // Color.GREEN
        } else {
            Log.i(tag, "AP: Unknown graphName for color:"+graphName);
            return randomColor();
        }
    }
}