package com.thaleslima.android.stockhawk.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.thaleslima.android.stockhawk.R;
import com.thaleslima.android.stockhawk.data.Contract;
import com.thaleslima.android.stockhawk.data.PrefUtils;
import com.thaleslima.android.stockhawk.util.Utility;
import com.thaleslima.android.stockhawk.util.XAxisDateFormatter;
import com.thaleslima.android.stockhawk.util.YAxisPriceFormatter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String EXTRA_SYMBOL_DATA = "symbol-data";
    private static final int STOCK_LOADER = 0;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.stock_name)
    TextView stockName;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.symbol)
    TextView symbol;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.price)
    TextView price;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.change)
    TextView change;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.chart)
    public LineChart linechart;

    public static void navigate(Activity activity, String symbol) {
        Intent intent = new Intent(activity, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_SYMBOL_DATA, symbol);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        initToolbar();

        getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);
    }

    private void initToolbar() {
        setSupportActionBar(toolbar);

        if (null != getSupportActionBar()) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private String getSymbolExtra() {
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_SYMBOL_DATA)) {
            return getIntent().getStringExtra(EXTRA_SYMBOL_DATA);
        }

        return null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String symbol = getSymbolExtra();

        if (null != symbol) {
            Uri stockUri = Contract.Quote.makeUriForStock(symbol);
            return new CursorLoader(
                    this,
                    stockUri,
                    Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                    null,
                    null,
                    null
            );
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            initHeader(data);
            initGraph(data);
        }
    }

    private void initHeader(Cursor data) {
        DecimalFormat dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        DecimalFormat dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus.setPositivePrefix("+$");
        DecimalFormat percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
        percentageFormat.setPositivePrefix("+");

        stockName.setText(data.getString(Contract.Quote.POSITION_NAME));
        symbol.setText(data.getString(Contract.Quote.POSITION_SYMBOL));
        price.setText(dollarFormat.format(data.getFloat(Contract.Quote.POSITION_PRICE)));

        float rawAbsoluteChange = data.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
        float percentageChange = data.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

        if (rawAbsoluteChange > 0) {
            change.setBackgroundResource(R.drawable.percent_change_pill_green);
        } else {
            change.setBackgroundResource(R.drawable.percent_change_pill_red);
        }

        String changeValue = dollarFormatWithPlus.format(rawAbsoluteChange);
        String percentageValue = percentageFormat.format(percentageChange / 100);

        if (PrefUtils.getDisplayMode(this).equals(this.getString(R.string.pref_display_mode_absolute_key))) {
            change.setText(changeValue);
        } else {
            change.setText(percentageValue);
        }
    }

    private void initGraph(Cursor data) {
        Pair<Float, List<Entry>> pair = Utility.getFormattedStockHistory(data.getString(Contract.Quote.POSITION_HISTORY));

        List<Entry> dataPairs = pair.second;
        LineDataSet dataSet = new LineDataSet(dataPairs, "");
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setColor(Color.rgb(0, 200, 83));
        dataSet.setCircleColor(Color.WHITE);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        dataSet.setFillAlpha(65);
        dataSet.setFillColor(Color.rgb(0, 200, 83));
        dataSet.setHighLightColor(Color.rgb(0, 200, 83));
        dataSet.setDrawCircleHole(false);

        LineData lineData = new LineData(dataSet);
        lineData.setValueTextColor(Color.WHITE);

        linechart.setData(lineData);
        Float referenceTime = pair.first;

        XAxis xAxis = linechart.getXAxis();
        xAxis.setValueFormatter(new XAxisDateFormatter(referenceTime));
        xAxis.setDrawGridLines(false);
        xAxis.setAxisLineColor(Color.WHITE);
        xAxis.setAxisLineWidth(1.5f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setTextSize(12f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis yAxisRight = linechart.getAxisRight();
        yAxisRight.setEnabled(false);

        YAxis yAxis = linechart.getAxisLeft();
        yAxis.setValueFormatter(new YAxisPriceFormatter());
        yAxis.setDrawGridLines(false);
        yAxis.setAxisLineColor(Color.WHITE);
        yAxis.setAxisLineWidth(1.5f);
        yAxis.setTextColor(Color.WHITE);
        yAxis.setTextSize(12f);

        Legend l = linechart.getLegend();
        l.setEnabled(false);

        //disable all interactions with the graph
        linechart.setDragEnabled(false);
        linechart.setScaleEnabled(false);
        linechart.setDragDecelerationEnabled(false);
        linechart.setPinchZoom(false);
        linechart.setDoubleTapToZoomEnabled(false);
        Description description = new Description();
        description.setText(" ");
        linechart.setDescription(description);
        linechart.setExtraOffsets(10, 0, 0, 10);
        linechart.animateX(500, Easing.EasingOption.Linear);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
