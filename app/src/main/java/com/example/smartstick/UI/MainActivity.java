package com.example.smartstick.UI;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.smartstick.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.navigation.NavigationApi;
import com.google.android.libraries.navigation.Navigator;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.android.libraries.navigation.Waypoint;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MainActivity extends AppCompatActivity {
    private static final int SPEECH_REQUEST_CODE = 100;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    private TextView tvResult;
    private TextView tvtraffic_light, tvdetected_text,tvdetect_object,tvcross_walk;
    private TextToSpeech textToSpeech;
    private String destination = "";
    private Navigator navigator;
    private LatLng currentLocation;
    private boolean isNavigating = false;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private WebSocket webSocket; // WebSocket instance

    private static final String API_KEY = Bundle.getString("com.google.android.geo.API_KEY"); // Replace with your actual API key
    private static final String WEBSOCKET_URL = "ws://10.21.26.105:8765"; // Emulator to MacBook
 // Replace with your Pi's IP

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResult = findViewById(R.id.tvResult);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        tvtraffic_light = findViewById(R.id.tvtraffic_light);
        tvdetected_text = findViewById(R.id.tvdetected_text);
        tvdetect_object = findViewById(R.id.tvdetected_objects);
        tvcross_walk = findViewById(R.id.tvcrosswalk);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), API_KEY);
        }

        // Initialize Navigation Fragment
        SupportNavigationFragment navFragment = (SupportNavigationFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navigation_fragment);
        if (navFragment != null) {
            NavigationApi.getNavigator(this, new NavigationApi.NavigatorListener() {
                @Override
                public void onNavigatorReady(Navigator nav) {
                    navigator = nav;
                    Log.d("NavigationDebug", "Navigator initialized");
                }

                @Override
                public void onError(int errorCode) {
                    Log.e("NavigationError", "Failed to initialize Navigator: Error code " + errorCode);
                    speak("Navigation initialization failed.");
                }
            });
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLastKnownLocation();
        }

        textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            }
        });


    }

    private void setupWebSocket() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(WEBSOCKET_URL).build();
        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d("WebSocket", "Connected to Raspberry Pi");
                runOnUiThread(() -> speak("Connected to Raspberry Pi"));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d("WebSocket", "Received: " + text);
                processDetectionData(text);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d("WebSocket", "Closed: " + reason);
                runOnUiThread(() -> speak("WebSocket connection closed"));
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e("WebSocket", "Error: " + t.getMessage());
                runOnUiThread(() -> speak("WebSocket connection failed. Please ensure both devices are on the same Wi-Fi network and try again."));
            }
        };
        webSocket = client.newWebSocket(request, listener);
    }


    private void processDetectionData(String jsonData) {
        try {
            JSONObject data = new JSONObject(jsonData);

            // Extract traffic light status
            String trafficLight = data.optString("traffic_light", "No signal");

            // Extract crosswalk status
            boolean crosswalkDetected = data.optBoolean("crosswalk", false);

            // Extract detected texts
            JSONArray detectedTexts = data.optJSONArray("detected_texts");
            StringBuilder detectedTextDisplay = new StringBuilder();
            StringBuilder speakText = new StringBuilder();

            if (detectedTexts != null && detectedTexts.length() > 0) {
                for (int i = 0; i < detectedTexts.length(); i++) {
                    detectedTextDisplay.append(detectedTexts.getString(i));
                    speakText.append(detectedTexts.getString(i)).append(". ");
                }
            } else {
                detectedTextDisplay.append("None");
            }

            // Extract detected objects
            JSONArray detectedObjects = data.optJSONArray("detected_objects");
            StringBuilder detectedObjectsDisplay = new StringBuilder();
            StringBuilder speakObjectsText = new StringBuilder();

            if (detectedObjects != null && detectedObjects.length() > 0) {
                for (int i = 0; i < detectedObjects.length(); i++) {
                    detectedObjectsDisplay.append(detectedObjects.getString(i)).append(", ");
                    speakObjectsText.append(detectedObjects.getString(i)).append(". ");
                }
            } else {
                detectedObjectsDisplay.append("None");
            }

            // Determine speech message
            StringBuilder finalSpeechText = new StringBuilder();

            if ("green".equalsIgnoreCase(trafficLight)) {
                finalSpeechText.append("You can walk now. ");
            } else if ("red".equalsIgnoreCase(trafficLight)) {
                finalSpeechText.append("Please wait, it's red now. ");
            }

            if (crosswalkDetected) {
                finalSpeechText.append("Crosswalk detected. Please cross safely. ");
            }

            finalSpeechText.append(speakText.toString()).append(speakObjectsText.toString());

            // Update UI elements
            runOnUiThread(() -> {
                tvtraffic_light.setText("Traffic Light: " + trafficLight);
                tvdetected_text.setText("Detected Texts: " + detectedTextDisplay.toString());
                tvdetect_object.setText("Detected Objects: " + detectedObjectsDisplay.toString());
                tvcross_walk.setText("Crosswalk: " + (crosswalkDetected ? "Detected" : "Not detected")); // Update UI for crosswalk
                maybeDescribeIfStore(String.valueOf(detectedTexts));
                if (!finalSpeechText.toString().isEmpty()) {
                    speak(finalSpeechText.toString());
                }
            });

        } catch (JSONException e) {
            Log.e("WebSocket", "JSON Parsing Error: " + e.getMessage());
        }
    }










    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your destination");
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = result.get(0);
            Toast.makeText(this, "You said: " + spokenText, Toast.LENGTH_SHORT).show();
            extractSourceAndDestination(spokenText);
        }
    }

    private void extractSourceAndDestination(String spokenText) {
        destination = spokenText;
        tvResult.setText("Destination: " + destination);

        if (textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }

        speak("Your destination is " + destination);

        if (currentLocation != null) {
            new GetCoordinatesTask().execute(destination);
        } else {
            speak("Current location not available. Please try again.");
        }
    }

    private class GetCoordinatesTask extends AsyncTask<String, Void, LatLng> {
        @Override
        protected LatLng doInBackground(String... locations) {
            try {
                return getLatLngFromAddress(locations[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(LatLng destinationLatLng) {
            if (destinationLatLng != null && currentLocation != null) {
                startNavigation(currentLocation, destinationLatLng);
                // Initialize WebSocket connection
                setupWebSocket();
            } else {
                speak("Could not find destination.");
            }
        }
    }

    private LatLng getLatLngFromAddress(String place) {
        try {
            String urlStr = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json?" +
                    "input=" + URLEncoder.encode(place, "UTF-8") +
                    "&inputtype=textquery&fields=geometry&key=" + API_KEY;

            String response = getJsonFromUrl(urlStr);
            JSONObject jsonObject = new JSONObject(response);
            JSONArray candidates = jsonObject.getJSONArray("candidates");

            if (candidates.length() > 0) {
                JSONObject location = candidates.getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONObject("location");

                return new LatLng(location.getDouble("lat"), location.getDouble("lng"));
            }
        } catch (Exception e) {
            Log.e("PlacesAPI", "Error fetching coordinates: " + e.getMessage());
        }
        return null;
    }

    private void startNavigation(LatLng source, LatLng destination) {
        if (navigator == null) {
            speak("Navigation not available.");
            return;
        }

        Waypoint start = Waypoint.builder()
                .setLatLng(source.latitude, source.longitude)
                .build();

        Waypoint end = Waypoint.builder()
                .setLatLng(destination.latitude, destination.longitude)
                .build();

        List<Waypoint> waypoints = new ArrayList<>();
        waypoints.add(end);
        navigator.setDestinations(waypoints);
        navigator.startGuidance();
        isNavigating = true;
        speak("Navigation started. Follow the voice instructions.");
    }

    private String getJsonFromUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            inputStream.close();
            connection.disconnect();
            return sb.toString();
        } catch (Exception e) {
            Log.e("NetworkError", "Error fetching JSON: " + e.getMessage());
            return null;
        }
    }

    private void speak(String text) {
        if (textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SpeechID");
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || locationResult.getLocations().isEmpty()) {
                    Log.e("LocationError", "Could not get current location.");
                    return;
                }

                Location location = locationResult.getLastLocation();
                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                new GetAddressTask().execute(currentLocation);

                fusedLocationProviderClient.removeLocationUpdates(this);
            }
        };

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private class GetAddressTask extends AsyncTask<LatLng, Void, String> {
        @Override
        protected String doInBackground(LatLng... locations) {
            LatLng location = locations[0];
            try {
                String urlStr = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                        + location.latitude + "," + location.longitude
                        + "&key=" + API_KEY;
                String response = getJsonFromUrl(urlStr);
                JSONObject jsonObject = new JSONObject(response);
                JSONArray results = jsonObject.getJSONArray("results");

                if (results.length() > 0) {
                    JSONObject firstResult = results.getJSONObject(0);
                    return firstResult.getString("formatted_address");
                }
            } catch (Exception e) {
                Log.e("GeoError", "Error fetching address: " + e.getMessage());
            }
            return "Unknown location";
        }

        @Override
        protected void onPostExecute(String address) {
            speak("Your current location is " + address);
            textToSpeech.setOnUtteranceCompletedListener(utteranceId -> runOnUiThread(() -> {
                if (!isNavigating && destination.isEmpty()) {
                    startVoiceInput();
                }
            }));
        }
    }
    private void maybeDescribeIfStore(String detectedText) {
        if (detectedText == null || detectedText.length() < 3) return;

        PlacesClient placesClient = Places.createClient(this);

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(detectedText)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    if (!response.getAutocompletePredictions().isEmpty()) {
                        // Detected text is likely a store or business
                        String placeId = response.getAutocompletePredictions().get(0).getPlaceId();
                        fetchPlaceDetails(placeId);
                    } else {
                        // Not a valid business/store
                        // You can log or skip
                    }
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    private void fetchPlaceDetails(String placeId) {
        PlacesClient placesClient = Places.createClient(this);
        List<Place.Field> fields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.TYPES);

        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, fields).build();

        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            Place place = response.getPlace();
            if (place.getTypes() != null && !place.getTypes().isEmpty()) {
                String type = place.getTypes().get(0).toString().toLowerCase().replace("_", " ");
                String description = "This is " + place.getName() + ". It's a " + type + ".";
                speak(description);
            }
        }).addOnFailureListener(Throwable::printStackTrace);
    }



    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (navigator != null) {
            navigator.stopGuidance();
        }
        if (webSocket != null) {
            webSocket.close(1000, "App closed"); // Close WebSocket gracefully
        }
        super.onDestroy();
    }
}

class LatLng {
    double latitude;
    double longitude;

    LatLng(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}