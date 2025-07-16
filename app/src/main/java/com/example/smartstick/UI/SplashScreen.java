package com.example.smartstick.UI;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;

import com.blogspot.atifsoftwares.animatoolib.Animatoo;
import com.example.smartstick.R;

import java.util.HashMap;
import java.util.Locale;

public class SplashScreen extends AppCompatActivity {

    private TextToSpeech textToSpeech;
    private static final String UTTERANCE_ID = "SmartStickWelcome";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        getWindow().setNavigationBarColor(getResources().getColor(R.color.maincolor));

        View view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    String welcomeMessage = "Welcome to the Smart Stick App. "
                            + "This app is designed to assist visually impaired individuals by identifying objects, traffic signals, and reading text from surroundings. "
                            + "Please make sure your smart stick is connected. "
                            + "Point the camera ahead to start detecting your environment. "
                            + "Launching the app now.";

                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            // Nothing needed here
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            if (utteranceId.equals(UTTERANCE_ID)) {
                                runOnUiThread(() -> {
                                    Intent intent = new Intent(SplashScreen.this, MainActivity.class);
                                    startActivity(intent);
                                    Animatoo.INSTANCE.animateFade(SplashScreen.this);
                                    finish();
                                });
                            }
                        }

                        @Override
                        public void onError(String utteranceId) {
                            // Optionally handle error
                        }
                    });

                    // Speak with Utterance ID
                    HashMap<String, String> params = new HashMap<>();
                    params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID);
                    textToSpeech.speak(welcomeMessage, TextToSpeech.QUEUE_FLUSH, params);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
