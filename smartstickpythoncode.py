import cv2
import torch
import json
import asyncio
import websockets
from ultralytics import YOLO
from paddleocr import PaddleOCR
import numpy as np
from picamera2 import Picamera2  # Raspberry Pi Camera support
import RPi.GPIO as GPIO
import time

# Run file in RPi

# GPIO Setup
BUZZER_PIN = 18
RED_LED_PIN = 23
GREEN_LED_PIN = 24
TRIG_PIN = 17  # Ultrasonic sensor trigger pin
ECHO_PIN = 27  # Ultrasonic sensor echo pin

GPIO.setmode(GPIO.BCM)
GPIO.setup(BUZZER_PIN, GPIO.OUT)
GPIO.setup(RED_LED_PIN, GPIO.OUT)
GPIO.setup(GREEN_LED_PIN, GPIO.OUT)
GPIO.setup(TRIG_PIN, GPIO.OUT)
GPIO.setup(ECHO_PIN, GPIO.IN)

# PWM for buzzer intensity control
buzzer_pwm = GPIO.PWM(BUZZER_PIN, 1000)  # 1000 Hz frequency
buzzer_pwm.start(0)  # Start with 0% duty cycle (off)

def set_leds(red=False, green=False):
    GPIO.output(RED_LED_PIN, GPIO.HIGH if red else GPIO.LOW)
    GPIO.output(GREEN_LED_PIN, GPIO.HIGH if green else GPIO.LOW)

def get_distance():
    """Measure distance using HC-SR04 ultrasonic sensor."""
    GPIO.output(TRIG_PIN, False)
    time.sleep(0.00001)  # Small delay to settle

    GPIO.output(TRIG_PIN, True)
    time.sleep(0.00001)  # 10us pulse
    GPIO.output(TRIG_PIN, False)

    pulse_start = time.time()
    pulse_end = time.time()

    while GPIO.input(ECHO_PIN) == 0:
        pulse_start = time.time()
    while GPIO.input(ECHO_PIN) == 1:
        pulse_end = time.time()

    pulse_duration = pulse_end - pulse_start
    distance = pulse_duration * 17150  # Speed of sound = 343 m/s, halved for round trip
    return round(distance, 2)

def set_buzzer_intensity(distance):
    """Set buzzer intensity based on distance (closer = higher intensity)."""
    if distance < 50:  # Only activate if obstacle is within 50 cm
        intensity = max(0, min(100, (50 - distance) * 2))  # Scale 0-100% duty cycle
        buzzer_pwm.ChangeDutyCycle(intensity)
    else:
        buzzer_pwm.ChangeDutyCycle(0)  # Turn off if too far

# WebSocket event to track connection status
websocket_event = asyncio.Event()

async def blink_leds():
    """Blink LEDs when WebSocket is disconnected."""
    while not websocket_event.is_set():
        set_leds(red=True, green=True)
        await asyncio.sleep(0.5)
        set_leds(red=False, green=False)
        await asyncio.sleep(0.5)

# Load YOLO models
traffic_model = YOLO("vandan_traffic.pt")  # Existing traffic light model
custom_model = YOLO("yolov8n.pt")  # Custom YOLO model for object detection

# Load OCR
ocr = PaddleOCR(use_angle_cls=True, lang="en", use_gpu=False)

# Initialize Raspberry Pi Camera
picam2 = Picamera2()
picam2.configure(picam2.create_preview_configuration(main={"size": (640, 480)}))
picam2.start()

def detect_text_paddle(frame):
    """Perform OCR using PaddleOCR to detect text in a frame."""
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    results = ocr.ocr(gray, cls=True)
    return [res[1][0] for res in results[0]] if results and results[0] else []

