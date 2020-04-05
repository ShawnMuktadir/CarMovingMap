package com.example.carmovingmap;

import androidx.fragment.app.FragmentActivity;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.carmovingmap.retrofit.IGoogleApi;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private List<LatLng> polyLineList;
    private Marker marker;
    private float v;
    private double lat, lng;
    private Handler handler;
    private LatLng startPosition, endPosition;
    private int index, next;
    private Button btnGo;
    private EditText editTextPlace;
    private String destination;
    private PolylineOptions polylineOptions, blackPolylineOptions;
    private Polyline blackPolyline, grayPolyline;

    private LatLng myLocation;

    private IGoogleApi mService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
//        mapFragment.getMapAsync(this);
        polyLineList = new ArrayList<>();
        btnGo = findViewById(R.id.btnSearch);
        editTextPlace = findViewById(R.id.editTextPlace);
        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                destination = editTextPlace.getText().toString();
                destination = destination.replace(" ", "+");
                //replace pace to +,to make url
                mapFragment.getMapAsync(MapsActivity.this);
            }
        });

        mService = Common.getGoogleApi();

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);

//        mMap.setMyLocationEnabled(true);
//        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Add a marker in My Location and move the camera
        final LatLng myLocationLatLng = new LatLng(23.7822, 90.3595);
        mMap.addMarker(new MarkerOptions().position(myLocationLatLng).title("My Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocationLatLng));
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .target(googleMap.getCameraPosition().target)
                .zoom(17)
                .bearing(30)
                .tilt(45)
                .build()));

        String requestUrl = null;
        try {
            requestUrl = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin=" + myLocationLatLng.latitude + "," + myLocationLatLng.longitude + "&" +
                    "destination=" + destination + "&" +
                    "key=" + getResources().getString(R.string.google_direction_key);
            Log.d("URL", requestUrl);
            mService.getDataFromGoogleApi(requestUrl)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {
                                JSONObject jsonObject =
                                        new JSONObject(response.body().toString());
                                JSONArray jsonArray = jsonObject.getJSONArray("routes");
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject route = jsonArray.getJSONObject(i);
                                    JSONObject poly = route.getJSONObject("overview_polyline");
                                    String polyLine = poly.getString("points");
                                    polyLineList = decodePoly(polyLine);

                                }
                                //Adjusting Bounds
                                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                for (LatLng latLng : polyLineList)
                                    builder.include(latLng);
                                LatLngBounds bounds = builder.build();
                                CameraUpdate mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2);
                                mMap.animateCamera(mCameraUpdate);

                                polylineOptions = new PolylineOptions();
                                polylineOptions.color(Color.GRAY);
                                polylineOptions.width(5);
                                polylineOptions.startCap(new SquareCap());
                                polylineOptions.endCap(new SquareCap());
                                polylineOptions.jointType(JointType.ROUND);
                                polylineOptions.addAll(polyLineList);
                                grayPolyline = mMap.addPolyline(polylineOptions);

                                blackPolylineOptions = new PolylineOptions();
                                blackPolylineOptions.color(Color.BLACK);
                                blackPolylineOptions.width(5);
                                blackPolylineOptions.startCap(new SquareCap());
                                blackPolylineOptions.endCap(new SquareCap());
                                blackPolylineOptions.jointType(JointType.ROUND);
                                blackPolylineOptions.addAll(polyLineList);
                                blackPolyline = mMap.addPolyline(blackPolylineOptions);

                                mMap.addMarker(new MarkerOptions().
                                        position(polyLineList.get(polyLineList.size() - 1)));

                                //Animator
                                final ValueAnimator polyLineAnimator = ValueAnimator.ofInt(0, 100);
                                polyLineAnimator.setDuration(2000);
                                polyLineAnimator.setInterpolator(new LinearInterpolator());
                                polyLineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                        List<LatLng> points = grayPolyline.getPoints();
                                        int percentValue = (int) valueAnimator.getAnimatedValue();
                                        int size = points.size();
                                        int newPoints = (int) (size * (percentValue / 100.0f));
                                        List<LatLng> p = points.subList(0, newPoints);
                                        blackPolyline.setPoints(p);
                                    }
                                });
                                polyLineAnimator.start();
                                //Add car markar
                                marker = mMap.addMarker(new MarkerOptions().position(myLocationLatLng)
                                        .flat(true)
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car)));

                                //car moving
                                handler = new Handler();
                                index = -1;
                                next = 1;
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (index < polyLineList.size() - 1) {
                                            index++;
                                            next = index + 1;
                                        }
                                        if (index < polyLineList.size() - 1) {
                                            startPosition = polyLineList.get(index);
                                            endPosition = polyLineList.get(next);
                                        }

                                        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
                                        valueAnimator.setDuration(3000);
                                        valueAnimator.setInterpolator(new LinearInterpolator());
                                        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                            @Override
                                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                                v = valueAnimator.getAnimatedFraction();
                                                lng = v * endPosition.longitude + (1 - v)
                                                        * startPosition.longitude;
                                                lat = v * endPosition.latitude + (1 - v)
                                                        * startPosition.latitude;
                                                LatLng newPos = new LatLng(lat, lng);
                                                marker.setPosition(newPos);
                                                marker.setAnchor(0.5f, 0.5f);
                                                marker.setRotation(getBearing(startPosition, newPos));
                                                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                                                        new CameraPosition.Builder()
                                                                .target(newPos)
                                                                .zoom(15.5f)
                                                                .build()));
                                            }
                                        });
                                        valueAnimator.start();
                                        handler.postDelayed(this, 3000);
                                    }
                                }, 3000);


                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(MapsActivity.this, "" + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private float getBearing(LatLng startPosition, LatLng newPos) {
//        double lat = Math.abs(startPosition.latitude - newPos.latitude);
//        double lng = Math.abs(startPosition.longitude - newPos.longitude);
//
//        if (startPosition.latitude < newPos.latitude && startPosition.longitude < newPos.longitude)
//            return (float) (Math.toDegrees(Math.atan(lng / lat)));
//        else if ((startPosition.latitude >= newPos.latitude && startPosition.longitude < newPos.longitude))
//            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
//        else if ((startPosition.latitude >= newPos.latitude && startPosition.longitude >= newPos.longitude))
//            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
//        else if ((startPosition.latitude < newPos.latitude && startPosition.longitude >= newPos.longitude))
//            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat)) + 270));
//        return -1;
//    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    private static double degreeToRadians(double latLong) {
        return (Math.PI * latLong / 180.0);
    }

    private static double radiansToDegree(double latLong) {
        return (latLong * 180.0 / Math.PI);
    }

    public static float getBearing(LatLng startPosition, LatLng newPos) {

        //Source
        double lat1 = startPosition.latitude;
        double lng1 = startPosition.longitude;

        // destination
        double lat2 = newPos.latitude;
        double lng2 = newPos.longitude;

        double fLat = degreeToRadians(lat1);
        double fLong = degreeToRadians(lng1);
        double tLat = degreeToRadians(lat2);
        double tLong = degreeToRadians(lng2);

        double dLon = (tLong - fLong);

        float degree = (float)(radiansToDegree(Math.atan2(Math.sin(dLon) * Math.cos(tLat),
                Math.cos(fLat) * Math.sin(tLat) - Math.sin(fLat) * Math.cos(tLat) * Math.cos(dLon))));

        if (degree >= 0) {
            return degree;
        } else {
            return 360 + degree;
        }
    }


}










































