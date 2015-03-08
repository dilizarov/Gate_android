package com.unlock.gate.services;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class FusedLocationService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final long INTERVAL = 1000 * 120;
    private static final long FASTEST_INTERVAL = 1000 * 40;
    private LocationRequest locationRequest;
    private GoogleApiClient googleApiClient;
    private Location location;
    private FusedLocationProviderApi fusedLocationProviderApi;


    public FusedLocationService() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

         googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                 .addApi(LocationServices.API)
                 .addConnectionCallbacks(this)
                 .addOnConnectionFailedListener(this)
                 .build();

        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        //fusedLocationProviderApi.requestLocationUpdates(googleApiClient, locationRequest, )
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onStatusChanged(String a, int b, Bundle c) {
        Log.e("What", "THE STATUS HAS BEEN CHANGED");
    }

    @Override
    public void onProviderEnabled(String a) {
        Log.e("What", "PROVIDER ENABLED");
    }

    @Override
    public void onProviderDisabled(String a) {
        Log.e("What", "PROVIDER DISABLED");
    }

    @Override
    public void onLocationChanged(Location l) {
        Log.e("WHAT", "LOCATION CHANGED");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
