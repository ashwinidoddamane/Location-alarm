package com.juggernaut.location_alarm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.arch.core.BuildConfig;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

/**
 * Using location settings.
 *
 * Uses the SettingsClient to ensure that the device's system settings are properly configured for
 * the app's location needs. When making a request to Location services, the device's system settings
 * may be in a state that prevents the app from obtaining the location data that it needs. For example,
 * GPS or Wi-Fi scanning may be switched off. The SettingsClient makes it possible to determine if a
 * device's system settings are adequate for the location request, and to optionally invoke a dialog
 * that allows the user to enable the necessary settings.
 *
 * This application allows the user to request location updates using the ACCESS_FINE_LOCATION setting
 * (as specified in AndroidManifest.xml).
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnMyLocationClickListener {

    private static final String TAG = MapsActivity.class.getSimpleName();

    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    /**
     * Constant used in the location settings dialog.
     */
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 180000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * Keys for storing activity state in the Bundle.
     */
    private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LOCATION = "location";

    /**
     * Zoom level for the map camera.
     */
    private static final int ZOOM_LEVEL = 14;

    /**
     * This is the object of main class of the Google Maps Android API and is the entry point
     * for all methods related to the map.
     */
    static GoogleMap mMap;

    /**
     * Used to store destination coordinates.
     */
    static double destinationLatitude;
    static double destinationLongitude;

    /**
     * Current location coordinates.
     */
    static double currentLatitude;
    static double currentLongitude;

    /**
     * Represents a geographical location.
     */
    public Location mCurrentLocation;

    /**
     * Keep the device awake.
     */
    protected PowerManager.WakeLock mWakeLock;

    /**
     * The autocomplete widget is a search dialog for searching places with built-in autocomplete functionality.
     */
    PlaceAutocompleteFragment autocompleteFragment;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationProviderClient;

    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient mSettingsClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Callback for Location events.
     */
    private LocationCallback mLocationCallback;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    private Boolean mRequestingLocationUpdates;

    /**
     * The BroadcastReceiver used to listen from broadcasts from the service.
     */
    private MyReceiver myReceiver;

    /**
     * A reference to the service used to get location updates.
     */
    private LocationUpdatesService mService = null;

    /**
     * Used for positioning current location button.
     */
    private View mapView;

    /**
     * Tracks the bound state of the service.
     */
    private boolean mBound = false;

    /**
     * Monitors the state of the connection to the service.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        // Called when a connection  to the Service has been established, with the IBinder
        // of the communication channel to the Service.
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Service Connected");
            LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        // Called when a connection to the Service has been lost.
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Service Disconnected");

            mService = null;
            mBound = false;
        }
    };

    /**
     * Used in onBackPressed.
     */
    private Boolean exit = false;

    /**
     * Getter method for getting destination's latitude
     *
     * @return destination's latitude
     */
    public static double getLatitude() {
        return destinationLatitude;
    }

    /**
     * Getter method for getting destination's longitude
     *
     * @return destination's longitude
     */
    public static double getLongitude() {
        return destinationLongitude;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "<onCreate>");

        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapView = mapFragment.getView();
        mapFragment.getMapAsync(this);

        Utils.setRequestingLocationUpdates(this, false);
        mRequestingLocationUpdates = true;
        updateValuesFromBundle(savedInstanceState);

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            this.mWakeLock = powerManager.newWakeLock(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, getString(R.string.tag));
        }
        this.mWakeLock.acquire(5 * 60 * 1000L);    // 5 Minutes

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

    }

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        KEY_REQUESTING_LOCATION_UPDATES);
                Log.i(TAG, "Getting mRequestingLocationUpdates from bundle - " + mRequestingLocationUpdates);
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation is not null.
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
                Log.i(TAG, "Getting Location from bundle!" + mCurrentLocation);
            }
        }
    }

    /**
     * Zoom in to place in response to user's selection
     */
    public void autoCompleteSearch() {
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.i(TAG, "Place selected !");
                LatLng coordinate;
                coordinate = place.getLatLng();
                CameraUpdate location = CameraUpdateFactory.newLatLngZoom(
                        coordinate, ZOOM_LEVEL);
                mMap.animateCamera(location);

            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "Place selection failed!");
                Toast.makeText(getApplicationContext(), "Place selection failed: " + status.getStatusMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Log.i(TAG, "Location callback created!");

                // Using the Google Play services location APIs, your app can request the last known location
                // of the user's device. In most cases, you are interested in the user's current location,
                // which is usually equivalent to the last known location of the device.
                mCurrentLocation = locationResult.getLastLocation();
                updateCurrentLocation();
            }
        };
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * ACCESS_COARSE_LOCATION and ACCESS_FINE_LOCATION. These settings control
     * the accuracy of the current location. This application uses ACCESS_FINE_LOCATION,
     * as defined in the AndroidManifest.xml.
     *
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     *
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private void createLocationRequest() {
        Log.i(TAG, "Location request created!");

        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Uses a LocationSettingsRequest.Builder to build LocationSettingsRequest that is used
     * for checking if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        Log.i(TAG, "GPS dialog created!");

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    /**
     * To move camera to current position
     */
    private void updateCurrentLocation() {
        if (mCurrentLocation != null) {
            Log.i(TAG, "Current location updated - " + mCurrentLocation.getLatitude() + ", " + mCurrentLocation.getLongitude());

            currentLatitude = mCurrentLocation.getLatitude();
            currentLongitude = mCurrentLocation.getLongitude();
            LatLng coordinate = new LatLng(currentLatitude, currentLongitude);
            CameraUpdate location = CameraUpdateFactory.newLatLngZoom(
                    coordinate, ZOOM_LEVEL);

            mMap.animateCamera(location);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "<onStart>");

        mRequestingLocationUpdates = true;
        myReceiver = new MyReceiver();
        if (checkPermissions()) {
            startLocationUpdates();

        } else {
            requestPermissions();
        }
        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(new Intent(this, LocationUpdatesService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "<onResume>");

        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver,
                new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "<onStop>");

        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection);
            mBound = false;
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "<onDestroy>");

        this.mWakeLock.release();
        super.onDestroy();
    }

    /**
     * onSaveInstanceState method gets called typically before/after onStop() is called.
     * Helps in managing the state of the application.
     */
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        Log.i(TAG, "Saving state of the application - " + mRequestingLocationUpdates);

        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        Log.i(TAG, "Checking permission!");

        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied, location update started!.");

                        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        updateCurrentLocation();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");

                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                mRequestingLocationUpdates = false;
                        }

                        updateCurrentLocation();
                    }
                });
    }

    /**
     * Requesting ACCESS_FINE_LOCATION location permission.
     */
    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");

            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MapsActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");

            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Shows Snackbar.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Log.i(TAG, "Showing snackbar!");

        Snackbar.make(
                        findViewById(android.R.id.content),
                        getString(mainTextStringId),
                        Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Handles pin click.
     */
    public void pinClicked(View view) {
        Log.i(TAG, "Pin clicked!");

        final LatLng targetCoordinate = mMap.getCameraPosition().target;
        destinationLatitude = targetCoordinate.latitude;
        destinationLongitude = targetCoordinate.longitude;

        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        View dialogView = LayoutInflater.from(MapsActivity.this)
                .inflate(R.layout.dialog_box, null, false);
        ((TextView) dialogView.findViewById(R.id.checkpoint_lat_tv))
                .setText(String.format("%s%s", getString(R.string.lat_dialog_text), String.valueOf(targetCoordinate.latitude)));
        ((TextView) dialogView.findViewById(R.id.checkpoint_long_tv))
                .setText(String.format("%s%s", getString(R.string.long_dialog_text), String.valueOf(targetCoordinate.longitude)));

        final EditText nameEditText = dialogView.findViewById(R.id.checkpoint_name_tv);
        final AlertDialog alertDialog = builder.setView(dialogView).show();
        Button done = alertDialog.findViewById(R.id.dialogbox_done_btn);

        if (done != null) {
            done.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String enteredText = nameEditText.getText().toString();
                    if (enteredText.length() >= 3) {

                        float[] results = new float[3];
                        Location.distanceBetween(currentLatitude, currentLongitude, destinationLatitude, destinationLongitude, results);
//                        if (results[0] < LocationUpdatesService.MAX_DISTANCE_RANGE) {
//                             Toast.makeText(getApplicationContext(), "You are already near to the destination", Toast.LENGTH_SHORT).show();
//                        } else {
                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(targetCoordinate);
                        markerOptions.draggable(false);
                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.flag));
                        markerOptions.title(enteredText);
                        mMap.clear();
                        mMap.addMarker(markerOptions);
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(targetCoordinate));
                        mMap.setMaxZoomPreference(mMap.getMaxZoomLevel());
                        LatLng latLng = new LatLng(destinationLatitude, destinationLongitude);
                        mMap.addCircle(new CircleOptions().center(latLng)
                                .strokeWidth(5)
                                .strokeColor(getResources().getColor(R.color.cardview_dark_background))
                                .radius(LocationUpdatesService.MAX_DISTANCE_RANGE));

                        if (!checkPermissions()) {
                            requestPermissions();
                        } else {
                            if (!Utils.requestingLocationUpdates(getApplicationContext())) {
                                mService.requestLocationUpdates();
                            }
                        }
                        alertDialog.dismiss();
//                        }

                    } else {
                        nameEditText.setError("Name should have minimum of 4 characters.");
                    }
                }
            });
        }

        Button cancel = alertDialog.findViewById(R.id.dialogbox_cancel_btn);
        if (cancel != null) {
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "Cancel Clicked!");
                    alertDialog.dismiss();
                }
            });
        }
    }

    /**
     * Callback interface for when the map is ready to be used.
     *
     * @param googleMap A non-null instance of a GoogleMap associated with the MapFragment
     *                  or MapView that defines the callback.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i(TAG, "Map is ready!");

        mMap = googleMap;
        mMap.setOnMyLocationClickListener(this);
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));

        if (mapView != null && mapView.findViewById(Integer.parseInt("1")) != null) {
            // Get the button view
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            // and next place it, on bottom right (as Google Maps app)
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 50, 50);
        }
        if (checkPermissions()) {
            mMap.setMyLocationEnabled(true);
        }
        Log.i(TAG, "Location Button Enabled! (1)");
    }

    /**
     * Getting a Result from an Activity & track response from dialog for enabling GPS.
     */
    @SuppressLint("MissingPermission")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");

                        if (checkPermissions()) {
                            mMap.setMyLocationEnabled(true);
                            Log.i(TAG, "Location Button Enabled! (2)");
                        }
                        startLocationUpdates();
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        Toast.makeText(this, "Enable GPS for getting accurate result.", Toast.LENGTH_SHORT).show();
                        mRequestingLocationUpdates = false;
                        break;
                }
                break;
        }
    }

    /**
     * Handles back key pressed event.
     */
    @Override
    public void onBackPressed() {
        Log.i(TAG, "Back button pressed!");

        if (exit) {
            finish(); // finish activity
        } else {
            Toast.makeText(this, "Press Back again to Exit.",
                    Toast.LENGTH_SHORT).show();
            exit = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exit = false;
                }
            }, 3 * 1000);
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "<onPause>");

        if (mRequestingLocationUpdates) {
            stopLocationUpdates();
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        super.onPause();
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.i(TAG, "Location updates never requested!");
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.i(TAG, "Location update stopped!");

                        mRequestingLocationUpdates = false;
                    }
                });
    }

    /**
     * Handle the permissions request response
     *
     * When the user responds to your app's permission request, the system invokes your app's
     * onRequestPermissionsResult() method, passing it the user response.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted, updates requested, starting location updates");

                startLocationUpdates();
                mMap.setMyLocationEnabled(true);
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.

                Log.i(TAG, "Permission Denied!");

                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    /**
     * Display current location coordinates and accuracy in toast message on clicking blue dot.
     *
     * @param location Current geographic location.
     */
    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Log.i(TAG, "Location button Clicked!");

        int acc;
        acc = (int) location.getAccuracy();
        Toast.makeText(MapsActivity.this, "Accuracy: " + acc + " m\n" + Utils.getLocationCoordinate(location),
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Receiver for broadcasts sent by LocationUpdatesService.
     */
    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
            if (location != null) {
                Log.i(TAG, "Receiving broadcasts : " + location.getLatitude() + ", " + location.getLongitude());

                //     Toast.makeText(MapsActivity.this, Utils.getLocationCoordinate(location), Toast.LENGTH_SHORT).show();
            }
        }
    }

}