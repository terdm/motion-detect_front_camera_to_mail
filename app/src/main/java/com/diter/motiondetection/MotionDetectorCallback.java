package com.diter.motiondetection;

public interface MotionDetectorCallback {
    void onMotionDetected(byte[] img);
    void onTooDark();
}
