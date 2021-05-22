package com.example.tesseract;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class OCRActivity extends AppCompatActivity{
    TessBaseAPI tess;
    String dataPath="";
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dataPath = getFilesDir() + "/tessdata/";
        checkFile(new File(dataPath + "tessdata/"), "kor");
        //checkFile(new File(dataPath + "tesseract/"), "eng");

        String lang = "kor";
        tess = new TessBaseAPI();
        tess.init(dataPath, lang);

        //이미지를 비트맵으로 받아서 textview로 출력
        processImage(BitmapFactory.decodeResource(getResources(), R.drawable.test1));
    }
    public void processImage(Bitmap bitmap){
        Toast.makeText(getApplicationContext(),"checking...",Toast.LENGTH_LONG).show();
        String result=null;
        tess.setImage(bitmap);
        result=tess.getUTF8Text();
        TextView OCRTextview=(TextView)findViewById((R.id.textView));

        OCRTextview.setText(result);
    }
    private void copyFiles(String lang){
        try{
            String filepath=dataPath+"/tessdata/"+lang+".traineddata";

            AssetManager assetManager=getAssets();

            InputStream inStream=assetManager.open("/tessdata/"+lang+".traineddata");
            OutputStream outStream=new FileOutputStream(filepath);

            byte[] buffer=new byte[1024];
            int read;
            while((read=inStream.read(buffer))!=-1)
            {
                outStream.write(buffer,0,read);
            }
            outStream.flush();
            outStream.close();
        }catch(FileNotFoundException e) {
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void checkFile(File dir,String lang){
        if(!dir.exists()&&dir.mkdirs()){
            copyFiles(lang);
        }
        if(dir.exists()){
            String datafilePath= dataPath+"/tessdata/"+lang+".traineddata";
            File datafile=new File(datafilePath);
            if(!datafile.exists()){
                copyFiles(lang);
            }
        }
    }
}
