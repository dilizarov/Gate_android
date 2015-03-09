package com.unlock.gate.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.unlock.gate.receivers.GpsLocationAcquiredReceiver;

public class FusedLocationHandler implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static FusedLocationHandler instance;

    private Context mContext;

    private static final long INTERVAL = 1000 * 40;
    private static final long FASTEST_INTERVAL = 1000 * 20;
    private LocationRequest locationRequest;
    private GoogleApiClient googleApiClient;
    private Location location;
    private PendingIntent locationIntent;

    private boolean locationUpdating;

    public static synchronized FusedLocationHandler getInstance(Context context) {
        if (instance == null) {
            instance = new FusedLocationHandler(context);
        }

        return instance;
    }

    public static synchronized FusedLocationHandler getInstance() {
        if (instance == null) {
            throw new IllegalStateException(FusedLocationHandler.class.getSimpleName() +
                    " is not initialized, call getInstance(..) method first.");
        }

        return instance;
    }

    public FusedLocationHandler(Context context) {
        mContext = context;
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        googleApiClient = new GoogleApiClient.Builder(mContext)
                 .addApi(LocationServices.API)
                 .addConnectionCallbacks(this)
                 .addOnConnectionFailedListener(this)
                 .build();

        locationUpdating = false;

        Intent i = new Intent(mContext, GpsLocationAcquiredReceiver.class);
        locationIntent = PendingIntent.getBroadcast(mContext, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public void connect() {
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public void startLocationUpdates() {
        if (!locationUpdating) {
            locationUpdating = true;
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, locationIntent);
        }
    }

    public void stopLocationUpdates() {
        if (locationUpdating) {
            locationUpdating = false;
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, locationIntent);
        }
    }

    public boolean locationUpdating() {
        return locationUpdating;
    }
}
