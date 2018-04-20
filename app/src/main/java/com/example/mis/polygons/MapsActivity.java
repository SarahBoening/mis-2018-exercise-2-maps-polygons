package com.example.mis.polygons;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


// How to get current location code from: https://github.com/mitchtabian/Google-Maps-Google-Places/blob/e8ad8f165c7df3f25b6a9128c70f4c0e3ed84f94/app/src/main/java/codingwithmitch/com/googlemapsgoogleplaces/MapActivity.java

// and some parts from: https://github.com/googlemaps/android-samples/blob/master/tutorials/CurrentPlaceDetailsOnMap/app/src/main/java/com/example/currentplacedetailsonmap/MapsActivityCurrentPlace.java

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";

    private static final String PREF_FILE = "LocationSave";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;

    private GoogleMap mMap;
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private Boolean mLocationPermissionsGranted = false;

    EditText editText;

    // All code for save marker in Shared preferences: https://stackoverflow.com/questions/25438043/store-google-maps-markers-in-sharedpreferences?rq=1
    private List<Marker> markerList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        editText = findViewById(R.id.editText);

        markerList = new ArrayList<Marker>();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        initMap();
        getLocationPermission();
        loadPreferences();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            super.onSaveInstanceState(outState);
            getDeviceLocation();
            loadPreferences();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMap.clear();
        markerList.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPreferences();

    }

    @Override
    protected void onPause() {
        super.onPause();
        savePreferences();
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        savePreferences();
    }

    @Override
    public void onStop(){
        super.onStop();
        savePreferences();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                drawDefaultLocation();
            }
        }
        loadPreferences();
        // source: https://www.programcreek.com/java-api-examples/?code=alaskalinuxuser/apps_small/apps_small-master/MymemoriablePlacesApp/app/src/main/java/com/alaskalinuxuser/mymemoriableplacesapp/MapsActivity.java
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                String descr = editText.getText().toString();
                markerList.add(mMap.addMarker(new MarkerOptions().title(descr).position(latLng)));
                Toast.makeText(MapsActivity.this, "new location marker saved", Toast.LENGTH_LONG).show();
                editText.setText("");
            }
        });
    }

    private void getDeviceLocation(){

        FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            if(mLocationPermissionsGranted){

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Location currentLocation = (Location) task.getResult();
                            LatLng currentGps = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                            markerList.add(mMap.addMarker(new MarkerOptions().title("current location").position(currentGps)));
                            drawMarker(currentGps, "current location");
                        }else{
                            makeErrorLog("Location is null");
                        }
                    }
                });
            }
        }catch (SecurityException e){
            makeErrorLog(e.getMessage());
        }
    }

    private void getLocationPermission(){
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                initMap();
            }else{
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }


    private void initMap(){
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionsGranted = false;

        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionsGranted = false;
                            // Set map to default position
                            drawDefaultLocation();
                            makeErrorLog("Location permission denied");
                            return;
                        }
                    }
                    mLocationPermissionsGranted = true;
                    initMap();
                }
            }
        }
    }

    private void savePreferences(){
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt("listSize", markerList.size());
        for(int i = 0; i <markerList.size(); i++){
            editor.putFloat("lat"+i, (float) markerList.get(i).getPosition().latitude);
            editor.putFloat("long"+i, (float) markerList.get(i).getPosition().longitude);
            editor.putString("title"+i, markerList.get(i).getTitle());
        }

        editor.commit();
    }

    private void loadPreferences(){
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_FILE, MODE_PRIVATE);

        int size = sharedPreferences.getInt("listSize", 0);
        if(size != 0 && mMap != null) {
            for (int i = 0; i < size; i++) {
                double lat = (double) sharedPreferences.getFloat("lat" + i, 0);
                double longi = (double) sharedPreferences.getFloat("long" + i, 0);
                String descr = sharedPreferences.getString("title" + i, "NULL");
                markerList.add(mMap.addMarker(new MarkerOptions().title(descr).position(new LatLng(lat, longi))));
                drawSavedMarker();
            }
        }
    }

    private void drawMarker(LatLng gpsCoord, String description) {
        if (mMap != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(gpsCoord)
                    .title(description));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(gpsCoord, DEFAULT_ZOOM));
        }
    }

    // Simple function to display and log errors/exceptions
    public void makeErrorLog(String error){
        Log.e("MapsActivity", error);
        Toast.makeText(MapsActivity.this, error, Toast.LENGTH_LONG).show();
    }

    public void drawDefaultLocation(){
        // Set map to default position
        Location defaultLocation = null;
        drawMarker(mDefaultLocation, "Default location: Sydney");
    }

    public void drawSavedMarker(){
        for(Marker currentMarker : markerList){
            drawMarker(currentMarker.getPosition(), currentMarker.getTitle());
        }
    }
}
