package com.ssusp.canbus;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class VoiceSearchActivity extends AppCompatActivity {

    TextInputEditText TextInputEditText_voicePlace;
    Button Button_voiceSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_search);

        TextInputEditText_voicePlace = findViewById(R.id.TextInputEditText_voicePlace);
        Button_voiceSearch = findViewById(R.id.Button_voiceSearch);

        VoiceTask voiceTask = new VoiceTask();
        voiceTask.execute();

        Button_voiceSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String place = TextInputEditText_voicePlace.getText().toString();

                Intent intent = new Intent(com.ssusp.canbus.VoiceSearchActivity.this, SearchPlaceVActivity.class);
                intent.putExtra("place", place);
                startActivity(intent);
            }

        });
/*
        place = TextInputEditText_voicePlace.getText().toString();

        Intent intent = new Intent(com.ssusp.canbus.VoiceSearchActivity.this, SearchPlaceVActivity.class);
        intent.putExtra("place", place);

        if (place!=null)
            startActivity(intent);
*/


        if (savedInstanceState != null) {
            finish();
            return;
        }
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

            TextView tv = findViewById(R.id.TextInputEditText_voicePlace);
            tv.setText(str);
        }
    }

}