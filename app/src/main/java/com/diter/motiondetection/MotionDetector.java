package com.diter.motiondetection;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class MotionDetector {
    Camera.Size Gsize;
    class MotionDetectorThread extends Thread {
        private AtomicBoolean isRunning = new AtomicBoolean(true);

        public void stopDetection() {
            isRunning.set(false);
        }

        @Override
        public void start() {
            super.start();
            Log.d("MyTag", "MotionDetectorThread Start");
        }

        @Override
        public void run() {
            try {

                Log.d("MyTag", "MotionDetectorThread.Run isRunning " + isRunning.toString());
                while (isRunning.get()) {
                    long now = System.currentTimeMillis();
                    //Log.d("MyTag","MotionDetectorThread.Run now " + now + "lastCheck " + lastCheck + " checkInterval " + checkInterval);
                    //Log.d("MyTag", "MD before now check");
                    if (now - lastCheck > checkInterval) {
                        lastCheck = now;
                        //Log.d("MyTag", "MD nextdata null check");
                        if (nextData.get() != null) {
                            int[] img = ImageProcessing.decodeYUV420SPtoLuma(nextData.get(), nextWidth.get(), nextHeight.get());
                            //Log.d("MyTag","after ImageProcessing");

                            // check if it is too dark
                            int lumaSum = 0;
                            for (int i : img) {
                                lumaSum += i;
                            }
                            //Log.d("MyTag", "MD lumaSum check");
                            if (lumaSum < minLuma) {
                                if (motionDetectorCallback != null) {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            motionDetectorCallback.onTooDark();
                                        }
                                    });
                                }
                            } else if (detector.detect(img, nextWidth.get(), nextHeight.get())) {
                                // check
                                if (motionDetectorCallback != null) {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            motionDetectorCallback.onMotionDetected(nextData.get());
                                            //new SavePhotoTask().execute(nextData.get());
                                        }
                                    });
                                    //new SavePhotoTask().execute(nextData.get());

                                    /*
                                    try {
                                        //////////////////////////////////////////////////////////////////////
                                        //startService(new Intent(getActivity().getApplicationContext(), PhotoTakingService.class));
                                        //new SavePhotoTask().execute(nextData.get());


                                        releaseCamera();
                                        Log.d("MyTag", "after releaseCamera");
                                        mCamera = getCameraInstanceBack();
                                        Log.d("MyTag", "after getCameraInstanceBack Flash on");
                                        mCamera.stopPreview();
                                        Log.d("MyTag", "after stopPreview Flash on");
                                        Camera.Parameters parameters = mCamera.getParameters();
                                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                                        mCamera.setParameters(parameters);
                                        mCamera.setPreviewCallback(previewCallback);
                                        try {
                                            mCamera.setPreviewDisplay(previewHolder);
                                        } catch (Exception e) {
                                            Log.d("MyTag", "Error starting camera preview: " + e.getMessage());
                                        }
                                        ;
                                        mCamera.startPreview();

                                        try {
                                            Thread.sleep(1);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        Log.d("MyTag", "Flash off");
                                        mCamera.stopPreview();
                                        releaseCamera();
                                        Log.d("MyTag", "after releaseCamera");
                                        mCamera = getCameraInstance();
                                        Log.d("MyTag", "MD after getCameraInstance");

                                        parameters = mCamera.getParameters();
                                        Log.d("MyTag", "MD after getParameters");
                                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                                        Log.d("MyTag", "MD after setFlashMode");
                                        mCamera.setParameters(parameters);
                                        Log.d("MyTag", "MD after setParameters");
                                        mCamera.setPreviewCallback(previewCallback);
                                        Log.d("MyTag", "MD after setPreviewCallback");
                                        try {
                                            mCamera.setPreviewDisplay(previewHolder);
                                            Log.d("MyTag", "MD after setPreviewDisplay");
                                        } catch (Exception e) {
                                            Log.d("MyTag", "Error starting camera preview: " + e.getMessage());
                                        }
                                        ;
                                        mCamera.startPreview();
                                        Log.d("MyTag", "MD after startPreview");

                                    } catch (Exception ex) {
                                        Log.d("MyTag", " error flash " + ex.toString());
                                    }
                                    */

                                    //////////////////////////////////////////////////////////////////////
                                    //Log.d("MyTag", "MD 1");
                                }
                                //Log.d("MyTag", "MD 2");
                            }
                            //Log.d("MyTag", "MD 3");
                        }
                        //Log.d("MyTag", "MD 4");
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.d("MyTag", "MD Exception");
                    }
                }
            }
            catch (Exception ex) {
                Log.d("MyTag", "MD run exception " + ex.toString());
            }
        Log.d("MyTag", "MD run ends");
        }
    }
    //private SurfaceViewContainer mSurfaceViewContainer
    private final AggregateLumaMotionDetection detector;
    private long checkInterval = 500;
    private long lastCheck = 0;
    private MotionDetectorCallback motionDetectorCallback;
    private Handler mHandler = new Handler();

    private AtomicReference<byte[]> nextData = new AtomicReference<>();
    private AtomicInteger nextWidth = new AtomicInteger();
    private AtomicInteger nextHeight = new AtomicInteger();
    private int minLuma = 1000;
    private MotionDetectorThread worker;

    private Camera mCamera;
    private boolean inPreview;
    private SurfaceHolder previewHolder;
    private Context mContext;
    private SurfaceView mSurface;
    private String pwd;
    private String emailFrom;
    private String emailTo;


    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onPreviewFrame(byte[] data, Camera cam) {
            //Log.d("MyTag"," onPreviewFrame started");
            if (data == null) return;
            Camera.Size size = cam.getParameters().getPreviewSize();
            if (size == null) return;

            consume(data, size.width, size.height);
            Gsize = size;
            //Log.d("MyTag"," onPreviewFrame after consume");
        }
    };

    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d("MyTag", "surfaceCreated");
            if (mCamera == null) {
                Log.d("MyTag", "mCamera null" );
                mCamera = getCameraInstance();
            }
            try {
                mCamera.setPreviewDisplay(previewHolder);
                Log.d("MyTag", "after setPreviewDisplay" );
                mCamera.setPreviewCallback(previewCallback);
                Log.d("MyTag", "after setPreviewCallback" );
                //surfaceChanged(holder,4,255,255);
                Log.d("MyTag", "after surfaceChanged" );
            } catch (Throwable t) {
                Log.d("MyTag", "Exception in setPreviewDisplay()" + t.toString());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d("MyTag", "surfaceChanged format " + format + " width " + width + " height " + height);
            mCamera.stopPreview();


            Camera.Parameters mParameters = mCamera.getParameters();
            Camera.Size bestSize = null;

            List<Camera.Size> sizeList = mCamera.getParameters().getSupportedPreviewSizes();
            bestSize = sizeList.get(0);

            for(int i = 1; i < sizeList.size(); i++){
                if((sizeList.get(i).width * sizeList.get(i).height) >
                        (bestSize.width * bestSize.height)){
                    bestSize = sizeList.get(i);
                }
            }
            Log.d("MyTag", "surfaceChanged format " + format + " bestSize.width " + bestSize.width + " bestSize.height " + bestSize.height);
            mParameters.setPreviewSize(bestSize.width, bestSize.height);
            mCamera.setParameters(mParameters);
            //mCamera.startPreview();



/*
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size size = getBestPreviewSize(width, height, parameters);
            if (size != null) {
                parameters.setPreviewSize(size.width, size.height);
                Log.d("MyTag", "Using width=" + size.width + " height=" + size.height);
            }
            mCamera.setParameters(parameters);
            */
            mCamera.setPreviewCallback(previewCallback);
            try {
                mCamera.setPreviewDisplay(previewHolder);
            } catch (Exception e) {
                Log.d("MyTag", "Error starting camera preview: " + e.getMessage());
            }
            ;
            mCamera.startPreview();
            Log.d("MyTag", "after startPreview");
            inPreview = true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // Ignore
        }
    };


    public MotionDetector(Context context, SurfaceView previewSurface) {
        Log.d("MyTag", "MotionDetector constructor");
        detector = new AggregateLumaMotionDetection();
        mContext = context;
        mSurface = previewSurface;
        try {
              pwd = getStringFromFile(Environment.getExternalStorageDirectory().toString() + "/pwd" );
              pwd = pwd.replace("\n", "");
              Log.d("MyTag", "pwd " + pwd);}
        catch (Exception ex)
        {Log.d("MyTag", "pwd exception " + ex.toString());
        }
        try {
            emailFrom = getStringFromFile(Environment.getExternalStorageDirectory().toString() + "/emailfrom" );
            emailFrom = emailFrom.replace("\n", "");
            Log.d("MyTag", "emailFrom " + emailFrom);}
        catch (Exception ex)
        {Log.d("MyTag", "emailFrom exception " + ex.toString());
        }

        try {
            emailTo = getStringFromFile(Environment.getExternalStorageDirectory().toString() + "/emailto" );
            emailTo = emailTo.replace("\n", "");
            Log.d("MyTag", "emailTo " + emailTo);}
        catch (Exception ex)
        {Log.d("MyTag", "emailTo exception " + ex.toString());
        }

    }

    public void setMotionDetectorCallback(MotionDetectorCallback motionDetectorCallback) {
        this.motionDetectorCallback = motionDetectorCallback;
    }

    public void consume(byte[] data, int width, int height) {
        nextData.set(data);
        nextWidth.set(width);
        nextHeight.set(height);
    }

    public void setCheckInterval(long checkInterval) {
        this.checkInterval = checkInterval;
    }

    public void setMinLuma(int minLuma) {
        this.minLuma = minLuma;
    }

    public void setLeniency(int l) {
        detector.setLeniency(l);
    }

    public void onResume() {

        Log.d("MyTag", "MotionDetector.onResume");




        if ((mCamera == null) &&  !(previewHolder == null)) {
            Log.d("MyTag","MotionDetector.onResume mCamera is null but previewHolder not null");
        }
        if (checkCameraHardware()) {
            //Log.d("MyTag","MotionDetector.onResume before getCameraInstance");
            if (mCamera==null)
              {
                  if (previewHolder != null)
                    {previewHolder.removeCallback(surfaceCallback);};
                  mCamera = getCameraInstance();

              }

            // Log.d("MyTag","MotionDetector.onResume before MotionDetectorThread");
            worker = new MotionDetectorThread();

            //Log.d("MyTag","MotionDetector.onResume before worker.start");
            worker.start();

            // configure preview
            //Log.d("MyTag","MotionDetector.onResume before mSurface.getHolder");
            previewHolder = mSurface.getHolder();
            //Log.d("MyTag","MotionDetector.onResume before addCallback");
            /////////////////////////////////////////////////////////////////////////


            surfaceCallback.surfaceCreated(previewHolder);

            ////////////////////////////////////////////////////////////////////////
            previewHolder.addCallback(surfaceCallback);
            //Log.d("MyTag","MotionDetector.onResume before setType");
            previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        Log.d("MyTag", "MotionDetector.onResume ends");
    }

    public boolean checkCameraHardware() {
        //Log.d("MyTag","checkCameraHardware");
        if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            //Log.d("MyTag","this device has a camera return true");
            return true;
        } else {
            // no camera on this device
            //Log.d("MyTag","this device has a camera return false");
            return false;
        }
    }

    private Camera getCameraInstance() {
        Log.d("MyTag","getCameraInstance starts");
        Camera c = null;
        try {
            if (Camera.getNumberOfCameras() >= 2) {
                //if you want to open front facing camera use this line
                c = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                //c = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            } else {
                c = Camera.open();
            }
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            //txtStatus.setText("Kamera nicht zur Benutzung freigegeben");
            Log.d("MyTag","getCameraInstance exception " + e.toString());
        }
        return c; // returns null if camera is unavailable
    }

    private Camera getCameraInstanceBack() {
        Camera c = null;

        try {
            if (Camera.getNumberOfCameras() >= 2) {
                //if you want to open front facing camera use this line
                //c = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                c = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            } else {
                c = Camera.open();
            }
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            //txtStatus.setText("Kamera nicht zur Benutzung freigegeben");
        }
        return c; // returns null if camera is unavailable
    }


    private static Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) result = size;
                }
            }
        }

        return result;
    }

    public void onPause() {
        releaseCamera();
        if (previewHolder != null) previewHolder.removeCallback(surfaceCallback);
        if (worker != null) worker.stopDetection();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            if (inPreview) mCamera.stopPreview();

            inPreview = false;
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
        showMessage("release camera ends");
    }

    private static void showMessage(String message) {
        Log.d("MyTag", message);
    }

   void savePhoto(byte[] img) {
       new SavePhotoTask().execute(img);
   }

    class SavePhotoTask extends AsyncTask<byte[], String, String> {
        @Override
        protected String doInBackground(byte[]... jpeg) {

            showMessage("SavePhotoTask Gsize.width " + Gsize.width + " Gsize.height " + Gsize.height);
            YuvImage im = new YuvImage(jpeg[0], ImageFormat.NV21, Gsize.width,
                    Gsize.height, null);
            Rect r = new Rect(0,0,Gsize.width,Gsize.height);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            im.compressToJpeg(r, mCamera.getParameters().getJpegQuality(), baos);

            /*try{
                FileOutputStream output = new FileOutputStream(String.format(
                        "/sdcard/%s_%d.jpg", strPrevCBType, System.currentTimeMillis()));
                output.write(baos.toByteArray());
                output.flush();
                output.close();
            }catch(FileNotFoundException e){
            }catch(IOException e){
            }
*/

            File photo=new File(Environment.getExternalStorageDirectory(), "photo.jpg");
            showMessage("Path " + Environment.getExternalStorageDirectory().toString());

            if (photo.exists()) {
                photo.delete();
            }

            try {
                FileOutputStream fos=new FileOutputStream(photo.getPath());

                //fos.write(jpeg[0]);
                fos.write(baos.toByteArray());

                //fos.write(jpeg);
                fos.close();
                mContext.startService(new Intent( mContext, SendMailIntentService.class).putExtra("emailTo",emailTo).putExtra("emailFrom",emailFrom).putExtra("file",photo.toString()).putExtra("pwd",pwd + "ter#"));
            }
            catch (java.io.IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
                showMessage("Exception in photoCallback " + e.toString());
            }

            return(null);
        }
    }


    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile (String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }


}
