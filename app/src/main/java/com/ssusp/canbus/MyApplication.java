package com.ssusp.canbus;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import com.ssusp.canbus.tflite.BusNumberClassifier;
import com.ssusp.canbus.tflite.Classifier;
import com.ssusp.canbus.tflite.YoloV5Classifier;

import org.opencv.android.OpenCVLoader;

import java.io.IOException;

public class MyApplication extends Application {




    // Yolov5 Properties ------------------------
    public static final int TF_OD_API_INPUT_SIZE = 320;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "best-img320.tflite";
    private static final String TF_OD_API_LABELS_FILE = "label_map.txt";

    private YoloV5Classifier classifier;

    // BusNumber Properties ---------------------
    public static final int BUS_NUMBER_INPUT_SIZE = 54;
    private static final String BUS_NUMBER_MODEL_FILE = "busnum.pb";

    private BusNumberClassifier numberClassifier;

    private boolean modelLoaded;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public synchronized void modelLoad() throws IOException {
        // Load Bus Number Classifier
        classifier = YoloV5Classifier.create(
                getAssets(),
                TF_OD_API_MODEL_FILE,
                TF_OD_API_LABELS_FILE,
                TF_OD_API_IS_QUANTIZED,
                TF_OD_API_INPUT_SIZE);


        OpenCVLoader.initDebug();
        Log.d("LoadModel", "Finish Opencvloader");
        // Load Bus Number Classifier
        numberClassifier = BusNumberClassifier.create(this, BUS_NUMBER_MODEL_FILE);
        modelLoaded = true;

        Log.d("LoadModel", "LoadModel finished");
    }

    public synchronized boolean isModelLoaded(){
        return modelLoaded;
    }

    public BusNumberClassifier getBusNumberClassifier(){
        return numberClassifier;
    }

    public YoloV5Classifier getYoloClassifier(){
        return classifier;
    }
}
