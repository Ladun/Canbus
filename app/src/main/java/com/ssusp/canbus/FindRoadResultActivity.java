package com.ssusp.canbus;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.ssusp.canbus.R;

import java.util.ArrayList;
import java.util.List;

public class FindRoadResultActivity extends AppCompatActivity {

    private ListView ListView_roadList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_road_result);

        ListView_roadList = (ListView)findViewById(R.id.ListView_roadList);

        List<String> data = new ArrayList<>();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
    }
}