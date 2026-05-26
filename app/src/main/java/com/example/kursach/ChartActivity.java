package com.example.kursach;

import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

public class ChartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);
        LineChart distance_line = findViewById(R.id.distance_line);
        LineChart humanity_line = findViewById(R.id.humanity_line);

        // Связываем график напрямую со списком из MainActivity
        LineDataSet distanceDataSet = new LineDataSet(MainActivity.distanceList, "Дистанция");

        LineDataSet humanityDataSet = new LineDataSet(MainActivity.humanityList, "Влажность");

        // Стиль синей линии с красными точками
        distanceDataSet.setColor(Color.BLUE);
        distanceDataSet.setCircleColor(Color.RED);
        distanceDataSet.setLineWidth(2f);
        distanceDataSet.setCircleRadius(2f);
        distanceDataSet.setDrawValues(false);

        humanityDataSet.setColor(Color.BLUE);
        humanityDataSet.setCircleColor(Color.RED);
        humanityDataSet.setLineWidth(2f);
        humanityDataSet.setCircleRadius(2f);
        humanityDataSet.setDrawValues(false);

        //Загружаем данные в график
        distance_line.setData(new LineData(distanceDataSet));

        humanity_line.setData(new LineData(humanityDataSet));

        //настройка оси времени
        XAxis xAxis =  distance_line.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return ((int) value * 10) + " сек";
            }
        });

        //настройка оси расстояния
        YAxis leftAxis = distance_line.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(300f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + " см";
            }
        });

        XAxis xAxisH =  humanity_line.getXAxis();
        xAxisH.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisH.setGranularity(1f);
        xAxisH.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return ((int) value * 10) + " сек";
            }
        });

        //настройка оси расстояния
        YAxis leftAxisH = humanity_line.getAxisLeft();
        leftAxisH.setAxisMinimum(0f);
        leftAxisH.setAxisMaximum(100f);
        leftAxisH.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + " %";
            }
        });

        // Выключаем лишний мусор библиотеки
        distance_line.getAxisRight().setEnabled(false);
        distance_line.getDescription().setEnabled(false);

        humanity_line.getAxisRight().setEnabled(false);
        humanity_line.getDescription().setEnabled(false);

        // Сдвигаем экран к самой последней точке и рисуем
        if (MainActivity.distanceList.size() > 0) {
            distance_line.moveViewToX(MainActivity.distanceList.size());
        }
        distance_line.invalidate();

        if (MainActivity.humanityList.size() > 0) {
            humanity_line.moveViewToX(MainActivity.humanityList.size());
        }
        humanity_line.invalidate();
    }
}
