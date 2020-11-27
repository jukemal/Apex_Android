// Dakshina

package com.example.apex.ui.dashboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.apex.databinding.FragmentDashboardBinding;
import com.example.apex.services.BluetoothService;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import static android.graphics.Color.RED;
import static android.graphics.Color.WHITE;
import static android.graphics.Color.rgb;

public class DashboardFragment extends Fragment {

    public final static String Dashboard_Data = "Dashboard_Data";

    private final static float CO_LEVEL_THRESHOLD = 400;
    private final static float AIR_LEVEL_THRESHOLD = 350;

    private FragmentDashboardBinding binding;

    private BluetoothReceiver bluetoothReceiver;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(LayoutInflater.from(requireContext()), container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        bluetoothReceiver = new BluetoothReceiver();

        initChart(binding.humidityChart);
        initChart(binding.temperatureChart);
        initChart(binding.carbonMonoxideChart);
        initChart(binding.airQualityChart);
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(bluetoothReceiver,
                new IntentFilter(BluetoothService.BLUETOOTHBROADCAST));
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(bluetoothReceiver,
                new IntentFilter(BluetoothService.BLUETOOTHBROADCAST));
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(bluetoothReceiver);
        super.onPause();
    }

    private void init(String message) {
        String[] temp = message.split(":");

        switch (temp[0]) {
            case "H":
                try {
                    addEntry(binding.humidityChart, Float.parseFloat(temp[1]));
                } catch (Exception e) {
                    addEntry(binding.humidityChart, 0f);
                }
                break;
            case "T":
                try {
                    addEntry(binding.temperatureChart, Float.parseFloat(temp[1]));
                } catch (Exception e) {
                    addEntry(binding.temperatureChart, 0f);
                }
                break;
            case "C":
                try {
                    addEntry(binding.carbonMonoxideChart, Float.parseFloat(temp[1]));

                    if (CO_LEVEL_THRESHOLD < Float.parseFloat(temp[1])) {
                        binding.headingWarningCoLevel.setVisibility(View.VISIBLE);
                    } else {
                        binding.headingWarningCoLevel.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    addEntry(binding.carbonMonoxideChart, 0f);
                    binding.headingWarningCoLevel.setVisibility(View.GONE);
                }
                break;
            case "G":
                try {
                    addEntry(binding.airQualityChart, Float.parseFloat(temp[1]));

                    if (AIR_LEVEL_THRESHOLD < Float.parseFloat(temp[1])) {
                        binding.headingWarningGasLevel.setVisibility(View.VISIBLE);
                    } else {
                        binding.headingWarningGasLevel.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    addEntry(binding.airQualityChart, 0f);
                    binding.headingWarningGasLevel.setVisibility(View.GONE);
                }
                break;
        }
    }

    private void initChart(LineChart chart) {
        chart.setTouchEnabled(true);

        chart.getDescription().setEnabled(false);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(true);

        // set an alternative background color
        chart.setBackgroundColor(WHITE);

        LineData data = new LineData();
        data.setValueTextColor(RED);

        // add empty data
        chart.setData(data);

        XAxis xl = chart.getXAxis();
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);
        xl.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return Integer.toString((int) value * 5);
            }

        });

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    // adding data to chart.
    private void addEntry(LineChart chart, float entry) {

        LineData data = chart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), entry), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            chart.notifyDataSetChanged();

            // limit the number of visible entries
            chart.setVisibleXRangeMaximum(10);
            // chart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            chart.moveViewToX(data.getEntryCount());
        }
    }

    // Create data set.
    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(RED);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(rgb(244, 117, 117));
        set.setValueTextColor(WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

    private void clearChart(LineChart chart) {
        chart.clear();
    }

    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            init(intent.getStringExtra(Dashboard_Data));
        }
    }
}