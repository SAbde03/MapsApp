package com.example.maps;

import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; 

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.maps.databinding.ActivityGoogleMapsBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GoogleMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityGoogleMapsBinding binding;
    String showUrl = "https://192.168.0.172/Maps/Source Files/createPosition.php";
    RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_maps);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (id == R.id.nav_map) {
                return true;
            } else if (id == R.id.nav_settings) {
                return true;
            }

            return false;
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (getIntent().hasExtra("latitude") && getIntent().hasExtra("longitude")) {
            double lat = getIntent().getDoubleExtra("latitude", 0);
            double lng = getIntent().getDoubleExtra("longitude", 0);

            LatLng location = new LatLng(lat, lng);
            mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Current Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
        }
    }

    private void setUpMap() {
        Log.d("GoogleMapsActivity", "Sending POST request to: " + showUrl);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                showUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("GoogleMapsActivity", "Response received: " + response.toString());

                        try {
                            JSONArray positions = response.getJSONArray("positions");
                            Log.d("GoogleMapsActivity", "Found " + positions.length() + " positions in the response");

                            for (int i = 0; i < positions.length(); i++) {
                                JSONObject position = positions.getJSONObject(i);
                                double latitude = position.getDouble("latitude");
                                double longitude = position.getDouble("longitude");
                                Log.d("GoogleMapsActivity", "Adding marker at: Latitude = " + latitude + ", Longitude = " + longitude);

                                mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("Marker"));
                            }
                        } catch (JSONException e) {
                            Log.e("GoogleMapsActivity", "Error parsing response JSON", e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("GoogleMapsActivity", "Error in response: " + error.toString());
            }
        });

        requestQueue.add(jsonObjectRequest);
    }
}