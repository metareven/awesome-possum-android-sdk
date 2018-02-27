package com.telenor.possumauth.example;

import android.graphics.Color;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.Random;

public class GraphUtil {
    public static int randomColor() {
        Random rand = new Random();
        int r = rand.nextInt(255);
        int g = rand.nextInt(255);
        int b = rand.nextInt(255);
        return Color.rgb(r, g, b);
    }

    public static LineDataSet lineDataSet(String graphName) {
        return lineDataSet(graphName, 0);
    }

    public static LineDataSet lineDataSet(String graphName, int definedColor) {
        LineDataSet set = new LineDataSet(null, graphName);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setLineWidth(2f);
        if (definedColor != 0) {
            set.setColor(definedColor);
        } else {
            set.setColor(randomColor());
        }
        set.setDrawValues(false);
        set.setDrawCircles(false);
        return set;
    }
}
