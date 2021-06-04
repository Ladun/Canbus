package com.ssusp.canbus.tflite;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class BusNumberClassifier {

    private Net busNum;

    private BusNumberClassifier() {
    }

    public static BusNumberClassifier create(
            final Context context,
            final String modelFilename)
            throws IOException {
        final BusNumberClassifier d = new BusNumberClassifier();

        String numsPath = getPath(modelFilename, context);
        d.busNum = Dnn.readNetFromTensorflow(numsPath);
        return d;
    }

    // Upload file to storage and return a path.
    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i("BCV2", "Failed to upload a file");
        }
        return "";
    }



    public String recognizeImage(final Bitmap bitmap) {
        Mat numROI = new Mat();
        Utils.bitmapToMat(bitmap, numROI);
        Imgproc.cvtColor(numROI, numROI, Imgproc.COLOR_RGBA2RGB);

        Log.d("BCV2", numROI.size() + ", " + numROI.dims() + ", " + numROI.depth());

        Mat imageBlobNums = Dnn.blobFromImage(numROI, 0.00392, new Size(54, 54), new Scalar(0, 0, 0), true, false);

        java.util.List<Mat> resultNums = new java.util.ArrayList<Mat>(5);

        List<String> outBlobNamesNums = new java.util.ArrayList<>();
        outBlobNamesNums.add(0, "dLength/Softmax");
        outBlobNamesNums.add(1, "d1/Softmax");
        outBlobNamesNums.add(2, "d2/Softmax");
        outBlobNamesNums.add(3, "d3/Softmax");
        outBlobNamesNums.add(4, "d4/Softmax");

        busNum.setInput(imageBlobNums);
        busNum.forward(resultNums,outBlobNamesNums);

        Core.MinMaxLocResult lengthScores = Core.minMaxLoc(resultNums.get(0));
        Point lengthPoint = lengthScores.maxLoc;
        int length = (int)lengthPoint.x;

        String realNums = "";

        for (int j = 1; j <= length+1; ++j){

            Core.MinMaxLocResult numScores = Core.minMaxLoc(resultNums.get(j));
            Point numPoint = numScores.maxLoc;
            int num = (int)numPoint.x;

            if (num!=10){

                realNums=realNums + num;

            }

        }
        return realNums;
    }

}
