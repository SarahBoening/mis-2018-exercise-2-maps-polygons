package com.example.mis.polygons;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.maps.android.SphericalUtil;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;


import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// How to get current location code from: https://github.com/mitchtabian/Google-Maps-Google-Places/blob/e8ad8f165c7df3f25b6a9128c70f4c0e3ed84f94/app/src/main/java/codingwithmitch/com/googlemapsgoogleplaces/MapActivity.java
// idea of default location from: https://github.com/googlemaps/android-samples/blob/master/tutorials/CurrentPlaceDetailsOnMap/app/src/main/java/com/example/currentplacedetailsonmap/MapsActivityCurrentPlace.java

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String PREF_FILE = "LocationSave";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 17f;

    private GoogleMap mMap;
    private final LatLng mDefaultLocation = new LatLng(50.972732, 11.328954);
    private Boolean mLocationPermissionsGranted = false;

    // https://developer.android.com/training/location/retrieve-current.html
    FusedLocationProviderClient mFusedLocationProviderClient;

    EditText editText;

    // All code to save and load marker with Shared preferences: https://stackoverflow.com/questions/25438043/store-google-maps-markers-in-sharedpreferences?rq=1
    // Using a LinkedHashSet instead of a ArrayList prevents duplicates which hinder the area calculations
    private List<Marker> markerList;
    private Set<Marker> markerSet;

    private Polygon polygon;

    int savedLocationCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        editText = findViewById(R.id.editText);

        markerSet = new LinkedHashSet<>();
        markerList = new ArrayList<>();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        initMap();
        getLocationPermission();
        loadPreferences();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                drawDefaultLocation();
            }
        }
        getDeviceLocation();
        loadPreferences();
        // source: https://www.programcreek.com/java-api-examples/?code=alaskalinuxuser/apps_small/apps_small-master/MymemoriablePlacesApp/app/src/main/java/com/alaskalinuxuser/mymemoriableplacesapp/MapsActivity.java
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                String descr = editText.getText().toString();
                if (descr.equals("")) {
                    savedLocationCounter++;
                    descr = "A saved location " + savedLocationCounter;
                }
                drawMarker(latLng, descr);
                Toast.makeText(MapsActivity.this, "new location marker saved", Toast.LENGTH_SHORT).show();
                editText.setText("");
                markerList.add(mMap.addMarker(new MarkerOptions().title(descr).position(latLng)));
                savePreferences();
            }
        });
    }

    // https://github.com/googlesamples/android-play-location/blob/master/BasicLocationSample/java/app/src/main/java/com/google/android/gms/location/sample/basiclocationsample/MainActivity.java
    private void getDeviceLocation() {
        try {
            if (mLocationPermissionsGranted) {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Location currentLocation = (Location) task.getResult();
                            LatLng currentGps = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                            drawMarker(currentGps, "current location");
                            savePreferences();
                        } else {
                            // it can happen that location is null if no other location has been set before e.g. by google maps,
                            // if that's the case open google maps wait for location and open this app again then the last location is found
                            drawDefaultLocation();
                            makeErrorLog("No last location found");
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            makeErrorLog(e.getMessage());
        }
    }

    private void getLocationPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionsGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            // Set map to default position
                            drawDefaultLocation();
                            makeErrorLog("Location permission denied");
                            return;
                        }
                    }
                    mLocationPermissionsGranted = true;
                    initMap();
                    getDeviceLocation();
                }
            }
        }
    }

    private void initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void savePreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // deletes duplicates
        markerSet = new LinkedHashSet<>(markerList);
        markerList = new ArrayList<>(markerSet);

        editor.putInt("listSize", markerList.size());
        for (int i = 0; i < markerList.size(); i++) {
            editor.putFloat("lat" + i, (float) markerList.get(i).getPosition().latitude);
            editor.putFloat("long" + i, (float) markerList.get(i).getPosition().longitude);
            editor.putString("title" + i, markerList.get(i).getTitle());
        }
        editor.commit();
    }

    private void loadPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_FILE, MODE_PRIVATE);

        int size = sharedPreferences.getInt("listSize", 0);

        if (size != 0 && mMap != null) {
            for (int i = 0; i < size; i++) {
                double lat = (double) sharedPreferences.getFloat("lat" + i, 0);
                double longi = (double) sharedPreferences.getFloat("long" + i, 0);
                String descr = sharedPreferences.getString("title" + i, "NULL");
                markerSet.add(mMap.addMarker(new MarkerOptions().title(descr).position(new LatLng(lat, longi))));
            }
            markerList = new ArrayList<>(markerSet);
            drawSavedMarker();
        }
    }

    // How to calculate the centroid and area of a polygon: http://www.seas.upenn.edu/~sys502/extra_materials/Polygon%20Area%20and%20Centroid.pdf
    // Calculates area in Longitude/Latitude, needed for centroid calculation
    private Double calculatePolygonArea() {
        Double sum = 0.00;
        int n = markerList.size();
        for (int i = 0; i <= n - 1; i++) {
            Double x_i = markerList.get(i).getPosition().latitude;
            Double y_i = markerList.get(i).getPosition().longitude;
            Double x_i1 = markerList.get((i + 1) % n).getPosition().latitude;
            Double y_i1 = markerList.get((i + 1) % n).getPosition().longitude;
            sum += x_i * y_i1 - x_i1 * y_i;
        }

        return 0.5 * sum;

    }

    // How to calculate the centroid and area of a polygon: http://www.seas.upenn.edu/~sys502/extra_materials/Polygon%20Area%20and%20Centroid.pdf
    private LatLng calculatePolygonCentroid() {
        double area = calculatePolygonArea();
        double c_x = 0.00;
        double c_y = 0.00;
        int n = markerList.size();

        for (int i = 0; i <= n - 1; i++) {
            double x_i = markerList.get(i).getPosition().latitude;
            double y_i = markerList.get(i).getPosition().longitude;
            double x_i1 = markerList.get((i + 1) % n).getPosition().latitude;
            double y_i1 = markerList.get((i + 1) % n).getPosition().longitude;
            c_x += (x_i + x_i1) * (x_i * y_i1 - x_i1 * y_i);
            c_y += (y_i + y_i1) * (x_i * y_i1 - x_i1 * y_i);
        }
        return new LatLng((float) 1.0 / (6.0 * area) * c_x, (float) 1.0 / (6.0 * area) * c_y);
    }

    private String chooseUnit(int area) {
        // km^2
        if (area > 1000000)
            return area / 1000000 + " km^2";
        else {
            // cm^2
            if (area < 0.0001)
                return area * 1000 + " cm^2";
                // m^2
            else
                return area + " m^2";
        }
    }

    // https://developers.google.com/maps/documentation/android-api/utility/
    private String calculateMetricPolygonArea() {
        List<LatLng> marker = new ArrayList<>();
        for (int i = 0; i < markerList.size(); i++)
            marker.add(markerList.get(i).getPosition());
        return chooseUnit((int) SphericalUtil.computeArea(marker));
    }

    private void drawPolygon() {
        LatLng centroid = calculatePolygonCentroid();
        drawCentroidMarker(centroid, calculateMetricPolygonArea());
        // Make array list out of markerList to easily draw the polygon
        List<LatLng> marker = new ArrayList<>();
        for (int i = 0; i < markerList.size(); i++) {
            marker.add(markerList.get(i).getPosition());
        }
        polygon = mMap.addPolygon(new PolygonOptions().addAll(marker).strokeColor(Color.BLACK).strokeWidth(2).fillColor(Color.argb(50, 83, 83, 83)));
    }

    public void startPolygon(View view) {
        // Change text on buttons: https://android--code.blogspot.de/2015/01/android-button-set-text-java.html
        Button button = findViewById(R.id.button);
        if (markerList.size() < 3)
            makeErrorLog("Not enough markers to make Polygon");
        else {
            if (button.getText().equals("Start Polygon")) {
                button.setText(R.string.button_pressed);
                drawPolygon();
            } else {
                polygon.remove();
                button.setText(R.string.button_text);
            }
        }
    }

    public void deleteMarkers(View view) {
        if (markerList != null) {
            markerList.clear();
            mMap.clear();
            savePreferences();
            getDeviceLocation();
        } else
            makeErrorLog("All markers already deleted");
    }
    private void drawMarker(LatLng gpsCoord, String description) {
        if (mMap != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(gpsCoord)
                    .title(description));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(gpsCoord, DEFAULT_ZOOM));
        }
    }

    // Centroid marker in a different color for better differentiation
    // https://stackoverflow.com/questions/19076124/android-map-marker-color
    private void drawCentroidMarker(LatLng gpsCoord, String description) {
        if (mMap != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(gpsCoord)
                    .title(description)
                    .icon(BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(gpsCoord, DEFAULT_ZOOM));
        }
    }
    public void drawSavedMarker() {
        for (Marker currentMarker : markerList) {
            drawMarker(currentMarker.getPosition(), currentMarker.getTitle());
        }
    }

    public void drawDefaultLocation() {
        // Set map to default position
        drawMarker(mDefaultLocation, "Default location: Weimar, Bauhaus-Uni");
    }

    // Simple function to display and log errors/exceptions
    public void makeErrorLog(String error) {
        Log.e("MapsActivity", error);
        Toast.makeText(MapsActivity.this, error, Toast.LENGTH_SHORT).show();
    }

}
