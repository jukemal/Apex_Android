package com.example.apex.ui.dashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.apex.R;
import com.example.apex.databinding.FragmentDashboardBinding;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import timber.log.Timber;

import static android.graphics.Color.RED;
import static android.graphics.Color.WHITE;
import static android.graphics.Color.rgb;

public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;

    private FragmentDashboardBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(LayoutInflater.from(requireContext()), container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        if (getArguments() != null) {
            final String device_name = getArguments().getString("DEVICE_NAME");
            final String device_mac = getArguments().getString("DEVICE_MAC");

            Timber.e("Details : " + device_mac + " , " + device_name);

            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(getResources().getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putString("DEVICE_NAME", device_name);
            editor.putString("DEVICE_MAC", device_mac);

            editor.apply();

            if (viewModel.setupViewModel(device_name, device_mac)) {
                setupCharts();
                viewModel.connect();
            } else {
                binding.connectButton.setEnabled(false);
                Toast.makeText(requireContext(), "Error setting up Bluetooth dash", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (viewModel.setupViewModel()) {
                setupCharts();
                viewModel.connect();
            } else {
                binding.connectButton.setEnabled(false);
                Toast.makeText(requireContext(), "Error setting up Bluetooth new", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupCharts() {
        viewModel.getConnectionStatus().observe(getViewLifecycleOwner(), this::onConnectionStatus);

        viewModel.getHumidityData().observe(getViewLifecycleOwner(), data -> addEntry(binding.humidityChart, data));
        viewModel.getTemperatureDate().observe(getViewLifecycleOwner(), data -> addEntry(binding.temperatureChart, data));
        viewModel.getGasData().observe(getViewLifecycleOwner(), data -> addEntry(binding.airQualityChart, data));
        viewModel.getCOData().observe(getViewLifecycleOwner(), data -> addEntry(binding.carbonMonoxideChart, data));

        viewModel.getCOLevel().observe(getViewLifecycleOwner(), isHigh -> {
            if (isHigh) {
                binding.headingWarningCoLevel.setVisibility(View.VISIBLE);
            } else {
                binding.headingWarningCoLevel.setVisibility(View.GONE);
            }
        });

        viewModel.getAirLevel().observe(getViewLifecycleOwner(), isHigh -> {
            if (isHigh) {
                binding.headingWarningGasLevel.setVisibility(View.VISIBLE);
            } else {
                binding.headingWarningGasLevel.setVisibility(View.GONE);
            }
        });
    }

    private void onConnectionStatus(DashboardViewModel.ConnectionStatus status) {
        switch (status) {
            case CONNECTED:
                binding.connectionText.setText("Status: Connected");
                binding.connectButton.setEnabled(true);
                binding.connectButton.setText("Disconnect");
                binding.connectButton.setOnClickListener(v -> viewModel.disconnect());
                initChart(binding.humidityChart);
                initChart(binding.temperatureChart);
                initChart(binding.carbonMonoxideChart);
                initChart(binding.airQualityChart);
                break;

            case CONNECTING:
                binding.connectionText.setText("Status: Connecting");
                binding.connectButton.setEnabled(false);
                binding.connectButton.setText("Connect");
                break;

            case DISCONNECTED:
                binding.connectionText.setText("Status: Disconnected");
                binding.connectButton.setEnabled(true);
                binding.connectButton.setText("Connect");
                binding.connectButton.setOnClickListener(v -> viewModel.connect());
                clearChart(binding.humidityChart);
                clearChart(binding.temperatureChart);
                clearChart(binding.carbonMonoxideChart);
                clearChart(binding.airQualityChart);
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
}