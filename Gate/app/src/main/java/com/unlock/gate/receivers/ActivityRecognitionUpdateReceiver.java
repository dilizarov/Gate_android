package com.unlock.gate.receivers;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.unlock.gate.utils.FusedLocationHandler;

public class ActivityRecognitionUpdateReceiver extends WakefulBroadcastReceiver {
    public ActivityRecognitionUpdateReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (ActivityRecognitionResult.hasResult(intent)) {

            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            DetectedActivity detectedActivity = result.getMostProbableActivity();

            FusedLocationHandler.getInstance().processDetectedActivity(detectedActivity);
        }
    }
}
