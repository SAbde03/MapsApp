package com.example.maps;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private double latitude;
    private double longitude;
    private double altitude;
    private Button btn;
    private float accuracy;
    private RequestQueue requestQueue;
    private static final int PERMISSION_REQUEST_CODE = 1000;
    private static final String[] REQUIRED_PERMISSIONS = {
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private String insertUrl = "http://192.168.0.172/Maps/Source%20Files/createPosition.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn = findViewById(R.id.button);
        requestQueue = Volley.newRequestQueue(this);

        if (checkPermissions()) {
            startLocationUpdates();
        } else {
            requestPermissions();
        }

        btn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GoogleMapsActivity.class);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            startActivity(intent);
        });
    }

    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startLocationUpdates() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showEnableGpsDialog();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000,
                    10,
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            altitude = location.getAltitude();
                            accuracy = location.getAccuracy();

                            String msg = String.format("Lat: %.6f, Long: %.6f, Acc: %.1fm",
                                    latitude, longitude, accuracy);
                            Log.d("LOCATION", msg);
                            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();

                            addPosition(latitude, longitude);
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {}

                        @Override
                        public void onProviderEnabled(String provider) {}

                        @Override
                        public void onProviderDisabled(String provider) {}
                    });

            Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLocation != null) {
                onLocationChanged(lastLocation);
            }
        }
    }

    public void onLocationChanged(Location location) {
        if (location == null) {
            Log.e("Location", "Received null location");
            return;
        }

        latitude = location.getLatitude();
        longitude = location.getLongitude();
        altitude = location.hasAltitude() ? location.getAltitude() : 0.0;
        accuracy = location.hasAccuracy() ? location.getAccuracy() : 0.0f;

        String msg = String.format(Locale.US,
                "New Location:\nLat: %.6f\nLon: %.6f\nAlt: %.1f m\nAcc: %.1f m",
                latitude, longitude, altitude, accuracy);

        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
        Log.d("LocationUpdate", msg);

        addPosition(latitude, longitude);
    }

    private void showEnableGpsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("GPS Disabled")
                .setMessage("Please enable GPS for accurate location tracking")
                .setPositiveButton("Settings", (dialog, which) -> {
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addPosition(double lat, double lon) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("latitude", lat);
            jsonBody.put("longitude", lon);
            jsonBody.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            jsonBody.put("imei", Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ANDROID_ID
            ));
        } catch (JSONException e) {
            Log.e("JSON_ERROR", "Failed to create JSON", e);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                insertUrl,
                jsonBody,
                response -> Log.d("SUCCESS", response.toString()),
                error -> {
                    String errorMsg = "Error: ";
                    if (error.networkResponse != null) {
                        errorMsg += error.networkResponse.statusCode + " - ";
                        try {
                            errorMsg += new String(error.networkResponse.data, "UTF-8");
                        } catch (Exception e) {
                            errorMsg += "Couldn't parse error";
                        }
                    } else {
                        errorMsg += error.getMessage();
                    }
                    Log.e("ERROR", error.toString());
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }
}