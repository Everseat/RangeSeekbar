package com.everseat.rangeseekbar.demo;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.everseat.rangeseekbar.RangeSeekbar;

public class MainActivity extends AppCompatActivity {
  private RangeSeekbar rangeSeekbar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    rangeSeekbar = (RangeSeekbar) findViewById(R.id.range_seekbar);
    rangeSeekbar.setTrackFillColor(Color.CYAN);
    rangeSeekbar.setTrackColor(Color.argb(102, 0, 0, 0));
    rangeSeekbar.setMinLabelText(R.string.label_am);
    rangeSeekbar.setMaxLabelText(R.string.label_pm);
    rangeSeekbar.setValueFormatter(new RangeSeekbar.ValueFormatter() {
      @Override
      public String formatValue(float value) {
        int hour = (int) (value * 23);
        String amOrPm = hour >= 12 ? "PM" : "AM";
        if (hour > 12) {
          hour -= 12;
        }
        if (hour == 0) {
          hour = 12;
        }
        return hour + amOrPm;
      }
    });
  }
}
