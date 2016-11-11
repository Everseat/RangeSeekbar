package com.everseat.rangeseekbar.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.everseat.rangeseekbar.RangeSeekbar;
import com.everseat.rangeseekbar.Seekbar;

public class MainActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    final RangeSeekbar rangeSeekbar = (RangeSeekbar) findViewById(R.id.range_seekbar);
    final Seekbar seekbar = (Seekbar) findViewById(R.id.seekbar);
    Button resetButton = (Button) findViewById(R.id.reset_button);

    resetButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        seekbar.setCurrentValue(0);
        rangeSeekbar.setCurrentRange(0.5f, 1);
      }
    });
  }
}
