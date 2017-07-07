package com.diter.motiondetection;

import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private TextView txtStatus;
    private MotionDetector motionDetector;

public void onClickStart(View v) {
    //byte[] img = new byte[] {};

    Toast.makeText(getBaseContext(), "onClickStart", Toast.LENGTH_SHORT).show();
    Log.d("MyTag", "onClickStart");
    try {
        if (motionDetector==null) {
            Log.d("MyTag", "motionDetector is null");
            motionDetector = new MotionDetector(this, (SurfaceView) findViewById(R.id.surfaceView));
            motionDetector.setMotionDetectorCallback(new MotionDetectorCallback() {
                @Override
                public void onMotionDetected(byte[] img ) {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(80);
                    //txtStatus.setText("Motion detected");
                    Log.d("MyTag","Motion Detected");
                }

                @Override
                public void onTooDark() {
                    txtStatus.setText("Too dark here");
                }
            });
        } else {
            Log.d("MyTag", "motionDetector is not null");
        }
        ;
    }
    catch (Exception ex) {
        Log.d("MyTag", "motionDetector error " + ex.toString());
    }
}
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtStatus = (TextView) findViewById(R.id.txtStatus);


        //onClickStart(txtStatus);

        motionDetector = new MotionDetector(this, (SurfaceView) findViewById(R.id.surfaceView));
        motionDetector.setMotionDetectorCallback(new MotionDetectorCallback() {
            @Override
            public void onMotionDetected(byte[] img ) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(80);
                txtStatus.setText("Motion detected");
                Log.d("MyTag","Motion detected");
                //new SavePhotoTask().execute(img);
                motionDetector.savePhoto(img);
                //startService(new Intent(getApplicationContext(), PhotoTakingService.class));
                Log.d("MyTag","After PhotoTakingService");
            }

            @Override
            public void onTooDark() {
                txtStatus.setText("Too dark here");
            }
        });

        ////// Config Options
        //motionDetector.setCheckInterval(500);
        //motionDetector.setLeniency(20);
        //motionDetector.setMinLuma(1000);
    }

    @Override
    protected void onResume() {

        super.onResume();
        //onClickResume(txtStatus);
/*
        mSurfaceViewContainer.removeAllViews();
        mSurfaceView = new SurfaceView(mSurfaceViewContainer.getContext());
        mSurfaceViewContainer.addView(mSurfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        SurfaceHolder previewHolder = mSurfaceView.getHolder();
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        previewHolder.addCallback(mSurfaceHolderCallback);
        */

        motionDetector.onResume();

        if (motionDetector.checkCameraHardware()) {
            txtStatus.setText("Camera found");
        } else {
            txtStatus.setText("No camera available");
        }
    }

    public void onClickResume(View v) {
        Toast.makeText(getBaseContext(), "onClickResume", Toast.LENGTH_SHORT).show();
        Log.d("MyTag", "onClickResume");
        //super.onResume();
        motionDetector.onResume();

        if (motionDetector.checkCameraHardware()) {
            txtStatus.setText("Camera found");
        } else {
            txtStatus.setText("No camera available");
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        //motionDetector.onPause();
        Log.d("MyTag", "MainActivity onPause");
    }

    public void onClickPause(View v) {
        //super.onPause();
        Toast.makeText(getBaseContext(), "onClickPause", Toast.LENGTH_SHORT).show();
        Log.d("MyTag", "onClickPause");
        motionDetector.onPause();
    }

}
