package com.thaleslima.android.stockhawk.util;

import android.support.v4.util.Pair;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Utility {
    private Utility() {
    }

    public static Pair<Float, List<Entry>> getFormattedStockHistory(String history) {
        List<Entry> entries = new ArrayList<>();
        List<Float> timeData = new ArrayList<>();
        List<Float> stockPrice = new ArrayList<>();
        String[] dataPairs = history.split("\n");

        for (String pair : dataPairs) {
            String[] entry = pair.split(", ");
            timeData.add(Float.valueOf(entry[0]));
            stockPrice.add(Float.valueOf(entry[1]));
        }

        Collections.reverse(timeData);
        Collections.reverse(stockPrice);
        Float referenceTime = timeData.get(0);
        for (int i = 0; i < timeData.size(); i++) {
            entries.add(new Entry(timeData.get(i) - referenceTime, stockPrice.get(i)));
        }


        return new Pair<>(referenceTime, entries);
    }
}
