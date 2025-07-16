//package com.example.smartstick.UI;
//
//import androidx.appcompat.app.AppCompatActivity;
//import android.os.Bundle;
//import android.speech.tts.TextToSpeech;
//import android.widget.Spinner;
//import android.widget.ArrayAdapter;
//import android.widget.TextView;
//import android.view.View;
//import android.graphics.Color;
//
//import com.example.smartstick.R;
//
////import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
////import org.eclipse.paho.client.mqttv3.MqttClient;
////import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
////import org.eclipse.paho.client.mqttv3.MqttException;
////import org.eclipse.paho.client.mqttv3.MqttMessage;
////import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
//
//import java.util.HashMap;
//import java.util.Locale;
//
//public class ObjectDetection extends AppCompatActivity {
//    private static final String MQTT_BROKER = "tcp://test.mosquitto.org:1883";
//    private static final String MQTT_TOPIC = "yolo/detection";
//
//    private TextView detectedObjectsTextView;
//    private TextView connectionStatusTextView;
//    private Spinner languageSpinner;
//    private MqttClient client;
//    private TextToSpeech textToSpeech;
//    private HashMap<String, Locale> languageMap;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_object_detection);
//
//        detectedObjectsTextView = findViewById(R.id.detectedObjectsTextView);
//        connectionStatusTextView = findViewById(R.id.connectionStatus);
//        languageSpinner = findViewById(R.id.languageSpinner);
//
//        // Initialize language map
//        languageMap = new HashMap<>();
//        languageMap.put("English", Locale.US);
//        languageMap.put("French", Locale.FRENCH);
//        languageMap.put("German", Locale.GERMAN);
//        languageMap.put("Spanish", new Locale("es", "ES"));
//        languageMap.put("Gujarati", new Locale("gu", "IN"));
//
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languageMap.keySet().toArray(new String[0]));
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        languageSpinner.setAdapter(adapter);
//
//        textToSpeech = new TextToSpeech(this, status -> {
//            if (status == TextToSpeech.SUCCESS) {
//                textToSpeech.setLanguage(Locale.US);
//            }
//        });
//
//        // Connect to MQTT in a background thread
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                connectToMqtt();
//            }
//        }).start();
//    }
//
//    private void connectToMqtt() {
//        try {
//            client = new MqttClient(MQTT_BROKER, MqttClient.generateClientId(), new MemoryPersistence());
//            MqttConnectOptions options = new MqttConnectOptions();
//            options.setAutomaticReconnect(true);
//            options.setCleanSession(true);
//
//            runOnUiThread(() -> {
//                connectionStatusTextView.setText("Connecting...");
//                connectionStatusTextView.setTextColor(Color.BLACK);
//            });
//
//            client.connect(options);
//
//            runOnUiThread(() -> {
//                connectionStatusTextView.setText("Connected");
//                connectionStatusTextView.setTextColor(Color.GREEN);
//            });
//
//            client.subscribe(MQTT_TOPIC, (topic, message) -> {
//                final String detectedText = new String(message.getPayload());
//
//                runOnUiThread(() -> {
//                    detectedObjectsTextView.setText(detectedText);
//                    speakText(detectedText);
//                });
//            });
//        } catch (MqttException e) {
//            e.printStackTrace();
//            runOnUiThread(() -> {
//                connectionStatusTextView.setText("Disconnected (Retrying...)");
//                connectionStatusTextView.setTextColor(Color.RED);
//            });
//            connectionStatusTextView.postDelayed(this::connectToMqtt, 3000);
//        }
//    }
//
//    private void speakText(String text) {
//        if (textToSpeech != null && !text.isEmpty()) {
//            String selectedLanguage = languageSpinner.getSelectedItem().toString();
//            Locale selectedLocale = languageMap.get(selectedLanguage);
//            if (selectedLocale != null) {
//                textToSpeech.setLanguage(selectedLocale);
//            }
//            String speechText = "New object detected: " + text;
//            textToSpeech.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, null);
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        try {
//            if (client != null && client.isConnected()) {
//                client.disconnect();
//                runOnUiThread(() -> {
//                    connectionStatusTextView.setText("Disconnected");
//                    connectionStatusTextView.setTextColor(Color.RED);
//                });
//            }
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }
//        if (textToSpeech != null) {
//            textToSpeech.stop();
//            textToSpeech.shutdown();
//        }
//    }
//}
