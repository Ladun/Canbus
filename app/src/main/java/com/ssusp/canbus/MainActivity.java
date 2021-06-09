package com.ssusp.canbus;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ssusp.canbus.R;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    // Permissions
    public static final int REQUEST_CODE_PERMISSIONS = 1001;
    public final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    Button Button_findRoad, Button_idenBusNum;

    private HandlerThread handlerThread;

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
               Intent intent = new Intent(com.ssusp.canbus.MainActivity.this, PathFindActivity.class);
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


        if (!allPermissionGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        handlerThread = new HandlerThread("LoadModel");
        handlerThread.start();

        new Handler(handlerThread.getLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    ((MyApplication)getApplication()).modelLoad();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast toast =
                            Toast.makeText(
                                    getApplicationContext(), "Detector could not be initialized", Toast.LENGTH_SHORT);
                    toast.show();
                    finish();
                }
            }
        });
    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            Log.d("Check123",  "onRequestPermissionsResult : Check");

            if (!allPermissionGranted()) {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }



    private boolean allPermissionGranted(){
        for(String permission : REQUIRED_PERMISSIONS){
            Log.d("Check123", permission + ": Check");
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }
}