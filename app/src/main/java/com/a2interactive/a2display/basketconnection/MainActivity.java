package com.a2interactive.a2display.basketconnection;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;
import com.goka.kenburnsview.KenBurnsView;
import com.goka.kenburnsview.LoopViewPager;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.FaceDetector;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


/**
 * This is a very bare sample app to demonstrate the usage of the CameraDetector object from Affectiva.
 * It displays statistics on frames per second, percentage of time a face was detected, and the user's smile score.
 *
 * The app shows off the maneuverability of the SDK by allowing the user to start and stop the SDK and also hide the camera_affectiva_source SurfaceView.
 *
 * For use with SDK 2.02
 */
public class MainActivity extends Activity implements Detector.ImageListener, CameraDetector.CameraEventListener {

    /*PARAMETRES*/

    int seconds_before_detection = 0;
    int seconds_before_detection_welcome = 5;
    long seconds_before_disappear = 4000;

    int duration_welcome_popup=3500;

    //FIN



    final String LOG_TAG = "CameraDetectorDemo";

    ImageView left_camera;
    CameraSource mCameraSource = null;

    Bitmap finalScreenshot;

    Date lastDate;
    Date lastDateWelcome;



    SurfaceView cameraPreview;
    ImageView default_image;


    boolean isSDKStarted = false;

    RelativeLayout mainLayout;

    CameraDetector detector;
    CameraDetector.CameraType camera_affectiva_source;
    int camera_google_source;

