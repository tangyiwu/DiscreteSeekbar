package com.android.tyw.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.android.tyw.library.DiscreteSeekBar;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DiscreteSeekBar seekBar = (DiscreteSeekBar) findViewById(R.id.discrete_seek_bar);
        final String[] data = new String[5];
        data[0] = "500";
        data[1] = "1000";
        data[2] = "2000";
        data[3] = "5000";
        data[4] = "10000";
        seekBar.setData(data);
        seekBar.setSelect(2);
        seekBar.setOnSeekBarSelectedListener(new DiscreteSeekBar.OnSeekBarSelectedListener() {
            @Override
            public void onSelect(int position) {
                Toast.makeText(MainActivity.this, data[position], Toast.LENGTH_SHORT).show();
            }
        });
    }
}