async def send_data(websocket):
    print(f"Client connected: {websocket.remote_address}")
    websocket_event.set()
    set_leds(red=False, green=True)

    prev_state = {
        "traffic_light": None,
        "crosswalk": False,
        "detected_texts": set(),
        "detected_objects": set()
    }

    try:
        while True:
            frame = picam2.capture_array()
            frame = cv2.cvtColor(frame, cv2.COLOR_RGB2BGR)

            # OCR
            detected_texts = set(detect_text_paddle(frame))

            # Traffic light & crosswalk detection
            traffic_light_state = None
            crosswalk_detected = False

            traffic_results = traffic_model(frame)
            for result in traffic_results:
                for box in result.boxes:
                    label = traffic_model.names[int(box.cls[0])].lower()
                    if label == "redsignal":
                        traffic_light_state = "Red"
                    elif label == "greensignal":
                        traffic_light_state = "Green"
                    elif label == "crosswalk":
                        crosswalk_detected = True

            # Object detection: capture all objects, sort by size, pick top 3 unique
            custom_results = custom_model(frame)
            filtered_boxes = []

            for result in custom_results:
                for box in result.boxes:
                    confidence = box.conf[0]
                    if confidence < 0.5:
                        continue
                    label = custom_model.names[int(box.cls[0])]
                    x1, y1, x2, y2 = map(int, box.xyxy[0])
                    area = (x2 - x1) * (y2 - y1)
                    filtered_boxes.append((label, area))

            filtered_boxes.sort(key=lambda x: x[1], reverse=True)

            seen_labels = set()
            detected_objects = []
            for label, _ in filtered_boxes:
                if label not in seen_labels:
                    detected_objects.append(label)
                    seen_labels.add(label)
                if len(detected_objects) >= 3:
                    break

            # Ultrasonic sensor check when no objects detected
            distance = get_distance() if not detected_objects else None

            # Check if any state changed
            state_changed = (
                traffic_light_state != prev_state["traffic_light"] or
                crosswalk_detected != prev_state["crosswalk"] or
                detected_texts != prev_state["detected_texts"] or
                set(detected_objects) != prev_state["detected_objects"]
            )

            if state_changed:
                prev_state["traffic_light"] = traffic_light_state
                prev_state["crosswalk"] = crosswalk_detected
                prev_state["detected_texts"] = detected_texts
                prev_state["detected_objects"] = set(detected_objects)

                data = {
                    "traffic_light": traffic_light_state,
                    "crosswalk": crosswalk_detected,
                    "detected_texts": list(detected_texts),
                    "detected_objects": detected_objects,
                    "distance": distance if distance is not None else None
                }

                # Sensory feedback
                if detected_texts or traffic_light_state or detected_objects or crosswalk_detected:
                    set_buzzer_intensity(0)  # Turn off buzzer if YOLO detects something
                elif distance is not None:
                    set_buzzer_intensity(distance)  # Activate buzzer based on distance
                else:
                    set_buzzer_intensity(0)  # No detection, no obstacle nearby

                # LED Logic
                if crosswalk_detected:
                    set_leds(red=True, green=True)
                elif traffic_light_state == "Red":
                    set_leds(red=True, green=False)
                elif traffic_light_state == "Green":
                    set_leds(red=False, green=True)
                else:
                    set_leds(red=False, green=False)

                await websocket.send(json.dumps(data))

            # Show camera frame
            cv2.imshow("Smart Stick AI Vision", frame)
            if cv2.waitKey(1) & 0xFF == ord("q"):
                break

            await asyncio.sleep(1)

    except websockets.exceptions.ConnectionClosed:
        print(f"Client {websocket.remote_address} disconnected!")
    except asyncio.CancelledError:
        print("WebSocket Task Cancelled")
    finally:
        websocket_event.clear()
        set_leds(red=False, green=False)
        buzzer_pwm.ChangeDutyCycle(0)
        asyncio.create_task(blink_leds())

async def websocket_server():
    """Starts the WebSocket server and keeps it running."""
    asyncio.create_task(blink_leds())  # Start blinking before connection
    async with websockets.serve(send_data, "0.0.0.0", 8765, ping_interval=None):  
        print("WebSocket server started on ws://0.0.0.0:8765")
        await asyncio.Future()  # Keep server running

try:
    loop = asyncio.get_event_loop()
    loop.run_until_complete(websocket_server())
    loop.run_forever()
except KeyboardInterrupt:
    print("Exiting...")
finally:
    buzzer_pwm.stop()
    set_leds(red=False, green=False)
    GPIO.cleanup()
    cv2.destroyAllWindows()
