package com.unlock.gate.receivers;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderApi;

public class GpsLocationAcquiredReceiver extends WakefulBroadcastReceiver {
    public GpsLocationAcquiredReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Location l = (Location) intent.getExtras().get(FusedLocationProviderApi.KEY_LOCATION_CHANGED);

        // 110.0 isn't extremely accurate, but unless they have Wi-Fi, they might not even get any results, so we want
        // to at least show them something.
        if (l.getAccuracy() != 0.0 && l.getAccuracy() < 110.0) {
            Log.e("LOCATION", l.toString());
            Log.e("ACCURACY", Float.toString(l.getAccuracy()));
        }
    }
}
