package com.unlock.gate.receivers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.unlock.gate.R;
import com.unlock.gate.services.GcmIntentService;
import com.unlock.gate.utils.FusedLocationHandler;

public class GpsProviderReceiver extends WakefulBroadcastReceiver {
    public GpsProviderReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            FusedLocationHandler.getInstance().startLocationUpdates();
        } else {
            FusedLocationHandler.getInstance().stopLocationUpdates();

            // If it is the first time they turn off GPS, send them a notification explaining that generated gates
            // will stay available, but the next time they turn GPS back on, they arne't guaranteed to stay available.
            SharedPreferences gpsPref = context.getSharedPreferences(context.getString(R.string.gps_shared_preferences_key), context.MODE_PRIVATE);
            boolean firstTimeGpsTurnedOff = gpsPref.getBoolean("firstTimeGpsTurnedOff", true);
            if (firstTimeGpsTurnedOff) {
                gpsPref.edit().putBoolean("firstTimeGpsTurnedOff", false).commit();

                ComponentName comp = new ComponentName(context.getPackageName(),
                        GcmIntentService.class.getName());

                Intent i = new Intent();
                i.setComponent(comp);

                // String to keep uniformity with when notifications come through from GCM.
                i.putExtra("notification_type", "5000");

                startWakefulService(context, i);
            }
        }
    }
}
