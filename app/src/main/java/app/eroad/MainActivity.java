package app.eroad;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity
        extends ActionBarActivity
        implements LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {


    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    Location mCurrentLocation;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    String mLastUpdateTime;

    private static final String TAG = "MainActivity";
    private static final long INTERVAL = 1000 * 60;
    private static final long FASTEST_INTERVAL = 1000 * 10;
    private LatLng eroadLocation = new LatLng(-36.7221948, 174.7061665);

    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (map == null) {
            ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                    .getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            map = googleMap;
                            initializeMap();
                        }
                    });
        } else {
            initializeMap();
        }

        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }


    private String getTimeZoneLocation() {
        return TimeZone.getDefault().getID();
    }

    private String getUTCtime() {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("gmt"));
        return df.format(new Date());
    }

    private String getLocalTime() {
        return new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTimeInMillis());
    }

    public String getAddress(Location location) {
        if (location == null) {
            return "";
        }
        try {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            int maxResults = 1;

            Geocoder gc = new Geocoder(getApplicationContext(), Locale.getDefault());
            List<Address> addresses = gc.getFromLocation(latitude, longitude,
                    maxResults);

            if (addresses.size() > 0) {
                String address = addresses.get(0).getAddressLine(0);
                String city = addresses.get(0).getAddressLine(1);
                String country = addresses.get(0).getAddressLine(2);
                return address + ", " + city + ", " + country;
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    public void initializeMap() {
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.setMyLocationEnabled(true);

        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marcador) {
                View v = getLayoutInflater().inflate(R.layout.custom_marker, null);
                TextView title = (TextView) v.findViewById(R.id.title);
                TextView subtitle = (TextView) v.findViewById(R.id.subtitle);
                title.setText(marcador.getTitle());
                subtitle.setText(marcador.getSnippet());
                return v;
            }

            @Override
            public View getInfoContents(Marker marcador) {
                Log.i("LoadImage", "chamou getInfoContents de " + marcador.getTitle());
                return null;
            }
        });

    }

    // Check that Google Play services is available
    private boolean servicesConnected() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates", "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            try {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0).show();
            } catch (Exception e) {
                Log.e("GooglePlayService", "Error: GooglePlayServiceUtil: " + e);

                Uri uri = Uri.parse("market://details?id=com.google.android.gms");
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException ex) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.google.android.gms")));
                }
            }
            Log.e("Location Updates", "Google Play services NOT available.");
            return false;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
    }

    /*
     * Called by Location Services if the attempt to Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
        Log.e("Location Updates",
                "Google Play services NOT available");
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
			/*
			 * If no resolution is available, display a dialog to the user with
			 * the error.
			 */
            // showErrorDialog(connectionResult.getErrorCode());
            Toast.makeText(this, "Error on connect.", Toast.LENGTH_SHORT)
                    .show();
            Log.e("Location Updates",
                    "Google Play services NOT available, error:");
        }
    }

    /*
     * Called when the Activity becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mGoogleApiClient.connect();
    }

    /*
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
        Log.d(TAG, "Location update resumed");
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    protected void startLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
//            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
//            Log.d(TAG, "Location update started ..............: ");
            LatLng currentLatLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            // Move the camera and Zoom in, animating the camera.
            CameraPosition newCamPos = new CameraPosition(currentLatLng, 17,
                    map.getCameraPosition().tilt,
                    map.getCameraPosition().bearing); // use old bearing
            map.animateCamera(CameraUpdateFactory.newCameraPosition(newCamPos),
                    2000, null);

            Location eroad = new Location("");
            eroad.setLatitude(eroadLocation.latitude);
            eroad.setLongitude(eroadLocation.longitude);
            Marker myLocationMarker = map.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .alpha(0.7f));
            myLocationMarker.setTitle("MyLocation");
            myLocationMarker.setSnippet("" + mCurrentLocation.getLatitude() + " / " + mCurrentLocation.getLongitude() + "\n" +
                            getTimeZoneLocation() + "\n" +
                            "UTC Time " + getUTCtime() + "\n" +
                            "Local Time " + getLocalTime() + "\n" +
                            "Local Time2 " + getLocalTime() + "\n" +
                            "Time to Eroad " + getTimeDistance(mCurrentLocation, eroad)
//                getAddress(mCurrentLocation)
            );
            Log.i("", "Zone:" + getTimeZoneLocation());
            Log.i("", "UTC:" + getUTCtime());
            Log.i("", "LocalTime:" + getLocalTime());
            Log.i("", "Time from Eroad:" + getTimeDistance(mCurrentLocation, eroad));
            Log.i("", "Local:" + getAddress(mCurrentLocation));

            Marker eroadMarker = map.addMarker(new MarkerOptions()
                    .position(eroadLocation)
                    .alpha(0.7f));
            eroadMarker.setTitle("Eroad");
            eroadMarker.setSnippet("260 Oteha Valley Rd, Albany,Auckland");
        }
    }

    public static int TIME_PER_METER_SECONDS_EST = 2;

    private String getTimeDistance(Location location1, Location location2) {
        Float distanceInMeters;
        distanceInMeters = location1.distanceTo(location2);


        Log.i("", "XTime from Eroad:" + (distanceInMeters.intValue()/ TIME_PER_METER_SECONDS_EST)/60);//SphericalUtil.computeDistanceBetween(new LatLng(location1.getLatitude(), location1.getLongitude()), new LatLng(location2.getLatitude(), location2.getLongitude())));
        Log.i("", "XTime from Eroad:" + distanceInMeters.intValue()/ TIME_PER_METER_SECONDS_EST);

        return new SimpleDateFormat("HH:mm:ss").format(new Date(0).getTime() + (distanceInMeters.intValue() / TIME_PER_METER_SECONDS_EST));
    }

    protected void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            Log.d(TAG, "Location update stopped ..............: ");
        }
    }

    @Override
    public void onConnected(Bundle dataBundle) {
        Log.d(TAG, "onConnected - isConnected ... : " + mGoogleApiClient.isConnected());
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        startLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Firing onLocationChanged...");
        mCurrentLocation = location;
        mLastUpdateTime = java.text.DateFormat.getTimeInstance().format(new Date());
//            stopLocationUpdates();
    }
}