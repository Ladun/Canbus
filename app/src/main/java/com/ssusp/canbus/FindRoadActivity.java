package com.ssusp.canbus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.ssusp.canbus.R;
import com.google.android.material.textfield.TextInputEditText;

public class FindRoadActivity extends AppCompatActivity {

    TextInputEditText TextInputEditText_arrival, TextInputEditText_destination;
    Button Button_searchArr, Button_searchDes, Button_arrVoice, Button_desVoice, Button_searchRoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_road);

        TextInputEditText_arrival = findViewById(R.id.TextInputEditText_arrival);
        TextInputEditText_destination = findViewById(R.id.TextInputEditText_destination);

        Button_arrVoice = findViewById(R.id.Button_arrVoice);
        Button_desVoice = findViewById(R.id.Button_desVoice);

        Button_searchRoad = findViewById(R.id.Button_searchRoad);

        TextInputEditText_arrival.setClickable(true);
        TextInputEditText_arrival.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(com.ssusp.canbus.FindRoadActivity.this, SearchPlaceActivity.class);
                startActivity(intent);
            }
        });

        TextInputEditText_destination.setClickable(true);
        TextInputEditText_destination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(com.ssusp.canbus.FindRoadActivity.this, SearchPlaceActivity.class);
                startActivity(intent);
            }
        });



        //Button_searchRoad.setClickable(true);
        Button_searchRoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String arrival = TextInputEditText_arrival.getText().toString();
                String destination = TextInputEditText_destination.getText().toString();

                Intent intent = new Intent(com.ssusp.canbus.FindRoadActivity.this, FindRoadResultActivity.class);
                intent.putExtra("arrival", arrival);
                intent.putExtra("destination", destination);
                startActivity(intent);
            }

        });

/*
        Button_arrVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VoiceTask voiceTask = new VoiceTask();
                voiceTask.execute();
            }
        });

        Button_desVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VoiceTask voiceTask = new VoiceTask();
                voiceTask.execute();
            }
        });

    }

    public class VoiceTask extends AsyncTask<String, Integer, String> {
        String str = null;

        @Override
        protected String doInBackground(String... params) {
            try {
                getVoice();
            } catch (Exception e) {

            }
            return str;
        }

        @Override
        protected void onPostExecute(String result) {
            try {

            } catch (Exception e) {
                Log.d("onActivityResult", "getImageURL exception");
            }
        }
    }

    private void getVoice() {
        Intent intent = new Intent();
        intent.setAction(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        String language = "ko-KR";

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        startActivityForResult(intent, 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            ArrayList<String> results = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            String str = results.get(0);
            Toast.makeText(getBaseContext(), str, Toast.LENGTH_SHORT).show();

            TextView tv = findViewById(R.id.TextInputEditText_arrival);
            tv.setText(str);
        }
    }
*/
    }
}