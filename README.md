# Smart Stick for Visually Impaired

An AI- and IoT-powered Smart Stick designed to assist visually impaired individuals with safe and independent navigation. This project integrates Raspberry Pi, YOLO object detection, PaddleOCR, Google Maps API, and a custom Android app for real-time guidance and feedback.

## 🚀 Project Overview

Visually impaired users often face difficulty navigating their environment. This Smart Stick addresses that by combining sensors and real-time AI-based object and text detection to provide auditory and haptic feedback. The system also includes a mobile application for navigation using Google Maps API and voice guidance.

## 🎯 Project Aim

To design and develop a Smart Stick that enhances the mobility and safety of visually impaired individuals by:
- Detecting objects, signals, and text
- Providing real-time voice assistance
- Enabling navigation using Google Maps
- Using haptic and visual feedback through LEDs and buzzer

## 🧠 Key Features

- Real-time object detection using YOLOv8 (e.g. potholes, streetlights, trash bins)
- Pedestrian traffic light and crosswalk recognition
- Text recognition using PaddleOCR and semantic interpretation via Google Places API
- Google Maps navigation with voice instructions
- WebSocket-based communication between Raspberry Pi and Android app
- Feedback via buzzer and LEDs
- Ultrasonic sensor for obstacle detection

## 📱 Mobile Application

**Android App Capabilities:**
- Voice command to set destinations
- Google TTS for real-time announcements
- Google Maps API for route planning
- WebSocket for real-time data exchange with Raspberry Pi

## 🧰 Tools & Technologies

| Component         | Description                                         |
|------------------|-----------------------------------------------------|
| Raspberry Pi 5    | Central processing unit                            |
| Pi Camera Module | Captures images for object and text detection      |
| YOLOv8           | Real-time object detection model                   |
| PaddleOCR        | OCR library for detecting signboard and shop names |
| Google TTS       | Converts text to speech                            |
| Google Maps API  | Provides navigation and location context           |
| Google Places API| Interprets detected text into meaningful places    |
| Ultrasonic Sensor| Detects nearby obstacles                           |
| LEDs + Buzzer    | Provides visual and audio feedback                 |
| WebSocket        | Communication bridge between Pi and Android app   |

## 🔁 Workflow Overview

1. User connects headphones and opens the Android app.
2. Destination is set via voice.
3. Real-time object and text detection begins.
4. Feedback is given through voice (TTS), LED, and buzzer.
5. Google Maps provides audio navigation.


## 🧪 Results

### Model 1
- Objects Detected: Trash bins, potholes, streetlights
- Confidence: 0.87 - 0.88

### Model 2
- Traffic Light: Red (0.92), Green (0.78)
- Crosswalk: Detected at 0.70

### Model 3
- Texts Detected: Signboards like “NIRMA”, “STARBUCKS”
- Contextual output via Google Places API

## 🔮 Future Enhancements

- Distance estimation of obstacles
- Suspicious activity detection
- Real-time face recognition
- Haptic vibration feedback
- Water/staircase detection
- “Find My Stick” feature
- Weather alerts

## 📚 References

Research papers and conferences from IEEE, ICAC3N, ICCUBEA supporting the development.

## 🙌 Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## 📄 License

This project is open-source under the [MIT License](LICENSE).

---

> Developed with ❤️ to improve accessibility and safety for visually impaired users.