    int previewWidth = 0;
    int previewHeight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Log.d(LOG_TAG,"le début");
        PackageManager pm = this.getPackageManager();
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.CAMERA);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            boolean frontCam, rearCam;

            //Must have a targetSdk >= 9 defined in the AndroidManifest
            frontCam = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
            rearCam = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
            if(frontCam || rearCam){
                setContentView(R.layout.activity_main);
                Log.d(LOG_TAG,"caméra found");
                if(frontCam){
                    Log.d(LOG_TAG,"front caméra found");
                    camera_affectiva_source =CameraDetector.CameraType.CAMERA_FRONT;
                    camera_google_source=CameraSource.CAMERA_FACING_FRONT;
                    //Toast.makeText(this,"J'ai détecté uen caméra avant",Toast.LENGTH_SHORT).show();
                    if(rearCam){
                        Log.d(LOG_TAG,"rear caméra found");
                        //Toast.makeText(this,"J'ai détecté une caméra arrière",Toast.LENGTH_SHORT).show();
                        camera_affectiva_source =CameraDetector.CameraType.CAMERA_BACK;
                        camera_google_source=CameraSource.CAMERA_FACING_BACK;
                    }

                }
                else{
                    Log.d(LOG_TAG,"rear caméra found");
                    camera_affectiva_source =CameraDetector.CameraType.CAMERA_BACK;
                    camera_google_source=CameraSource.CAMERA_FACING_BACK;
                    //Toast.makeText(this,"J'ai détecté une caméra arrière",Toast.LENGTH_SHORT).show();
                }
                lastDate = new Date();
                lastDateWelcome = new Date();

                left_camera = (ImageView) findViewById(R.id.left_camera);
                default_image = (ImageView) findViewById(R.id.default_image);

                left_camera.setScaleX(-1);

                //We create a custom SurfaceView that resizes itself to match the aspect ratio of the incoming camera_affectiva_source frames
                mainLayout = (RelativeLayout) findViewById(R.id.right_camera);
                cameraPreview = new SurfaceView(this) {
                    @Override
                    public void onMeasure(int widthSpec, int heightSpec) {
                        int measureWidth = MeasureSpec.getSize(widthSpec);
                        int measureHeight = MeasureSpec.getSize(heightSpec);
                        int width;
                        int height;
                        if (previewHeight == 0 || previewWidth == 0) {
                            width = measureWidth;
                            height = measureHeight;
                        } else {
                            float viewAspectRatio = (float) measureWidth / measureHeight;
                            float cameraPreviewAspectRatio = (float) previewWidth / previewHeight;

                            if (cameraPreviewAspectRatio > viewAspectRatio) {
                                width = measureWidth;
                                height = (int) (measureWidth / cameraPreviewAspectRatio);
                            } else {
                                width = (int) (measureHeight * cameraPreviewAspectRatio);
                                height = measureHeight;
                            }
                        }
                        setMeasuredDimension(width, height);
                    }
                };
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                cameraPreview.setLayoutParams(params);
                mainLayout.addView(cameraPreview, 0);

                detector = new CameraDetector(this, camera_affectiva_source, cameraPreview);
                detector.setMaxProcessRate(10);
                detector.setDetectAge(true);
                detector.setDetectGender(true);
                detector.setImageListener(this);
                detector.setOnCameraEventListener(this);

                initializeKenBurnsView();

                if (isSDKStarted) {
                    isSDKStarted = false;
                    stopDetector();
                } else {
                    isSDKStarted = true;
                    startDetector();
                }
            }
            else{
                Log.d(LOG_TAG,"pas de caméra");
                setContentView(R.layout.blank);
                //Toast.makeText(this,"Un problème est survenu lors de l'initialisaiton avec les caméra",Toast.LENGTH_SHORT).show();
            }

        }
        else{
            Log.d(LOG_TAG,"request permissons");
            setContentView(R.layout.blank);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 45);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isSDKStarted) {
            startDetector();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopDetector();
    }

    void startDetector() {
        if (!detector.isRunning()) {
            detector.start();
        }
    }

    void stopDetector() {
        if (detector.isRunning()) {
            detector.stop();
        }
    }

    @Override
    public void onImageResults(List<Face> list, Frame frame, float v) {
        if (list == null) {
            return;

        }


        if (list.size() == 0) {

        } else {

            Date now = new Date();
            long diffInMsWelcome = lastDateWelcome.getTime() - now.getTime();
            long diffInSecWelcome = TimeUnit.MILLISECONDS.toSeconds(diffInMsWelcome);



            long diffInMs = lastDate.getTime() - now.getTime();
            long diffInSec = TimeUnit.MILLISECONDS.toSeconds(diffInMs);
            if (diffInSec <= (0 - seconds_before_detection) && list.get(0).appearance.getGender() != Face.GENDER.UNKNOWN) {
                boolean done = false;

                Log.d(LOG_TAG, "Je tente une reconnaissance" + diffInSec);

                lastDate = now;
                Face face = list.get(0);

                switch (face.appearance.getAge()) {
                    case AGE_UNDER_18:
                        done = true;
                        setKenBurnsView("enfant");
                        break;
                }

                if (done == false) {
                    switch (face.appearance.getGender()) {
                        case MALE:
                            setKenBurnsView("homme");
                            break;
                        case FEMALE:
                            setKenBurnsView("femme");
                            break;
                    }

                }

                Bitmap faceBitmap = ImageHelper.getBitmapFromFrame(frame);
                finalScreenshot = Bitmap.createBitmap(faceBitmap.getWidth(), faceBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(finalScreenshot);
                Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

                canvas.drawBitmap(faceBitmap, 0, 0, paint);

                String timestamp = DateFormat.format("yyyy-MM-dd_hh-mm-ss", now).toString();
                File pictureFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AffdexMe");
                if (!pictureFolder.exists()) {
                    if (!pictureFolder.mkdir()) {
                        Log.e(LOG_TAG, "Unable to create directory: " + pictureFolder.getAbsolutePath());
                        return;
                    }
                }

                String screenshotFileName = timestamp + ".png";

                File screenshotFile = new File(pictureFolder, screenshotFileName);

                try {
                    ImageHelper.saveBitmapToFileAsPng(finalScreenshot, screenshotFile);
                    left_camera.setImageURI(Uri.parse(screenshotFile.getPath()));
                    String[] children = pictureFolder.list();
                    for (int i = 0; i < (children.length); i++) {
                        Log.d(LOG_TAG, children[i]);
                        if (Objects.equals(children[i], screenshotFileName) || Objects.equals(children[i], screenshotFile.getPath())) {

                        } else {
                            new File(pictureFolder, children[i]).delete();

                        }
                    }
                } catch (IOException e) {
                    String msg = "Unable to save screenshot";
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    Log.e(LOG_TAG, msg, e);
                }

                ImageHelper.addPngToGallery(getApplicationContext(), screenshotFile);

                left_camera.setImageURI(Uri.parse(screenshotFile.getPath()));

                String fileSavedMessage = "Screenshot saved to: " + screenshotFile.getPath();
                Log.d(LOG_TAG, fileSavedMessage);

                stopDetector();
                google_vision();


            }
            else{
                if (diffInSecWelcome <= (0 - seconds_before_detection_welcome) && lastDateWelcome!=now){
                    lastDateWelcome = now;
                    showImage(R.drawable.welcome_popup,duration_welcome_popup);
                }

            }
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void onCameraSizeSelected(int width, int height, Frame.ROTATE rotate) {
        if (rotate == Frame.ROTATE.BY_90_CCW || rotate == Frame.ROTATE.BY_90_CW) {
            previewWidth = height;
            previewHeight = width;
        } else {
            previewHeight = height;
            previewWidth = width;
        }
        cameraPreview.requestLayout();
    }

    private void initializeKenBurnsView() {
        // KenBurnsView
        final KenBurnsView kenBurnsView = (KenBurnsView) findViewById(R.id.ken_burns_view);
        kenBurnsView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // ResourceIDs
        List<Integer> resourceIDs = new ArrayList<>();
        resourceIDs.add(R.drawable.image_typo);

        kenBurnsView.initResourceIDs(resourceIDs);

        // LoopViewListener
        LoopViewPager.LoopViewPagerListener listener = new LoopViewPager.LoopViewPagerListener() {
            @Override
            public View OnInstantiateItem(int page) {
                TextView counterText = new TextView(getApplicationContext());
                //counterText.setText(String.valueOf(page));
                return counterText;
            }

            @Override
            public void onPageScroll(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                kenBurnsView.forceSelected(position);
            }

            @Override
            public void onPageScrollChanged(int page) {
            }
        };

        // LoopView
        LoopViewPager loopViewPager = new LoopViewPager(this, resourceIDs.size(), listener);

        FrameLayout viewPagerFrame = (FrameLayout) findViewById(R.id.view_pager_frame);
        viewPagerFrame.addView(loopViewPager);

        kenBurnsView.setPager(loopViewPager);

        kenBurnsView.setVisibility(View.GONE);
        viewPagerFrame.setVisibility(View.GONE);
        default_image.setVisibility(View.VISIBLE);


    }

    //imagePopup.viewPopup();
    public void showImage(int image_name,int duration) {
        final Dialog builder = new Dialog(this);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                //nothing;
            }
        });

        ImageView imageView = new ImageView(this);
        imageView.setImageResource(image_name);
        builder.addContentView(imageView, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        builder.show();

        final Handler handler  = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (builder.isShowing()) {
                    builder.dismiss();
                }
            }
        };
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeCallbacks(runnable);
            }
        });

        handler.postDelayed(runnable, duration);
    }

    private void setKenBurnsView(String type) {
        final KenBurnsView kenBurnsView = (KenBurnsView) findViewById(R.id.ken_burns_view);
        FrameLayout viewPagerFrame = (FrameLayout) findViewById(R.id.view_pager_frame);
        kenBurnsView.setVisibility(View.VISIBLE);
        viewPagerFrame.setVisibility(View.VISIBLE);
        default_image.setVisibility(View.GONE);
        // KenBurnsView
        kenBurnsView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // ResourceIDs
        List<Integer> resourceIDs = new ArrayList<>();
        if (Objects.equals(type, "enfant")) {
            resourceIDs.add(R.drawable.enfant_01);
            resourceIDs.add(R.drawable.enfant_02);
            resourceIDs.add(R.drawable.enfant_03);
            resourceIDs.add(R.drawable.enfant_04);
            resourceIDs.add(R.drawable.enfant_05);
            resourceIDs.add(R.drawable.enfant_06);
            resourceIDs.add(R.drawable.enfant_07);
            resourceIDs.add(R.drawable.enfant_08);
            resourceIDs.add(R.drawable.enfant_09);
            showImage(R.drawable.enfant_popup,4000);
        } else if (Objects.equals(type, "homme")) {
            resourceIDs.add(R.drawable.homme_01);
            resourceIDs.add(R.drawable.homme_02);
            resourceIDs.add(R.drawable.homme_03);
            resourceIDs.add(R.drawable.homme_04);
            resourceIDs.add(R.drawable.homme_05);
            resourceIDs.add(R.drawable.homme_06);
            resourceIDs.add(R.drawable.homme_07);
            resourceIDs.add(R.drawable.homme_08);
            resourceIDs.add(R.drawable.homme_09);
            resourceIDs.add(R.drawable.homme_10);
            resourceIDs.add(R.drawable.homme_11);
            resourceIDs.add(R.drawable.homme_12);
            resourceIDs.add(R.drawable.homme_13);
            resourceIDs.add(R.drawable.homme_14);
            resourceIDs.add(R.drawable.homme_15);
            resourceIDs.add(R.drawable.homme_16);
            resourceIDs.add(R.drawable.homme_17);
            resourceIDs.add(R.drawable.homme_18);
            resourceIDs.add(R.drawable.homme_19);
            resourceIDs.add(R.drawable.homme_20);
            resourceIDs.add(R.drawable.homme_21);
            resourceIDs.add(R.drawable.homme_22);
            resourceIDs.add(R.drawable.homme_23);
            resourceIDs.add(R.drawable.homme_24);
            showImage(R.drawable.homme_popup,4000);
        } else {//femme
            resourceIDs.add(R.drawable.femme_01);
            resourceIDs.add(R.drawable.femme_02);
            resourceIDs.add(R.drawable.femme_03);
            resourceIDs.add(R.drawable.femme_04);
            resourceIDs.add(R.drawable.femme_05);
            resourceIDs.add(R.drawable.femme_06);
            resourceIDs.add(R.drawable.femme_07);
            resourceIDs.add(R.drawable.femme_08);
            resourceIDs.add(R.drawable.femme_09);
            resourceIDs.add(R.drawable.femme_10);
            resourceIDs.add(R.drawable.femme_11);
            resourceIDs.add(R.drawable.femme_12);
            resourceIDs.add(R.drawable.femme_13);
            resourceIDs.add(R.drawable.femme_14);
            resourceIDs.add(R.drawable.femme_15);
            resourceIDs.add(R.drawable.femme_16);
            resourceIDs.add(R.drawable.femme_17);
            resourceIDs.add(R.drawable.femme_18);
            resourceIDs.add(R.drawable.femme_19);
            resourceIDs.add(R.drawable.femme_20);
            resourceIDs.add(R.drawable.femme_21);
            showImage(R.drawable.femme_popup,4000);
        }


        Collections.shuffle(resourceIDs);

        kenBurnsView.initResourceIDs(resourceIDs);

        // LoopViewListener
        LoopViewPager.LoopViewPagerListener listener = new LoopViewPager.LoopViewPagerListener() {
            @Override
            public View OnInstantiateItem(int page) {
                TextView counterText = new TextView(getApplicationContext());
                //counterText.setText(String.valueOf(page));
                return counterText;
            }

            @Override
            public void onPageScroll(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                kenBurnsView.forceSelected(position);
            }

            @Override
            public void onPageScrollChanged(int page) {
            }
        };

        // LoopView
        LoopViewPager loopViewPager = new LoopViewPager(this, resourceIDs.size(), listener);

        viewPagerFrame.addView(loopViewPager);

        kenBurnsView.setPager(loopViewPager);
    }

    private void google_vision() {

        Context context = getApplicationContext();
        FaceDetector google_detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(true)
                .build();

        google_detector.setProcessor(
                new MultiProcessor.Builder<>(new A2FaceTrackerFactory().addContext(this))
                        .build());

        if (!google_detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(LOG_TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, google_detector)
                .setAutoFocusEnabled(true)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(12.0f)
                .build();
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mCameraSource.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private class A2FaceTrackerFactory implements MultiProcessor.Factory<com.google.android.gms.vision.face.Face> {
        public MainActivity activity;
        public boolean man_detected=false;
        @Override
        public Tracker<com.google.android.gms.vision.face.Face> create(com.google.android.gms.vision.face.Face face) {
            return new A2FaceTracker(this);
        }

        public A2FaceTrackerFactory addContext(MainActivity activity){
            this.activity=activity;
            return this;
        }
    }

    /**
     * Face tracker for each detected individual. This will send to the API each new faces
     */
    private class A2FaceTracker extends Tracker<com.google.android.gms.vision.face.Face> {
        private String TAG = "A2FaceTracker";
        private A2FaceTrackerFactory factory;
        private boolean timer=false;
        public A2FaceTracker(A2FaceTrackerFactory factory) {
            super();
            this.factory=factory;
        }

        /**
         * Add a new face
         * @param i if of the face
         * @param face face itself
         */
        @Override
        public void onNewItem(int i, com.google.android.gms.vision.face.Face face) {
            Log.d(TAG,"New Face detected");
            factory.man_detected=true;
            super.onNewItem(i, face);
        }

        /**
         * Is called when the face is missing. We need to store this info somewhere
         */
        @Override
        public void onDone() {
            super.onDone();
            Log.d("tracker","j'ai perdu le visage");
            if(factory.man_detected==true && timer==false){
                factory.man_detected=false;
                timer=true;
                new MissingTask().execute();
            }
        }

        public void waitingFinished(){
            Log.d("Tracker","Je cherche à terminer la session");

            if(factory.man_detected==false){
                new DetectionTask().execute();
            }
            else{
                timer=false;
            }
        }

        private class MissingTask extends AsyncTask<Void, String, Void> {

            @Override
            protected Void doInBackground(Void... params) {
                long start=System.nanoTime();
                while((System.nanoTime()-start)<(seconds_before_disappear*1000000));
                return null;
            }

            @Override
            protected void onPreExecute() {
                Log.d("Task","Request: Detecting in image ");
            }

            @Override
            protected void onProgressUpdate(String... progress) {
            }

            @Override
            protected void onPostExecute(Void result) {
                waitingFinished();
            }
        }
    }

    private class DetectionTask extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            return null;
        }



        @Override
        protected void onPreExecute() {
            Log.d("Task","Request: Detecting in image ");
        }

        @Override
        protected void onProgressUpdate(String... progress) {
        }

        @Override
        protected void onPostExecute(Void result) {

            resultFromDetection();
        }
    }



    private void resultFromDetection() {
        mCameraSource.stop();
        showImage(R.drawable.goodbye,4000);
        startDetector();
        initializeKenBurnsView();
    }
}
