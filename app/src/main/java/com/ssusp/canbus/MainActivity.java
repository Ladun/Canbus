package com.ssusp.canbus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.ssusp.canbus.R;

public class MainActivity extends AppCompatActivity {

    Button Button_findRoad, Button_idenBusNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button_findRoad = findViewById(R.id.Button_findRoad);
        Button_idenBusNum = findViewById(R.id.Button_idenBusNum);

        Button_findRoad.setClickable(true);
        Button_findRoad.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               Intent intent = new Intent(com.ssusp.canbus.MainActivity.this, FindRoadActivity.class);
               startActivity(intent);
           }
        });

        Button_idenBusNum.setClickable(true);
        Button_idenBusNum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(com.ssusp.canbus.MainActivity.this, IdenBusActivity.class);
                startActivity(intent);
            }
        });
    }
}