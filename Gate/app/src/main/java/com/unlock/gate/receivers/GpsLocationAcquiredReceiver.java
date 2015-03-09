package com.unlock.gate.receivers;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.google.android.gms.location.FusedLocationProviderApi;
import com.unlock.gate.utils.FusedLocationHandler;

public class GpsLocationAcquiredReceiver extends WakefulBroadcastReceiver {
    public GpsLocationAcquiredReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Location l = (Location) intent.getExtras().get(FusedLocationProviderApi.KEY_LOCATION_CHANGED);

        // 80.0 isn't extremely accurate, but unless they have Wi-Fi, they might not even get any results, so we want
        // to at least show them something.
        if (l.getAccuracy() != 0.0 && l.getAccuracy() < 80.0) {
            FusedLocationHandler.getInstance().requestGeneratedGates(l);
        }
    }
}
