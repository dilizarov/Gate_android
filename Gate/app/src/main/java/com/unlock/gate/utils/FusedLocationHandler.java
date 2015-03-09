package com.unlock.gate.utils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.unlock.gate.MainActivity;
import com.unlock.gate.receivers.ActivityRecognitionUpdateReceiver;
import com.unlock.gate.receivers.GpsLocationAcquiredReceiver;

import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

public class FusedLocationHandler implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static FusedLocationHandler instance;

    private Context mContext;

    private Activity activity;

    private static final long INTERVAL = 1000 * 40;
    private static final long FASTEST_INTERVAL = 1000 * 20;
    private LocationRequest locationRequest;
    private GoogleApiClient googleApiClient;
    private PendingIntent locationIntent;
    private PendingIntent activityIntent;

    private boolean locationUpdating;
    private boolean activityUpdating;

    private boolean successfullyFetched;

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
                 .addApi(ActivityRecognition.API)
                 .addConnectionCallbacks(this)
                 .addOnConnectionFailedListener(this)
                 .build();

        locationUpdating = false;
        activityUpdating = false;
        successfullyFetched = false;

        Intent locIntent = new Intent(mContext, GpsLocationAcquiredReceiver.class);
        locationIntent = PendingIntent.getBroadcast(mContext, 0, locIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent actIntent = new Intent(mContext, ActivityRecognitionUpdateReceiver.class);
        activityIntent = PendingIntent.getBroadcast(mContext, 0, actIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public void connect(Activity activity) {
        this.activity = activity;

        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startLocationUpdates();
            startActivityUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public void requestGeneratedGates(Location l) {
        if (activity != null) ((MainActivity) activity).requestGeneratedGates(l);
    }

    public void startLocationUpdates() {
        if (!locationUpdating) {
            locationUpdating = true;
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, locationIntent);
        }
    }

    public void startActivityUpdates() {
        if (!activityUpdating) {
            activityUpdating = true;
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(googleApiClient, FASTEST_INTERVAL, activityIntent);

        }
    }

    public void stopLocationUpdates() {
        if (locationUpdating) {
            locationUpdating = false;
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, locationIntent);
        }
    }

    public void stopActivityUpdates() {
        if (activityUpdating) {
            activityUpdating = false;
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(googleApiClient, activityIntent);
        }
    }

    public void revokeSuccessfulFetch() {
        successfullyFetched = false;
    }

    public void successfullyFetchedGeneratedGates() {
        successfullyFetched = true;
    }

    public void processDetectedActivity(DetectedActivity detectedActivity) {
        // We go through at least three cycles of detectedActivities.

        DateTime now = DateTime.now();
        DateTimeComparator comparator = DateTimeComparator.getInstance();

        switch (detectedActivity.getType()) {
            case DetectedActivity.STILL:
                if (detectedActivity.getConfidence() >= 70 && successfullyFetched) {
                    stopLocationUpdates();
                }

                break;
            case DetectedActivity.IN_VEHICLE:
                if (detectedActivity.getConfidence() >= 90 && successfullyFetched) {
                    stopLocationUpdates();
                }

                break;
            case DetectedActivity.ON_FOOT:
            case DetectedActivity.ON_BICYCLE:
                if (detectedActivity.getConfidence() >= 50 && successfullyFetched) {
                    startLocationUpdates();
                }

                break;
        }
    }

    public boolean locationUpdating() {
        return locationUpdating;
    }
}
