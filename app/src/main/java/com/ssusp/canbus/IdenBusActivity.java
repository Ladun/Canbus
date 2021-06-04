package com.ssusp.canbus;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ssusp.canbus.R;
import com.ssusp.canbus.customview.AutoFitTextureView;
import com.ssusp.canbus.customview.OverlayView;
import com.ssusp.canbus.env.ImageUtils;
import com.ssusp.canbus.tflite.BusInformation;
import com.ssusp.canbus.tflite.BusNumberClassifier;
import com.ssusp.canbus.tflite.Classifier;
import com.ssusp.canbus.tflite.MultiBoxTracker;
import com.ssusp.canbus.tflite.YoloV5Classifier;

import org.opencv.android.OpenCVLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class IdenBusActivity extends AppCompatActivity
        implements ImageReader.OnImageAvailableListener {

    //ImageView ImageView_busCam;

    private static final String TAG = "MainActivity";

    // Camera Properties ------------------------
    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    // Camera View
    private AutoFitTextureView textureView;
    Button Button_busFin;

    // Camera Setting variables
    private String cameraId;
    private final Semaphore cameraOpenCloseLock = new Semaphore(1);
    private CameraDevice cameraDevice;
    private ImageReader previewReader;
    private CameraCaptureSession captureSession;
    private CaptureRequest previewRequest;
    private CaptureRequest.Builder previewRequestBuilder;

    private Size previewSize;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private Handler handler;
    private HandlerThread handlerThread;

    // Detection variables
    private Integer sensorOrientation;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;

    private boolean computingDetection = false;
    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    // Detected info variables
    private List<BusInformation> mBusInformations;


    // Camera Listener Callback
    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            try {
                openCamera(width, height);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            try {
                createCameraPreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            //super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            //super.onCaptureCompleted(session, request, result);
        }
    };


    // Yolov5 Properties ------------------------
    private static final int TF_OD_API_INPUT_SIZE = 320;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "best-img320.tflite";
    private static final String TF_OD_API_LABELS_FILE = "label_map.txt";

    private Classifier classifier;
    private Bitmap origin;

//    private MultiBoxTracker tracker;
//    OverlayView trackingOverlay;

    // BusNumber Properties ---------------------
    private static final int BUS_NUMBER_INPUT_SIZE = 54;
    private static final String BUS_NUMBER_MODEL_FILE = "busnum.pb";
    private BusNumberClassifier numberClassifier;
    private Bitmap numCroppedBitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iden_bus);

        OpenCVLoader.initDebug();

        textureView = findViewById(R.id.textureView);

        Button_busFin = findViewById(R.id.Button_busFin);
        //Button_busFin.setClickable(true);
        Button_busFin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(IdenBusActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        startCamera();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        startBackgroundThread();

        if(textureView.isAvailable()){
            try {
                openCamera(textureView.getWidth(), textureView.getHeight());
            }
            catch (CameraAccessException e){
                e.printStackTrace();
            }
        }
        else{
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    public synchronized void onPause() {

        closeCamera();
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        }
        catch (final InterruptedException e) {
            e.printStackTrace();
        }

        try{
            stopBackgroundThread();
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
        super.onPause();
    }

    // ------------------------------------- Bus Number Functions -------------------------------------

    public String processBusNum(Bitmap bitmap){
        Matrix frameToNumCropTransform =
                ImageUtils.getTransformationMatrix(
                        bitmap.getWidth(), bitmap.getHeight(),
                        BUS_NUMBER_INPUT_SIZE, BUS_NUMBER_INPUT_SIZE,
                        sensorOrientation, false);

        final Canvas canvas = new Canvas(numCroppedBitmap);
        canvas.drawBitmap(bitmap, frameToNumCropTransform, null);

        String result = numberClassifier.recognizeImage(numCroppedBitmap);

        return result;
    }
    // ------------------------------------- Detect Info Functions -------------------------------------


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(event.getAction() == MotionEvent.ACTION_UP){
            BusInfoSpeech();
        }

        return true;
    }

    public void BusInfoSpeech(){
        if(mBusInformations != null && mBusInformations.size() == 0)
            return;

        List<BusInformation> busInformations = mBusInformations;

        Collections.sort(busInformations, new Comparator<BusInformation>() {
            @Override
            public int compare(BusInformation t0, BusInformation t1) {
                return Float.compare(t0.getBusLocationArea(), t1.getBusLocationArea());
            }
        });

        // "XX, XX 버스가 있습니다."
        StringBuilder numberSpeech = new StringBuilder();
        for(int i = busInformations.size() - 1; i >= 0; i--){
            BusInformation busInfo = busInformations.get(i);

            numberSpeech.append(busInfo.getBusNumber());
            if(i != 0)
                numberSpeech.append(", ");
        }
        numberSpeech.append(" 버스가 있습니다.");

        // "xx 버스 뒷문이 더 가까이 있습니다."
        // 문에 대한 정보는 가장 가까이 있는 버스만 알려줌
        String doorSpeech = busInformations.get(busInformations.size() - 1).getBusDoorSpeech();

        // TODO: TTS 실행
        Log.d(TAG +"_SPEECH", numberSpeech.toString());
        if(doorSpeech.length() != 0) {
            Log.d(TAG + "_SPEECH", doorSpeech);
        }
    }

    // ------------------------------------- Detect Functions -------------------------------------
    @Override
    public void onImageAvailable(final ImageReader reader) {
        if(previewSize.getWidth() == 0 || previewSize.getHeight() == 0){
            return;
        }

        if (rgbBytes == null) {
            rgbBytes = new int[previewSize.getWidth() * previewSize.getHeight()];
        }
        try{
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            final Image.Plane[] planes = image.getPlanes();
            ImageUtils.fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    new Runnable() {
                        @Override
                        public void run() {
                            ImageUtils.convertYUV420ToARGB8888(
                                    yuvBytes[0],
                                    yuvBytes[1],
                                    yuvBytes[2],
                                    previewSize.getWidth(),
                                    previewSize.getHeight(),
                                    yRowStride,
                                    uvRowStride,
                                    uvPixelStride,
                                    rgbBytes);
                        }
                    };

            postInferenceCallback =
                    new Runnable() {
                        @Override
                        public void run() {
                            image.close();
                            isProcessingFrame = false;
                        }
                    };

            processImage();
        }
        catch (final Exception e) {
            e.printStackTrace();
            return;
        }

    }

    public void processImage(){
        if(computingDetection)
        {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewSize.getWidth(), 0, 0, previewSize.getWidth(), previewSize.getHeight());

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);


        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        final long startTime = SystemClock.uptimeMillis();
                        final List<Classifier.Recognition> results = classifier.recognizeImage(croppedBitmap);

                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        mBusInformations = processResult(results, rgbFrameBitmap);

                        computingDetection = false;
                    }
                });

    }

    private List<BusInformation>  processResult(final List<Classifier.Recognition> results, Bitmap origin) {

        Log.d(TAG + "_Recog", "Count: " + results.size());
        List<BusInformation> busInformations = new ArrayList<>();
        for (int i = results.size() - 1; i >= 0; i--) {
            if (results.get(i).getDetectedClass() == 0) {
                busInformations.add(new BusInformation(results.get(i).getLocation()));
                results.remove(i);
            }
        }

        // DetectedClass 값을 기준으로 오름차순 정렬
        Collections.sort(results, new Comparator<Classifier.Recognition>() {
            @Override
            public int compare(Classifier.Recognition t1, Classifier.Recognition t2) {
                return Integer.compare(t1.getDetectedClass(), t2.getDetectedClass());
            }
        });

        Matrix rotateMatrix = new Matrix();
        rotateMatrix.postRotate(90);
        if (busInformations.size() > 0) {
            for (Classifier.Recognition r : results) {
                int idx = 0;
                float minFitness = Float.MAX_VALUE;

                for (int j = 0; j < busInformations.size(); j++) {

                    float f = busInformations.get(j).locationFitness(r);
                    if (f < minFitness) {
                        idx = j;
                        minFitness = f;
                    }
                }
                if (r.getDetectedClass() <= 2) {
                    busInformations.get(idx).addDoor(r);
                } else {
                    final RectF location = r.getLocation();
                    cropToFrameTransform.mapRect(location);

                    int l = (int)Math.max(0, location.left - 10);
                    int t = (int)Math.max(0, location.top - 10);
                    int w = (int)Math.min(origin.getWidth(), location.width() + 20);
                    int h = (int)Math.min(origin.getHeight(), location.height() + 20);

                    Bitmap numberROI = android.graphics.Bitmap.createBitmap(origin, l, t, w, h, rotateMatrix, true);
                    String number = processBusNum(numberROI);
                    busInformations.get(idx).addNumber(number);
                }
            }
        }


        return busInformations;
    }

    public void onPreviewSizeChosen(final Size size, final int rotation){

        try {
            classifier = YoloV5Classifier.create(
                    getAssets(),
                    TF_OD_API_MODEL_FILE,
                    TF_OD_API_LABELS_FILE,
                    TF_OD_API_IS_QUANTIZED,
                    TF_OD_API_INPUT_SIZE);

        } catch (IOException e) {
            e.printStackTrace();
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Detector could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
        try {
            numberClassifier = BusNumberClassifier.create(this, BUS_NUMBER_MODEL_FILE);

        } catch (IOException e) {
            e.printStackTrace();
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Detector could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }


        sensorOrientation = rotation - getScreenOrientation();

        rgbFrameBitmap = android.graphics.Bitmap.createBitmap(size.getWidth(), size.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        croppedBitmap = android.graphics.Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, android.graphics.Bitmap.Config.ARGB_8888);
        numCroppedBitmap = android.graphics.Bitmap.createBitmap(BUS_NUMBER_INPUT_SIZE, BUS_NUMBER_INPUT_SIZE, android.graphics.Bitmap.Config.ARGB_8888);

        Log.d("Rotation", sensorOrientation + ", " + rotation + ", " + getScreenOrientation());
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        size.getWidth(), size.getHeight(),
                        TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                        sensorOrientation, false);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);


    }

    private void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    private int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    private int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    // ------------------------------------- Camera Functions -------------------------------------

    private void startBackgroundThread(){
        mBackgroundThread = new HandlerThread("CameraPreview");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void startCamera(){
        textureView.setSurfaceTextureListener(textureListener);
    }

    private void openCamera(int width, int height) throws CameraAccessException, NullPointerException {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        cameraId = manager.getCameraIdList()[0];
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
        int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        boolean swappedDimensions = false;
        switch (displayRotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true;
                }
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true;
                }
                break;
            default:
                Log.e(TAG, "Display rotation is invalid: " + displayRotation);
        }

        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        int rotatedPreviewWidth = width;
        int rotatedPreviewHeight = height;
        int maxPreviewWidth = displaySize.x;
        int maxPreviewHeight = displaySize.y;

        if (swappedDimensions) {
            rotatedPreviewWidth = height;
            rotatedPreviewHeight = width;
            maxPreviewWidth = displaySize.y;
            maxPreviewHeight = displaySize.x;
        }

        if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
            maxPreviewWidth = MAX_PREVIEW_WIDTH;
        }

        if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
            maxPreviewHeight = MAX_PREVIEW_HEIGHT;
        }

        previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                maxPreviewHeight, largest);
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
        } else {
            textureView.setAspectRatio(
                    previewSize.getHeight(), previewSize.getWidth());
        }

        onPreviewSizeChosen(previewSize, sensorOrientation);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            manager.openCamera(cameraId, stateCallback, null);
        } else {
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
    int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e("Camera2", "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private void createCameraPreview() throws CameraAccessException {

        SurfaceTexture texture = textureView.getSurfaceTexture();
        assert texture != null;

        texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface surface = new Surface(texture);

        previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        previewRequestBuilder.addTarget(surface);

        previewReader =
                ImageReader.newInstance(
                        previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 2);

        previewReader.setOnImageAvailableListener(this, mBackgroundHandler);
        previewRequestBuilder.addTarget(previewReader.getSurface());

        cameraDevice.createCaptureSession(
                Arrays.asList(surface, previewReader.getSurface()),
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        if (cameraDevice == null) {
                            return;
                        }

                        captureSession = session;
                        try {
                            // Auto focus should be continuous for camera preview.
                            previewRequestBuilder.set(
                                    CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            // Flash is automatically enabled when necessary.
                            previewRequestBuilder.set(
                                    CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                            // Finally, we start displaying the camera preview.
                            previewRequest = previewRequestBuilder.build();
                            captureSession.setRepeatingRequest(
                                    previewRequest,
                                    captureCallback, mBackgroundHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Toast.makeText(getApplicationContext(), "Configuration Changed", Toast.LENGTH_LONG).show();
                    }
                }, null);
    }

    protected void stopBackgroundThread() throws InterruptedException {
        mBackgroundThread.quitSafely();
        mBackgroundThread.join();
        mBackgroundThread = null;
        mBackgroundHandler = null;
    }

    private void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            if (null != captureSession) {
                captureSession.close();
                captureSession = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != previewReader) {
                previewReader.close();
                previewReader = null;
            }
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            cameraOpenCloseLock.release();
        }
    }
}