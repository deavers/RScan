# RScan

An Android application for real-time object recognition using the device camera.

A college project built to study computer vision technologies and create a working prototype of a mobile app with object detection.

---

## About

The app uses the phone camera to identify objects in real time. Detected objects are highlighted with a bounding box, label, and confidence score.

For each detected object the app shows:
- object class name
- confidence score (%)
- bounding box coordinates
- total number of detections

---

## Tech Stack

| Library | Purpose |
|---|---|
| **TensorFlow Lite** | Running the trained model on-device |
| **CameraX** | Camera preview and frame capture |
| **OpenCV** | Auxiliary image processing |
| **Teachable Machine** (Google) | Training a custom model |

- **Language:** Kotlin + Java
- **Min SDK:** 28 (Android 9.0)
- **Target SDK:** 31 (Android 12)
- **IDE:** Android Studio

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/deavers/RScan.git
cd RScan
```

### 2. Open in Android Studio

Open the project folder via **File → Open**.

### 3. Add the model

Place your `.tflite` model file inside the `models/` folder at the root of the project.  
You can train your own model at [Teachable Machine](https://teachablemachine.withgoogle.com/) → export as **TensorFlow Lite**.

### 4. Run

Connect an Android device (Android 9+) or start an emulator, then press **Run → Run 'app'**.

---

## Project Structure

```
RScan/
├── app/
│   └── src/main/
│       ├── java/       — main source code (Activity, Fragments)
│       ├── cpp/        — native code (CMakeLists.txt)
│       └── res/        — resources (layouts, drawables)
├── models/             — .tflite models (not included in the repo)
├── RScan.png           — app screenshot
└── build.gradle
```

---

## Note

This is a college learning project / prototype. The `.tflite` model file is not included in the repository — add it manually to the `models/` folder before running.
