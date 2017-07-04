package com.diter.motiondetection.motiondetection;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MotionDetector {
    class MotionDetectorThread extends Thread {
        private AtomicBoolean isRunning = new AtomicBoolean(true);

        public void stopDetection() {
            isRunning.set(false);
        }

        @Override
        public void start() {
            super.start();
            Log.d("MyTag","MotionDetectorThread Start");
        }
        @Override
        public void run() {
            Log.d("MyTag","MotionDetectorThread.Run isRunning " + isRunning.toString());
            while (isRunning.get()) {
                long now = System.currentTimeMillis();
                //Log.d("MyTag","MotionDetectorThread.Run now " + now + "lastCheck " + lastCheck + " checkInterval " + checkInterval);
                if (now-lastCheck > checkInterval) {
                    lastCheck = now;

                    if (nextData.get() != null) {
                        int[] img = ImageProcessing.decodeYUV420SPtoLuma(nextData.get(), nextWidth.get(), nextHeight.get());
                        //Log.d("MyTag","after ImageProcessing");

                        // check if it is too dark
                        int lumaSum = 0;
                        for (int i : img) {
                            lumaSum += i;
                        }
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
                                        motionDetectorCallback.onMotionDetected();
                                    }
                                });
                                try {
                                    //////////////////////////////////////////////////////////////////////
                                    releaseCamera();
                                    Log.d("MyTag", "after releaseCamera");
                                    mCamera =getCameraInstanceBack();
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
                                    mCamera =getCameraInstance();
                                    Log.d("MyTag", "after getCameraInstance");

                                    parameters = mCamera.getParameters();
                                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                                    mCamera.setParameters(parameters);
                                    mCamera.setPreviewCallback(previewCallback);
                                    try {
                                        mCamera.setPreviewDisplay(previewHolder);
                                    } catch (Exception e) {
                                        Log.d("MyTag", "Error starting camera preview: " + e.getMessage());
                                    }
                                    ;
                                    mCamera.startPreview();
                                }
                                catch (Exception ex)
                                {
                                    Log.d("MyTag"," error flash " + ex.toString());
                                }

                                //////////////////////////////////////////////////////////////////////
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

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
            try {
                mCamera.setPreviewDisplay(previewHolder);
                mCamera.setPreviewCallback(previewCallback);
            } catch (Throwable t) {
                Log.d("MyTag", "Exception in setPreviewDisplay()" +  t.toString());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d("MyTag","surfaceChanged format "+ format + " width " + width + " height " + height);
            mCamera.stopPreview();
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size size = getBestPreviewSize(width, height, parameters);
            if (size != null) {
                parameters.setPreviewSize(size.width, size.height);
                Log.d("MyTag", "Using width=" + size.width + " height=" + size.height);
            }
            mCamera.setParameters(parameters);
            mCamera.setPreviewCallback(previewCallback);
            try {
                mCamera.setPreviewDisplay(previewHolder);
            }
            catch(Exception e)
            {
                Log.d("MyTag", "Error starting camera preview: " + e.getMessage());
            };
            mCamera.startPreview();
            Log.d("MyTag","after startPreview");
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
        Log.d("MyTag","MotionDetector constructor");
        detector = new AggregateLumaMotionDetection();
        mContext = context;
        mSurface = previewSurface;



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
        Log.d("MyTag","MotionDetector.onResume");
        if (checkCameraHardware()) {
            //Log.d("MyTag","MotionDetector.onResume before getCameraInstance");
            mCamera = getCameraInstance();
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
        Log.d("MyTag","MotionDetector.onResume ends");
    }

    public boolean checkCameraHardware() {
        //Log.d("MyTag","checkCameraHardware");
        if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            //Log.d("MyTag","this device has a camera return true");
            return true;
        } else {
            // no camera on this device
            //Log.d("MyTag","this device has a camera return false");
            return false;
        }
    }

    private Camera getCameraInstance(){
        Camera c = null;

        try {
            if (Camera.getNumberOfCameras() >= 2) {
                //if you want to open front facing camera use this line
                c = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                //c = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            } else {
                c = Camera.open();
            }
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            //txtStatus.setText("Kamera nicht zur Benutzung freigegeben");
        }
        return c; // returns null if camera is unavailable
    }

    private Camera getCameraInstanceBack(){
        Camera c = null;

        try {
            if (Camera.getNumberOfCameras() >= 2) {
                //if you want to open front facing camera use this line
                //c = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                c = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            } else {
                c = Camera.open();
            }
        }
        catch (Exception e){
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

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.setPreviewCallback(null);
            if (inPreview) mCamera.stopPreview();
            inPreview = false;
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }
}
