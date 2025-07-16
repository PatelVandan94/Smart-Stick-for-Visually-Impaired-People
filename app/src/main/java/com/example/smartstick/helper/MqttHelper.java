//package com.example.smartstick.helper;
//
//import android.content.Context;
//import android.util.Log;
//import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
//import org.eclipse.paho.client.mqttv3.IMqttActionListener;
//import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
//import org.eclipse.paho.client.mqttv3.IMqttToken;
//import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
//import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
//import org.eclipse.paho.client.mqttv3.MqttException;
//import org.eclipse.paho.client.mqttv3.MqttMessage;
//
//import info.mqtt.android.service.MqttAndroidClient;
//
//public class MqttHelper {
//    public MqttAndroidClient mqttAndroidClient;
//    private final String serverUri = "tcp://broker.hivemq.com:1883";
//
//    private final String clientId = "ExampleAndroidClient_" + System.currentTimeMillis(); // Unique ID
//    private final String subscriptionTopic = "sensor/+";
//
//    public MqttHelper(Context context) {
//        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
//        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
//            @Override
//            public void connectComplete(boolean reconnect, String serverURI) {
//                Log.w("MQTT", "Connected to: " + serverURI);
//                subscribeToTopic();
//            }
//
//            @Override
//            public void connectionLost(Throwable cause) {
//                Log.w("MQTT", "Connection lost! Reconnecting...");
//                reconnect();
//            }
//
//            @Override
//            public void messageArrived(String topic, MqttMessage message) {
//                Log.w("MQTT", "Message received: " + message.toString());
//            }
//
//            @Override
//            public void deliveryComplete(IMqttDeliveryToken token) {
//                Log.w("MQTT", "Delivery complete");
//            }
//        });
//    }
//
//    public void connect() {
//        MqttConnectOptions options = new MqttConnectOptions();
//        options.setAutomaticReconnect(true);
//        options.setCleanSession(true); // Change to false if persistent session is needed
//
//        mqttAndroidClient.connect(options, null, new IMqttActionListener() {
//            @Override
//            public void onSuccess(IMqttToken asyncActionToken) {
//                Log.w("MQTT", "Successfully connected!");
//                DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
//                disconnectedBufferOptions.setBufferEnabled(true);
//                disconnectedBufferOptions.setBufferSize(100);
//                disconnectedBufferOptions.setPersistBuffer(false);
//                disconnectedBufferOptions.setDeleteOldestMessages(false);
//                mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
//                subscribeToTopic();
//            }
//
//            @Override
//            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                Log.e("MQTT", "Connection failed: " + exception.getMessage());
//            }
//        });
//    }
//
//    private void reconnect() {
//        Log.w("MQTT", "Attempting to reconnect...");
//        mqttAndroidClient.connect();
//    }
//
//    private void subscribeToTopic() {
//        mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
//            @Override
//            public void onSuccess(IMqttToken asyncActionToken) {
//                Log.w("MQTT", "Subscribed to topic: " + subscriptionTopic);
//            }
//
//            @Override
//            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                Log.e("MQTT", "Subscription failed: " + exception.getMessage());
//            }
//        });
//    }
//}
//
