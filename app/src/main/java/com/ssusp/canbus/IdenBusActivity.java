package com.ssusp.canbus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.ssusp.canbus.R;

public class IdenBusActivity extends AppCompatActivity {

    //ImageView ImageView_busCam;
    Button Button_busFin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iden_bus);

        Button_busFin = findViewById(R.id.Button_busFin);

        //Button_busFin.setClickable(true);
        Button_busFin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(IdenBusActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

    }
}