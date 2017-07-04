package com.diter.motiondetection.motiondetection;

public interface MotionDetectorCallback {
    void onMotionDetected();
    void onTooDark();
}
